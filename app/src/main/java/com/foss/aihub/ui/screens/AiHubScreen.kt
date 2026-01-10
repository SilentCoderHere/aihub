package com.foss.aihub.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.foss.aihub.MainActivity
import com.foss.aihub.models.LinkData
import com.foss.aihub.models.WebViewState
import com.foss.aihub.ui.components.AiHubAppBar
import com.foss.aihub.ui.components.DrawerContent
import com.foss.aihub.ui.components.ErrorOverlay
import com.foss.aihub.ui.components.ErrorType
import com.foss.aihub.ui.components.LoadingOverlay
import com.foss.aihub.ui.screens.dialogs.MD3LinkOptionsDialog
import com.foss.aihub.ui.webview.WebViewSecurity
import com.foss.aihub.ui.webview.createWebViewForService
import com.foss.aihub.ui.webview.updateWebViewSettings
import com.foss.aihub.utils.aiServices
import com.foss.aihub.utils.copyLinkToClipboard
import com.foss.aihub.utils.openInExternalBrowser
import com.foss.aihub.utils.shareLink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHubApp(activity: MainActivity) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settingsManager = remember { activity.settingsManager }
    val settings by settingsManager.settingsFlow.collectAsState()

    var showLinkDialog by remember { mutableStateOf(false) }
    var selectedLink by remember { mutableStateOf<LinkData?>(null) }
    var previousEnabledServices by remember { mutableStateOf(settings.enabledServices) }

    val loadingProgress = remember { mutableStateMapOf<String, Int>() }
    val webViewStates = remember { mutableStateMapOf<String, WebViewState>() }
    val errorStates = remember { mutableStateMapOf<String, Pair<Int, String>>() }

    val initialId = if (settings.loadLastOpenedAI) {
        settingsManager.getLastOpenedService() ?: settings.defaultServiceId
    } else {
        settings.defaultServiceId
    }

    var selectedService by remember {
        mutableStateOf(aiServices.find { it.id == initialId } ?: aiServices.first())
    }


    val hasCurrentError by derivedStateOf {
        errorStates.containsKey(selectedService.id) && errorStates[selectedService.id]?.let {
            ErrorType.shouldShowOverlay(
                it.first
            )
        } == true
    }
    val currentError by derivedStateOf { errorStates[selectedService.id] }

    var previousConnectionBlocking by remember {
        mutableStateOf(WebViewSecurity.isBlockingEnabled)
    }

    LaunchedEffect(selectedService) {
        if (settings.loadLastOpenedAI) {
            settingsManager.saveLastOpenedService(selectedService.id)
        }
    }

    var showSettingsScreen by remember { mutableStateOf(false) }
    var showManageServices by remember { mutableStateOf(false) }

    val webViews = remember { mutableStateMapOf<String, WebView>() }
    val loadingStates = remember { mutableStateMapOf<String, Boolean>() }
    val loadedServices = remember { mutableStateSetOf<String>() }

    var currentRoot by remember { mutableStateOf<FrameLayout?>(null) }

    LaunchedEffect(selectedService.id) {
        Log.d("AI_HUB", "Service switched to: ${selectedService.name} (id: ${selectedService.id})")
        val wv = webViews[selectedService.id]
        if (wv == null) {
            Log.d("AI_HUB", "No WebView yet for ${selectedService.name} → will create")
            webViewStates[selectedService.id] = WebViewState.LOADING

            errorStates.remove(selectedService.id)
        } else {
            Log.d("AI_HUB", "Reusing existing WebView for ${selectedService.name}")
            if (wv.parent == null && currentRoot != null) {
                currentRoot?.addView(wv)
            }
            wv.visibility = View.VISIBLE

            webViewStates[selectedService.id] = if (loadingStates[selectedService.id] == true) {
                WebViewState.LOADING
            } else {
                val hasError = errorStates.containsKey(selectedService.id)
                if (hasError) WebViewState.ERROR else WebViewState.SUCCESS
            }
        }
        loadedServices.add(selectedService.id)
    }

    ModalNavigationDrawer(
        drawerState = drawerState, gesturesEnabled = drawerState.isOpen, drawerContent = {
            DrawerContent(
                selectedService = selectedService,
                onServiceSelected = { service ->
                    selectedService = service
                    scope.launch { drawerState.close() }
                },
                onServiceReload = { service ->
                    scope.launch { drawerState.close() }
                    webViews[service.id]?.reload()

                    errorStates.remove(service.id)
                    webViewStates[service.id] = WebViewState.LOADING
                },
                webViewStates = webViewStates,
                enabledServices = settings.enabledServices,
                serviceOrder = settings.serviceOrder
            )
        }) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            topBar = {
                AiHubAppBar(selectedService = selectedService, onMenuClick = {
                    scope.launch {
                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                    }
                }, onSettingsClick = { showSettingsScreen = true })
            }) { innerPadding ->
            val isCurrentLoading by derivedStateOf { loadingStates[selectedService.id] ?: false }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") AndroidView(
                        factory = { ctx ->
                        FrameLayout(ctx).apply {
                            currentRoot = this
                            webViews.values.forEach { wv ->
                                if (wv.parent == null) {
                                    addView(wv)
                                    wv.visibility = View.GONE
                                }
                            }
                            val currentWebView = webViews[selectedService.id]
                            if (currentWebView == null) {
                                val newWebView = createWebViewForService(
                                    context = context,
                                    service = selectedService,
                                    activity = activity,
                                    settings = settings,
                                    onProgressUpdate = { progress ->
                                        loadingProgress[selectedService.id] = progress
                                    },
                                    onLoadingStateChange = { isLoading ->
                                        loadingStates[selectedService.id] = isLoading
                                        if (isLoading) {
                                            webViewStates[selectedService.id] = WebViewState.LOADING

                                            errorStates.remove(selectedService.id)
                                        } else {

                                            if (!errorStates.containsKey(selectedService.id)) {
                                                webViewStates[selectedService.id] =
                                                    WebViewState.SUCCESS
                                            }
                                        }
                                        if (!isLoading && loadingProgress.containsKey(
                                                selectedService.id
                                            )
                                        ) {
                                            scope.launch {
                                                delay(500)
                                                loadingProgress.remove(selectedService.id)
                                            }
                                        }
                                    },
                                    onLinkLongPress = { url, title, type ->
                                        selectedLink = LinkData(url, title, type)
                                        showLinkDialog = true
                                    },
                                    onError = { errorCode, description ->
                                        Log.w("AI_HUB", "→ $errorCode | $description | hide=${errorCode !in 500..599 && ErrorType.shouldShowOverlay(errorCode)}")
                                        errorStates[selectedService.id] = errorCode to description

                                        val isCriticalError = when {
                                            errorCode < 0               -> true   // network, ssl, timeout, etc.
                                            errorCode in 400..499       -> true   // client errors (forbidden, not found...)
                                            errorCode in 500..599       -> false  // server errors → show server's page
                                            else                        -> true   // everything else treated as critical
                                        }

                                        if (isCriticalError) {
                                            webViewStates[selectedService.id] = WebViewState.ERROR
                                            webViews[selectedService.id]?.visibility = View.GONE
                                        }
                                    }
                                )
                                webViews[selectedService.id] = newWebView
                                addView(newWebView)

                                newWebView.visibility = View.VISIBLE
                                webViewStates[selectedService.id] = WebViewState.LOADING
                            } else {
                                if (currentWebView.parent == null) addView(currentWebView)

                                currentWebView.visibility =
                                    if (errorStates.containsKey(selectedService.id)) {
                                        View.GONE
                                    } else {
                                        View.VISIBLE
                                    }
                            }
                        }
                    }, update = { root ->
                        currentRoot = root
                        webViews.forEach { (id, wv) ->
                            if (wv.parent == root) {
                                val shouldBeVisible =
                                    id == selectedService.id && !errorStates.containsKey(id)
                                wv.visibility = if (shouldBeVisible) View.VISIBLE else View.GONE
                            } else if (id == selectedService.id && wv.parent == null) {
                                root.addView(wv)
                                wv.visibility = if (errorStates.containsKey(id)) {
                                    View.GONE
                                } else {
                                    View.VISIBLE
                                }
                            }
                        }
                        if (webViews[selectedService.id] == null) {
                            val newWebView = createWebViewForService(
                                context = root.context,
                                service = selectedService,
                                activity = activity,
                                settings = settings,
                                onProgressUpdate = { progress ->
                                    loadingProgress[selectedService.id] = progress
                                },
                                onLoadingStateChange = { isLoading ->
                                    loadingStates[selectedService.id] = isLoading
                                    if (isLoading) {
                                        webViewStates[selectedService.id] = WebViewState.LOADING
                                        errorStates.remove(selectedService.id)
                                    } else {

                                        if (!errorStates.containsKey(selectedService.id)) {
                                            webViewStates[selectedService.id] = WebViewState.SUCCESS
                                        }
                                    }
                                    if (!isLoading && loadingProgress.containsKey(
                                            selectedService.id
                                        )
                                    ) {
                                        scope.launch {
                                            delay(500)
                                            loadingProgress.remove(selectedService.id)
                                        }
                                    }
                                },
                                onLinkLongPress = { url, title, type ->
                                    selectedLink = LinkData(url, title, type)
                                    showLinkDialog = true
                                }, onError = { errorCode, description ->
                                    Log.w("AI_HUB", "→ $errorCode | $description | hide=${errorCode !in 500..599 && ErrorType.shouldShowOverlay(errorCode)}")
                                    errorStates[selectedService.id] = errorCode to description

                                    val isCriticalError = when {
                                        errorCode < 0               -> true   // network, ssl, timeout, etc.
                                        errorCode in 400..499       -> true   // client errors (forbidden, not found...)
                                        errorCode in 500..599       -> false  // server errors → show server's page
                                        else                        -> true   // everything else treated as critical
                                    }

                                    if (isCriticalError) {
                                        webViewStates[selectedService.id] = WebViewState.ERROR
                                        webViews[selectedService.id]?.visibility = View.GONE
                                    }
                                })
                            webViews[selectedService.id] = newWebView
                            root.addView(newWebView)
                            newWebView.visibility = View.VISIBLE
                            webViewStates[selectedService.id] = WebViewState.LOADING
                        }
                    }, modifier = Modifier.fillMaxSize()
                    )


                    if (isCurrentLoading && !hasCurrentError) {
                        LoadingOverlay(
                            isVisible = true,
                            isRefreshing = false,
                            serviceName = selectedService.name,
                            accentColor = selectedService.accentColor,
                            progress = loadingProgress[selectedService.id] ?: 0,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (hasCurrentError && currentError != null) {
                        val (errorCode, errorMessage) = currentError!!
                        ErrorOverlay(
                            errorType = ErrorType.fromErrorCode(errorCode),
                            errorCode = errorCode,
                            errorMessage = errorMessage,
                            serviceName = selectedService.name,
                            accentColor = selectedService.accentColor,
                            onRetry = {
                                errorStates.remove(selectedService.id)
                                webViews[selectedService.id]?.reload()
                                webViewStates[selectedService.id] = WebViewState.LOADING
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }


    BackHandler(enabled = !showSettingsScreen && !showManageServices) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            hasCurrentError -> {
                errorStates.remove(selectedService.id)
                if (webViews[selectedService.id]?.canGoBack() == true) {
                    webViews[selectedService.id]?.goBack()
                } else {
                    webViews[selectedService.id]?.visibility = View.VISIBLE
                    webViewStates[selectedService.id] = WebViewState.SUCCESS
                }
            }

            webViews[selectedService.id]?.canGoBack() == true -> {
                webViews[selectedService.id]?.goBack()
            }

            else -> exitProcess(0)
        }
    }


    if (showLinkDialog) {
        selectedLink?.let { linkData ->
            MD3LinkOptionsDialog(
                linkData = linkData,
                onDismiss = { showLinkDialog = false },
                onOpenLinkInExternalBrowser = { url ->
                    openInExternalBrowser(context, url)
                    showLinkDialog = false
                },
                onCopyLink = {
                    copyLinkToClipboard(context, linkData.url)
                    showLinkDialog = false
                },
                onShareLink = {
                    shareLink(context, linkData.url, linkData.title)
                    showLinkDialog = false
                })
        }
    }


    var previousSettings by remember { mutableStateOf(settings) }

    val applySettingsToAllWebViews: (Boolean) -> Unit = { reload ->
        webViews.forEach { (_, webView) ->
            updateWebViewSettings(webView, settings, reload)
        }
        previousSettings = settings
    }

    LaunchedEffect(settings, WebViewSecurity.isBlockingEnabled) {
        val connectionBlockingChanged =
            WebViewSecurity.isBlockingEnabled != previousConnectionBlocking

        if (settings != previousSettings || connectionBlockingChanged) {
            val relevantChanges = listOf(
                settings.enableZoom != previousSettings.enableZoom,
                settings.fontSize != previousSettings.fontSize,
            ).any { it }

            if ((relevantChanges || connectionBlockingChanged) && webViews.isNotEmpty()) {
                webViews.forEach { (_, webView) ->
                    updateWebViewSettings(webView, settings, connectionBlockingChanged)
                }
            }

            previousSettings = settings
            previousConnectionBlocking = WebViewSecurity.isBlockingEnabled
        }
    }

    LaunchedEffect(showSettingsScreen) {
        if (!showSettingsScreen) {
            val currentEnabled = settings.enabledServices

            val disabledServices = previousEnabledServices.filter { it !in currentEnabled }
            val enabledServices = currentEnabled.filter { it !in previousEnabledServices }

            if (disabledServices.isNotEmpty() || enabledServices.isNotEmpty()) {
                Log.d("AI_HUB", "Enabled services changed: disabled=${disabledServices.size}, enabled=${enabledServices.size}")

                if (selectedService.id !in currentEnabled) {
                    val firstEnabled = aiServices.firstOrNull { it.id in currentEnabled }
                        ?: aiServices.firstOrNull { it.id == settings.defaultServiceId }
                    if (firstEnabled != null) {
                        selectedService = firstEnabled
                    }
                }

                val toRemove = mutableListOf<String>()
                webViews.forEach { (id, webView) ->
                    if (id !in currentEnabled) {
                        (webView.parent as? ViewGroup)?.removeView(webView)
                        webView.destroy()
                        toRemove.add(id)
                    }
                }

                toRemove.forEach { id ->
                    webViews.remove(id)
                    webViewStates.remove(id)
                    errorStates.remove(id)
                    loadingStates.remove(id)
                    loadingProgress.remove(id)
                    loadedServices.remove(id)
                }

                Log.d("AI_HUB", "Cleaned up ${toRemove.size} disabled services after returning from settings")

                previousEnabledServices = currentEnabled
            }
        } else {
            previousEnabledServices = settings.enabledServices
        }
    }

    if (showSettingsScreen) {
        BackHandler {
            showSettingsScreen = false
            applySettingsToAllWebViews(false)
        }

        SettingsScreen(
            onBack = { showSettingsScreen = false; applySettingsToAllWebViews(false) },
            settingsManager = settingsManager,
            onManageServicesClick = { showManageServices = true }
        )
    }

    if (showManageServices) {
        BackHandler { showManageServices = false }
        ManageAiServicesScreen(
            onBack = { showManageServices = false },
            enabledServices = settings.enabledServices,
            onEnabledServicesChange = { newSet ->
                settingsManager.updateSettings { it.enabledServices = newSet }
            },
            defaultServiceId = settings.defaultServiceId,
            loadLastAiEnabled = settings.loadLastOpenedAI,
            settingsManager = settingsManager
        )
    }
}