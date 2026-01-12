package com.foss.aihub

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.foss.aihub.ui.screens.AiHubApp
import com.foss.aihub.ui.theme.AiHubTheme
import com.foss.aihub.utils.SettingsManager

class MainActivity : ComponentActivity() {
    var filePathCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    lateinit var settingsManager: SettingsManager
    private var pendingWebViewPermissionRequest: PermissionRequest? = null
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Microphone enabled", Toast.LENGTH_SHORT).show()
            Log.d("AI_HUB", "User granted microphone permission")

            pendingWebViewPermissionRequest?.let { request ->
                if (request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                    request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                    Log.d("AI_HUB", "Granted microphone access to WebView")
                }
                pendingWebViewPermissionRequest = null
            }
        } else {
            Toast.makeText(
                this, "Microphone permission denied", Toast.LENGTH_LONG
            ).show()
            Log.d("AI_HUB", "User denied microphone permission")

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
                Log.d("AI_HUB", "Microphone permission already granted")
                permissionRequest.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                pendingWebViewPermissionRequest = null
            }

            else -> {
                Log.d("AI_HUB", "Requesting microphone permission for WebView")
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("AI_HUB", "onCreate started")
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        try {
            settingsManager = SettingsManager(this)
            settingsManager.cleanupAndFixServices()

            Log.d("AI_HUB", "SettingsManager initialized")

            initializeFileChooserLauncher()
            Log.d("AI_HUB", "File chooser launcher initialized")

            setContent {
                Log.d("AI_HUB", "setContent started")
                AiHubTheme(
                    darkTheme = isSystemInDarkTheme(), dynamicColor = true
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colorScheme.background)
                    ) {
                        Log.d("AI_HUB", "Rendering AiHubApp")
                        AiHubApp(this@MainActivity)
                    }
                }
                Log.d("AI_HUB", "setContent completed")
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate", e)

            setContent {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.errorContainer)
                ) {}
            }
        }
        Log.d("AI_HUB", "onCreate completed")
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
            Log.e("MainActivity", "Error launching file chooser", e)
        }
    }
}