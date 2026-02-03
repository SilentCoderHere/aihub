package com.foss.aihub.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

    data class ServiceUiState(
        val webViewState: WebViewState = WebViewState.LOADING,
        val isLoading: Boolean = true,
        val progress: Int = 0,
        val error: Pair<Int, String>? = null,
        val isVisible: Boolean = false
    )

    val serviceStates = remember { mutableStateMapOf<String, ServiceUiState>() }
    val webViews = remember { mutableStateMapOf<String, WebView>() }
    val loadedServices = remember { mutableStateSetOf<String>() }
    val serviceAccessOrder = remember { mutableListOf<String>() }

    val initialId = if (settings.loadLastOpenedAI) {
        settingsManager.getLastOpenedService() ?: settings.defaultServiceId
    } else {
        settings.defaultServiceId
    }

    var selectedService by remember {
        mutableStateOf(aiServices.find { it.id == initialId } ?: aiServices.first())
    }

    val currentState by derivedStateOf {
        serviceStates[selectedService.id] ?: ServiceUiState()
    }

    val hasCurrentError by derivedStateOf {
        currentState.error?.let { ErrorType.shouldShowOverlay(it.first) } == true
    }

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

    var currentRoot by remember { mutableStateOf<FrameLayout?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun updateServiceState(serviceId: String, update: (ServiceUiState) -> ServiceUiState) {
        val newState = update(serviceStates[serviceId] ?: ServiceUiState())
        serviceStates[serviceId] = newState
    }

    fun reloadAllActiveTabs() {
        webViews.forEach { (serviceId, webView) ->
            Log.d("AI_HUB", "Reloading tab: $serviceId")
            webView.reload()
            updateServiceState(serviceId) { state ->
                state.copy(
                    webViewState = WebViewState.LOADING,
                    isLoading = true,
                    error = null,
                    progress = 0
                )
            }
        }
    }

    fun enforceWebViewLimit() {
        val limit = settings.maxKeepAlive
        if (limit == Int.MAX_VALUE) {
            return
        }

        val servicesToKeep = serviceAccessOrder.take(limit).toSet()
        val servicesToDestroy = webViews.keys.filterNot { it in servicesToKeep }.toMutableList()

        if (selectedService.id !in servicesToKeep && webViews.containsKey(selectedService.id)) {
            servicesToDestroy.remove(selectedService.id)
        }

        servicesToDestroy.forEach { serviceId ->
            webViews[serviceId]?.let { webView ->
                (webView.parent as? ViewGroup)?.removeView(webView)
                webView.destroy()
                webViews.remove(serviceId)
                serviceStates.remove(serviceId)
                loadedServices.remove(serviceId)
                serviceAccessOrder.remove(serviceId)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_DESTROY -> {
                    webViews.forEach { (_, webView) ->
                        webView.destroy()
                    }
                    webViews.clear()
                    serviceAccessOrder.clear()
                    serviceStates.clear()
                }

                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(selectedService.id) {
        serviceAccessOrder.remove(selectedService.id)
        serviceAccessOrder.add(0, selectedService.id)

        updateServiceState(selectedService.id) { state ->
            state.copy(
                webViewState = WebViewState.LOADING, error = null
            )
        }

        val wv = webViews[selectedService.id]
        if (wv != null) {
            val isActuallyLoading = wv.progress < 100

            updateServiceState(selectedService.id) { state ->
                state.copy(
                    isLoading = isActuallyLoading,
                    webViewState = if (isActuallyLoading) WebViewState.LOADING else WebViewState.SUCCESS,
                    progress = wv.progress
                )
            }

            wv.bringToFront()
        }

        loadedServices.add(selectedService.id)

        enforceWebViewLimit()
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

                    updateServiceState(service.id) { state ->
                        state.copy(
                            webViewState = WebViewState.LOADING,
                            isLoading = true,
                            error = null,
                            progress = 0
                        )
                    }
                },
                webViewStates = serviceStates.mapValues { it.value.webViewState },
                enabledServices = settings.enabledServices,
                serviceOrder = settings.serviceOrder
            )
        }) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing),
            topBar = {
                AiHubAppBar(selectedService = selectedService, onMenuClick = {
                    scope.launch {
                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                    }
                }, onSettingsClick = { showSettingsScreen = true })
            }) { innerPadding ->

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

                            val currentService = selectedService
                            val currentWebView = webViews[currentService.id]
                            if (currentWebView == null) {
                                val newWebView = createWebViewForService(
                                    context = context,
                                    service = currentService,
                                    activity = activity,
                                    settings = settings,
                                    onProgressUpdate = { progress ->
                                        updateServiceState(currentService.id) { state ->
                                            state.copy(progress = progress)
                                        }
                                    },
                                    onLoadingStateChange = { isLoading ->
                                        updateServiceState(currentService.id) { state ->
                                            state.copy(
                                                isLoading = isLoading,
                                                webViewState = if (isLoading) WebViewState.LOADING else WebViewState.SUCCESS
                                            )
                                        }

                                        if (isLoading) {
                                            updateServiceState(currentService.id) { state ->
                                                state.copy(error = null)
                                            }
                                        }
                                    },
                                    onLinkLongPress = { url, title, type ->
                                        selectedLink = LinkData(url, title, type)
                                        showLinkDialog = true
                                    },
                                    onError = { errorCode, description ->
                                        updateServiceState(currentService.id) { state ->
                                            state.copy(
                                                error = errorCode to description,
                                                webViewState = WebViewState.ERROR,
                                                isLoading = false
                                            )
                                        }
                                        webViews[currentService.id]?.visibility = View.GONE
                                    })

                                updateWebViewSettings(newWebView, settings, false)
                                webViews[currentService.id] = newWebView
                                addView(newWebView)
                                newWebView.visibility = View.VISIBLE
                                newWebView.bringToFront()

                                updateServiceState(currentService.id) { state ->
                                    state.copy(
                                        webViewState = WebViewState.LOADING,
                                        isLoading = true,
                                        progress = 0
                                    )
                                }

                                newWebView.loadUrl(currentService.url)
                            } else {
                                if (currentWebView.parent == null) {
                                    addView(currentWebView)
                                }
                                currentWebView.bringToFront()

                                val shouldBeVisible =
                                    serviceStates[currentService.id]?.error == null
                                currentWebView.visibility =
                                    if (shouldBeVisible) View.VISIBLE else View.GONE

                                if (currentWebView.url.isNullOrEmpty() || currentWebView.url == "about:blank") {
                                    currentWebView.loadUrl(currentService.url)
                                }
                            }
                        }
                    }, update = { root ->
                        currentRoot = root

                        val currentService = selectedService
                        if (webViews[currentService.id] == null) {
                            val newWebView = createWebViewForService(
                                context = root.context,
                                service = currentService,
                                activity = activity,
                                settings = settings,
                                onProgressUpdate = { progress ->
                                    updateServiceState(currentService.id) { state ->
                                        state.copy(progress = progress)
                                    }
                                },
                                onLoadingStateChange = { isLoading ->
                                    updateServiceState(currentService.id) { state ->
                                        state.copy(
                                            isLoading = isLoading,
                                            webViewState = if (isLoading) WebViewState.LOADING else WebViewState.SUCCESS
                                        )
                                    }

                                    if (isLoading) {
                                        updateServiceState(currentService.id) { state ->
                                            state.copy(error = null)
                                        }
                                    }
                                },
                                onLinkLongPress = { url, title, type ->
                                    selectedLink = LinkData(url, title, type)
                                    showLinkDialog = true
                                },
                                onError = { errorCode, description ->
                                    updateServiceState(currentService.id) { state ->
                                        state.copy(
                                            error = errorCode to description,
                                            webViewState = WebViewState.ERROR,
                                            isLoading = false
                                        )
                                    }
                                    webViews[currentService.id]?.visibility = View.GONE
                                })

                            updateWebViewSettings(newWebView, settings, false)
                            webViews[currentService.id] = newWebView
                            root.addView(newWebView)
                            newWebView.visibility = View.VISIBLE
                            newWebView.bringToFront()

                            updateServiceState(currentService.id) { state ->
                                state.copy(
                                    webViewState = WebViewState.LOADING,
                                    isLoading = true,
                                    progress = 0
                                )
                            }

                            newWebView.loadUrl(currentService.url)
                        } else {
                            val currentWebView = webViews[currentService.id]!!
                            if (currentWebView.parent == null) {
                                root.addView(currentWebView)
                            }

                            val shouldBeVisible = serviceStates[currentService.id]?.error == null
                            currentWebView.visibility =
                                if (shouldBeVisible) View.VISIBLE else View.GONE
                            currentWebView.bringToFront()

                            if (currentWebView.url.isNullOrEmpty() || currentWebView.url == "about:blank") {
                                currentWebView.loadUrl(currentService.url)
                            }
                        }
                    }, modifier = Modifier.fillMaxSize()
                    )

                    if (currentState.isLoading && !hasCurrentError) {
                        LoadingOverlay(
                            isVisible = true,
                            isRefreshing = false,
                            serviceName = selectedService.name,
                            accentColor = selectedService.accentColor,
                            progress = currentState.progress,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (hasCurrentError && currentState.error != null) {
                        val (errorCode, errorMessage) = currentState.error!!
                        ErrorOverlay(
                            errorType = ErrorType.fromErrorCode(errorCode),
                            errorCode = errorCode,
                            errorMessage = errorMessage,
                            serviceName = selectedService.name,
                            accentColor = selectedService.accentColor,
                            onRetry = {
                                updateServiceState(selectedService.id) { state ->
                                    state.copy(
                                        error = null,
                                        webViewState = WebViewState.LOADING,
                                        isLoading = true,
                                        progress = 0
                                    )
                                }
                                webViews[selectedService.id]?.reload()
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
                updateServiceState(selectedService.id) { state ->
                    state.copy(error = null)
                }
                if (webViews[selectedService.id]?.canGoBack() == true) {
                    webViews[selectedService.id]?.goBack()
                } else {
                    webViews[selectedService.id]?.visibility = View.VISIBLE
                    updateServiceState(selectedService.id) { state ->
                        state.copy(webViewState = WebViewState.SUCCESS)
                    }
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

    var previousMaxKeepAlive by remember { mutableIntStateOf(settings.maxKeepAlive) }

    LaunchedEffect(settings, WebViewSecurity.isBlockingEnabled) {
        val connectionBlockingChanged =
            WebViewSecurity.isBlockingEnabled != previousConnectionBlocking
        val limitChanged = settings.maxKeepAlive != previousMaxKeepAlive

        if (settings != previousSettings || connectionBlockingChanged || limitChanged) {
            val relevantChanges = listOf(
                settings.enableZoom != previousSettings.enableZoom,
                settings.fontSize != previousSettings.fontSize,
            ).any { it }

            if ((relevantChanges || connectionBlockingChanged) && webViews.isNotEmpty()) {
                webViews.forEach { (_, webView) ->
                    updateWebViewSettings(webView, settings, connectionBlockingChanged)
                }
            }

            if (limitChanged) {
                enforceWebViewLimit()
                previousMaxKeepAlive = settings.maxKeepAlive
            }

            previousSettings = settings
            previousConnectionBlocking = WebViewSecurity.isBlockingEnabled
        }
    }

    LaunchedEffect(showSettingsScreen) {
        if (!showSettingsScreen) {
            enforceWebViewLimit()

            val currentEnabled = settings.enabledServices

            val disabledServices = previousEnabledServices.filter { it !in currentEnabled }
            val enabledServices = currentEnabled.filter { it !in previousEnabledServices }

            if (disabledServices.isNotEmpty() || enabledServices.isNotEmpty()) {
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

                        serviceAccessOrder.remove(id)
                    }
                }

                toRemove.forEach { id ->
                    webViews.remove(id)
                    serviceStates.remove(id)
                    loadedServices.remove(id)
                }

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
            onBack = {
            showSettingsScreen = false
            applySettingsToAllWebViews(false)
            enforceWebViewLimit()
        },
            settingsManager = settingsManager,
            onManageServicesClick = { showManageServices = true },
            onClearCache = {
                try {
                    context.cacheDir?.deleteRecursively()

                    val webViewCacheDir = java.io.File(context.cacheDir, "webviewCache")
                    webViewCacheDir.deleteRecursively()

                    val webViewDatabaseDir = java.io.File(context.filesDir, "webview")
                    webViewDatabaseDir.deleteRecursively()

                    WebView(context).apply {
                        clearCache(true)
                        clearHistory()
                        destroy()
                    }
                } catch (e: Exception) {
                    Log.e("AI_HUB", "Error clearing cache", e)
                }

                Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
            },
            onClearData = {
                webViews.forEach { (serviceId, webView) ->
                    webView.clearCache(true)
                    webView.clearHistory()
                    webView.clearFormData()

                    updateServiceState(serviceId) { state ->
                        state.copy(
                            error = null, isLoading = false, progress = 0
                        )
                    }
                }

                val cookieManager = android.webkit.CookieManager.getInstance()

                cookieManager.removeAllCookies { _ ->
                    cookieManager.flush()

                    scope.launch {
                        reloadAllActiveTabs()
                        Toast.makeText(context, "All data cleared", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    if (showManageServices) {
        BackHandler { showManageServices = false }
        ManageAiServicesScreen(
            onBack = {
            showManageServices = false
            enforceWebViewLimit()
        },
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