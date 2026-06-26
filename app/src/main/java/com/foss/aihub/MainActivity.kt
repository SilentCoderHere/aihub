package com.foss.aihub

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.foss.aihub.models.AiService
import com.foss.aihub.models.loadServices
import com.foss.aihub.ui.components.AiHubTheme
import com.foss.aihub.ui.screens.AiHubApp
import com.foss.aihub.ui.screens.OnboardingScreen
import com.foss.aihub.ui.webview.WebViewSecurity
import com.foss.aihub.utils.AI_SERVICES_FILE
import com.foss.aihub.utils.CloudDataHandler
import com.foss.aihub.utils.DOMAINS_FILE
import com.foss.aihub.utils.SettingsManager
import java.io.File

class MainActivity : ComponentActivity() {
    var filePathCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    lateinit var settingsManager: SettingsManager
    private var pendingWebViewPermissionRequest: PermissionRequest? = null

    private var isDataReady by mutableStateOf(false)
    private var onboardingComplete by mutableStateOf(false)
    private var aiServices by mutableStateOf<List<AiService>?>(null)
    private var domains: HashSet<String>? = null

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults: Map<String, Boolean> ->
        pendingWebViewPermissionRequest?.let { request ->
            val toGrant = mutableListOf<String>()
            if (request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                val audioGranted =
                    grantResults[Manifest.permission.RECORD_AUDIO] == true || ContextCompat.checkSelfPermission(
                        this, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                if (audioGranted) toGrant.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
            }
            if (request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                val cameraGranted =
                    grantResults[Manifest.permission.CAMERA] == true || ContextCompat.checkSelfPermission(
                        this, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                if (cameraGranted) toGrant.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
            }
            if (toGrant.isNotEmpty()) {
                request.grant(toGrant.toTypedArray())
                val msg = when {
                    toGrant.size > 1 -> this.getString(R.string.msg_camera_and_microphone_enabled)
                    toGrant.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) -> this.getString(R.string.msg_camera_enabled)
                    else -> this.getString(R.string.msg_microphone_enabled)
                }
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            } else {
                request.deny()
                Toast.makeText(
                    this, this.getString(R.string.msg_permission_denied), Toast.LENGTH_LONG
                ).show()
            }
            pendingWebViewPermissionRequest = null
        }
    }

    fun requestWebViewPermissions(permissionRequest: PermissionRequest) {
        pendingWebViewPermissionRequest = permissionRequest
        val permissionsNeeded = mutableListOf<String>()
        if (permissionRequest.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE) && ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }
        if (permissionRequest.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) && ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }
        if (permissionsNeeded.isEmpty()) {
            permissionRequest.grant(permissionRequest.resources)
            pendingWebViewPermissionRequest = null
            return
        }
        requestPermissionsLauncher.launch(permissionsNeeded.toTypedArray())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        settingsManager = SettingsManager(this)
        onboardingComplete = settingsManager.isOnboardingCompleted()

        splashScreen.setKeepOnScreenCondition {
            onboardingComplete && !isDataReady && aiServices == null
        }

        initializeFileChooserLauncher()

        setContent {
            AiHubTheme(context = this, settingsManager = settingsManager) {
                if (!onboardingComplete) {
                    OnboardingScreen(
                        context = this@MainActivity,
                        settingsManager = settingsManager,
                        onComplete = {
                            settingsManager.setOnboardingCompleted()
                            onboardingComplete = true
                        })
                } else {
                    LaunchedEffect(Unit) {
                        if (aiServices == null || domains == null) {
                            try {
                                val (services, domainSet) = loadAllData()
                                aiServices = services
                                domains = domainSet
                                WebViewSecurity.init(this@MainActivity, domainSet)
                                isDataReady = true
                            } catch (_: Exception) {
                                onboardingComplete = false
                                settingsManager.setOnboardingCompleted(false)
                            }
                        }
                    }

                    if (isDataReady && aiServices != null) {
                        AiHubApp(
                            this@MainActivity,
                            aiServices!!,
                            onServicesUpdated = { newList -> aiServices = newList })
                    } else {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    private suspend fun loadAllData(): Pair<List<AiService>, HashSet<String>> {
        val domainsFile = File(filesDir, DOMAINS_FILE)
        val domainsSet = if (domainsFile.exists()) {
            domainsFile.bufferedReader().useLines { lines ->
                lines.map { it.trim() }.filter { it.isNotBlank() }.toHashSet()
            }
        } else {
            HashSet()
        }

        val services = if (File(filesDir, AI_SERVICES_FILE).exists()) {
            loadServices(this)
        } else {
            emptyList()
        }

        if (domainsSet.isEmpty() || services.isEmpty()) {
            CloudDataHandler.updateAiServices(this)
            CloudDataHandler.updateDomains(this)
            return loadAllData()
        }
        return services to domainsSet
    }

    private fun initializeFileChooserLauncher() {
        fileChooserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleFileChooserResult(result)
        }
    }

    fun launchFileChooser(filePathCallback: ValueCallback<Array<Uri>>) {
        this.filePathCallback = filePathCallback
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
            }
        }
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        try {
            fileChooserLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            filePathCallback.onReceiveValue(null)
            this.filePathCallback = null
            Toast.makeText(
                this, this.getString(R.string.msg_no_suitable_app_found), Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {
            filePathCallback.onReceiveValue(null)
            this.filePathCallback = null
            Toast.makeText(this, this.getString(R.string.error_generic_title), Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun handleFileChooserResult(result: ActivityResult) {
        val callback = this.filePathCallback ?: return
        var uris: Array<Uri>? = null
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data != null) {
                val dataString = data.dataString
                if (dataString != null) {
                    uris = arrayOf(dataString.toUri())
                }
            }
        }
        callback.onReceiveValue(uris)
        this.filePathCallback = null
    }
}