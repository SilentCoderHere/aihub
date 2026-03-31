package com.foss.aihub

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.foss.aihub.ui.components.AiHubTheme
import com.foss.aihub.ui.screens.AiHubApp
import com.foss.aihub.ui.screens.ErrorScreen
import com.foss.aihub.ui.screens.InitialLoadingScreen
import com.foss.aihub.ui.webview.WebViewSecurity
import com.foss.aihub.utils.ConfigUpdater
import com.foss.aihub.utils.SettingsManager
import com.foss.aihub.utils.refreshAiServicesFromSettings
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    var filePathCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    lateinit var settingsManager: SettingsManager
    private var pendingWebViewPermissionRequest: PermissionRequest? = null

    private var isInitialConfigReady by mutableStateOf(false)
    private var initialConfigError by mutableStateOf<String?>(null)

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
        super.onCreate(savedInstanceState)
        WebViewSecurity.init(this)

        try {
            settingsManager = SettingsManager(this)

            if (!needsInitialConfig()) {
                refreshAiServicesFromSettings(this)
                settingsManager.cleanupAndFixServices(this)
            }

            initializeFileChooserLauncher()

            setContent {
                AiHubTheme(context = this, settingsManager = settingsManager) {
                    when {
                        initialConfigError != null -> {
                            ErrorScreen(
                                message = initialConfigError
                                    ?: stringResource(R.string.label_unknown_error),
                                onRetry = {
                                    initialConfigError = null
                                    lifecycleScope.launch { runInitialConfig(this@MainActivity) }
                                },
                            )
                        }

                        isInitialConfigReady -> {
                            settingsManager.saveLastUpdatedDate()
                            AiHubApp(this@MainActivity)
                        }

                        else -> {
                            InitialLoadingScreen()
                        }
                    }
                }
            }

            lifecycleScope.launch {
                if (needsInitialConfig()) {
                    runInitialConfig(this@MainActivity)
                } else {
                    isInitialConfigReady = true
                }
            }

        } catch (e: Exception) {
            setContent {
                AiHubTheme(context = this, settingsManager = settingsManager) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.msg_fail_to_start, e.message.toString()),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    private fun needsInitialConfig(): Boolean {
        val hasDomain = settingsManager.hasDomainConfig()
        val aiServicesList = settingsManager.getAiServices()
        val hasAiServices = aiServicesList.isNotEmpty()
        return !hasDomain || !hasAiServices
    }

    private suspend fun runInitialConfig(context: Context) {
        try {
            val (_, _) = ConfigUpdater.updateBothIfNeeded(this)
            settingsManager.cleanupAndFixServices(this)

            isInitialConfigReady = true
            initialConfigError = null

        } catch (e: Exception) {
            isInitialConfigReady = false
            initialConfigError = e.message ?: context.getString(R.string.msg_fail_to_load_config)
        }
    }

    private fun initializeFileChooserLauncher() {
        fileChooserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleFileChooserResult(result)
        }
    }

    fun launchFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ) {
        this.filePathCallback = filePathCallback

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
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
        } catch (e: ActivityNotFoundException) {
            filePathCallback.onReceiveValue(null)
            this.filePathCallback = null
        } catch (e: Exception) {
            filePathCallback.onReceiveValue(null)
            this.filePathCallback = null
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
                    uris = arrayOf(Uri.parse(dataString))
                }
            }
        }

        callback.onReceiveValue(uris)
        this.filePathCallback = null
    }
}