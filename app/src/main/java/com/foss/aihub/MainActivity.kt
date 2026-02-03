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
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.foss.aihub.ui.screens.AiHubApp
import com.foss.aihub.ui.screens.ErrorScreen
import com.foss.aihub.ui.screens.InitialLoadingScreen
import com.foss.aihub.ui.theme.AiHubTheme
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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Microphone enabled", Toast.LENGTH_SHORT).show()

            pendingWebViewPermissionRequest?.let { request ->
                if (request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                    request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                }
                pendingWebViewPermissionRequest = null
            }
        } else {
            Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_LONG).show()

            pendingWebViewPermissionRequest?.deny()
            pendingWebViewPermissionRequest = null
        }
    }

    fun requestMicrophonePermissionForWebView(permissionRequest: PermissionRequest) {
        pendingWebViewPermissionRequest = permissionRequest

        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                permissionRequest.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                pendingWebViewPermissionRequest = null
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebViewSecurity.init(this)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        try {
            settingsManager = SettingsManager(this)

            if (!needsInitialConfig()) {
                refreshAiServicesFromSettings(this)
                settingsManager.cleanupAndFixServices(this)
            }

            initializeFileChooserLauncher()

            setContent {
                AiHubTheme(context = this) {
                    when {
                        initialConfigError != null -> {
                            ErrorScreen(
                                message = initialConfigError ?: "Unknown error", onRetry = {
                                    initialConfigError = null
                                    lifecycleScope.launch { runInitialConfig() }
                                })
                        }

                        isInitialConfigReady -> {
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
                    runInitialConfig()
                } else {
                    isInitialConfigReady = true
                }
            }

        } catch (e: Exception) {
            setContent {
                AiHubTheme(context = this) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "App failed to start\n${e.message}",
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

    private suspend fun runInitialConfig() {
        try {
            val (_, _) = ConfigUpdater.updateBothIfNeeded(this)
            settingsManager.cleanupAndFixServices(this)

            isInitialConfigReady = true
            initialConfigError = null

        } catch (e: Exception) {
            initialConfigError =
                e.message ?: "Failed to load configurations. Please check your internet connection."
        }
    }

    private fun initializeFileChooserLauncher() {
        fileChooserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleFileChooserResult(result)
        }
    }

    private fun handleFileChooserResult(result: ActivityResult) {
        val callback = this.filePathCallback

        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val uris: Array<Uri>? = if (data != null) {
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
            } else {
                null
            }
            callback?.onReceiveValue(uris)
        } else {
            callback?.onReceiveValue(null)
        }
        this.filePathCallback = null
    }

    fun launchFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ) {
        this.filePathCallback = filePathCallback
        val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
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
}