package com.foss.aihub.ui.screens

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DrawerValue.Closed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.foss.aihub.MainActivity
import com.foss.aihub.R
import com.foss.aihub.models.JsDialog
import com.foss.aihub.models.LinkData
import com.foss.aihub.models.ServiceUiState
import com.foss.aihub.models.WebViewState
import com.foss.aihub.ui.components.AiHubAppBar
import com.foss.aihub.ui.components.DrawerContent
import com.foss.aihub.ui.components.ErrorOverlay
import com.foss.aihub.ui.components.ErrorType
import com.foss.aihub.ui.components.LoadingOverlay
import com.foss.aihub.ui.screens.dialogs.JsDialogHandler
import com.foss.aihub.ui.screens.dialogs.LinkOptionsDialog
import com.foss.aihub.ui.screens.dialogs.UpdateDialog
import com.foss.aihub.ui.webview.createWebViewForService
import com.foss.aihub.ui.webview.updateWebViewSettings
import com.foss.aihub.utils.DetailedUpdateDetails
import com.foss.aihub.utils.ServiceChanges
import com.foss.aihub.utils.UpdateResult
import com.foss.aihub.utils.aiServices
import com.foss.aihub.utils.checkUpdate
import com.foss.aihub.utils.copyLinkToClipboard
import com.foss.aihub.utils.openInExternalBrowser
import com.foss.aihub.utils.shareLink
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun AiHubApp(context: MainActivity) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = Closed)
    val scope = rememberCoroutineScope()

    val settingsManager = remember { context.settingsManager }
    val settings by settingsManager.settingsFlow.collectAsState()

    val initialId = if (settings.loadLastOpenedAI) {
        settingsManager.getLastOpenedService() ?: settings.defaultServiceId
    } else {
        settings.defaultServiceId
    }

    var selectedService by remember {
        mutableStateOf(aiServices.find { it.id == initialId } ?: aiServices.first())
    }

    var backPressedTime by remember { mutableLongStateOf(0L) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var selectedLink by remember { mutableStateOf<LinkData?>(null) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showManageServices by remember { mutableStateOf(false) }
    var showCustomInjectionScreen by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    var previousEnabledServices by remember { mutableStateOf(settings.enabledServices) }
    var previousDesktopView by remember { mutableStateOf(settings.desktopView) }
    var previousThirdPartyCookies by remember { mutableStateOf(settings.thirdPartyCookies) }
    var previousConnectionBlocking by remember { mutableStateOf(settings.blockUnnecessaryConnections) }

    val serviceStates = remember { mutableStateMapOf<String, ServiceUiState>() }
    val webViews = remember { mutableStateMapOf<String, WebView>() }
    val loadedServices = remember { mutableStateSetOf<String>() }
    val serviceAccessOrder = remember { mutableListOf<String>() }

    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateResult by remember {
        mutableStateOf(
            UpdateResult(
                null, DetailedUpdateDetails(
                    ServiceChanges(emptyList(), emptyList(), emptyList()),
                    ServiceChanges(emptyList(), emptyList(), emptyList()),
                    ServiceChanges(emptyList(), emptyList(), emptyList()),
                    ServiceChanges(emptyList(), emptyList(), emptyList()),
                    ServiceChanges(emptyList(), emptyList(), emptyList())
                )
            ),
        )
    }

    var jsDialog by remember { mutableStateOf<JsDialog?>(null) }

    val currentState by remember {
        derivedStateOf {
            serviceStates[selectedService.id] ?: ServiceUiState()
        }
    }

    val hasCurrentError by remember {
        derivedStateOf {
            currentState.error?.let { ErrorType.shouldShowOverlay(it.first) } == true
        }
    }

    if (settingsManager.getLastUpdatedDate() == null) {
        settingsManager.saveLastUpdatedDate()
    }

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

    UpdateDialog(
        showDialog = showUpdateDialog,
        updateResult = updateResult,
        onDismiss = { showUpdateDialog = false },
    )

    LaunchedEffect(true) {
        val updateFrequency = settings.updateFrequencyDays
        if (updateFrequency != -1) {
            val lastDate = settingsManager.getLastUpdatedDate()
            val daysBetween = ChronoUnit.DAYS.between(lastDate, LocalDate.now())

            if (daysBetween >= updateFrequency) {
                scope.launch {
                    try {
                        updateResult = checkUpdate(context)
                        if (updateResult.message != null) {
                            showUpdateDialog = true
                        }
                        settingsManager.saveLastUpdatedDate()
                    } catch (_: Exception) {
                        // Silent fail
                    }
                }
            }
        }
    }

    LaunchedEffect(configuration.orientation) {
        if (drawerState.isOpen) {
            drawerState.close()
        }
    }

    LaunchedEffect(selectedService) {
        if (settings.loadLastOpenedAI) {
            settingsManager.saveLastOpenedService(selectedService.id)
        }
    }

    LaunchedEffect(selectedService.id) {
        serviceAccessOrder.remove(selectedService.id)
        serviceAccessOrder.add(0, selectedService.id)

        val wv = webViews[selectedService.id]
        if (wv != null) {
            val isActuallyLoading = wv.progress < 100
            val currentError = serviceStates[selectedService.id]?.error
            updateServiceState(selectedService.id) { state ->
                state.copy(
                    isLoading = isActuallyLoading, webViewState = when {
                        currentError != null -> WebViewState.ERROR
                        isActuallyLoading -> WebViewState.LOADING
                        else -> WebViewState.SUCCESS
                    }, progress = wv.progress
                )
            }
            wv.bringToFront()
        } else {
            updateServiceState(selectedService.id) { state ->
                state.copy(
                    webViewState = WebViewState.LOADING,
                    error = null,
                    isLoading = true,
                    progress = 0
                )
            }
        }

        loadedServices.add(selectedService.id)
        enforceWebViewLimit()
    }

    ModalNavigationDrawer(
        drawerState = drawerState, gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            DrawerContent(
                context = context,
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
                serviceOrder = settings.serviceOrder,
                favoriteServices = settings.favoriteServices,
                onToggleFavorite = { serviceId ->
                    settingsManager.updateSettings { current ->
                        current.favoriteServices = if (serviceId in current.favoriteServices) {
                            current.favoriteServices - serviceId
                        } else {
                            current.favoriteServices + serviceId
                        }
                    }
                },
            )
        },
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            topBar = {
                AiHubAppBar(
                    selectedService = selectedService,
                    onMenuClick = {
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },
                    onSettingsClick = { showSettingsScreen = true },
                    onAboutClick = { showAbout = true },
                    onClearSiteData = {
                        val serviceId = selectedService.id
                        val webView = webViews[serviceId]
                        if (webView != null) {
                            val currentUrl = webView.url ?: selectedService.url
                            val domain = try {
                                currentUrl.toUri().host ?: ""
                            } catch (_: Exception) {
                                ""
                            }

                            val cookieManager = CookieManager.getInstance()
                            val cookies = cookieManager.getCookie(currentUrl)
                            if (cookies != null) {
                                val cookieNames = cookies.split(";").mapNotNull { cookie ->
                                    cookie.trim().split("=").firstOrNull()?.trim()
                                }
                                for (name in cookieNames) {
                                    cookieManager.setCookie(
                                        currentUrl,
                                        "$name=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/"
                                    )
                                    cookieManager.setCookie(
                                        currentUrl,
                                        "$name=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/; Domain=$domain"
                                    )
                                }
                                cookieManager.flush()
                            }

                            webView.evaluateJavascript(
                                "try { localStorage.clear(); sessionStorage.clear(); } catch(e) {}",
                                null
                            )

                            webView.clearCache(true)
                            webView.clearFormData()
                            webView.clearHistory()

                            updateServiceState(serviceId) { state ->
                                state.copy(
                                    webViewState = WebViewState.LOADING,
                                    isLoading = true,
                                    error = null,
                                    progress = 0
                                )
                            }
                            webView.loadUrl(selectedService.url)

                            Toast.makeText(
                                context, context.getString(
                                    R.string.msg_site_data_cleared, selectedService.name
                                ), Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    loadedServiceIds = webViews.keys,
                    allServices = aiServices,
                    onReload = { service ->
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
                    onServiceSelected = { service ->
                        selectedService = service
                    },
                    onKillService = { service ->
                        val serviceId = service.id
                        if (serviceId == selectedService.id) return@AiHubAppBar

                        webViews[serviceId]?.let { webView ->
                            (webView.parent as? ViewGroup)?.removeView(webView)
                            webView.destroy()
                            webViews.remove(serviceId)
                        }

                        serviceStates.remove(serviceId)
                        loadedServices.remove(serviceId)
                        serviceAccessOrder.remove(serviceId)

                        Toast.makeText(
                            context,
                            context.getString(R.string.msg_service_killed, service.name),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp),
                )
            }) { innerPadding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                        FrameLayout(ctx).apply {
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
                                    context = this.context,
                                    service = currentService,
                                    activity = context,
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
                                    },
                                    onJsAlertRequest = { message, result ->
                                        message?.let {
                                            jsDialog = JsDialog.Alert(it, result!!)
                                        }
                                    },
                                    onJsConfirmRequest = { message, result ->
                                        message?.let {
                                            jsDialog = JsDialog.Confirm(it, result!!)
                                        }
                                    },
                                    onJsPromptRequest = { message, result ->
                                        message?.let {
                                            jsDialog = JsDialog.Prompt(it, result!!)
                                        }
                                    },
                                    onJsBeforeUnloadRequest = { message, result ->
                                        message?.let {
                                            jsDialog = JsDialog.BeforeUnload(it, result!!)
                                        }
                                    },
                                )

                                updateWebViewSettings(newWebView, settings, false)
                                webViews[currentService.id] = newWebView
                                addView(newWebView)
                                newWebView.visibility = View.VISIBLE
                                newWebView.bringToFront()
                            } else {
                                if (currentWebView.parent == null) {
                                    addView(currentWebView)
                                }
                                currentWebView.bringToFront()

                                val shouldBeVisible =
                                    serviceStates[currentService.id]?.error == null
                                currentWebView.visibility =
                                    if (shouldBeVisible) View.VISIBLE else View.GONE
                            }
                        }
                    }, update = { root ->
                        val currentService = selectedService
                        if (webViews[currentService.id] == null) {
                            val newWebView = createWebViewForService(
                                context = root.context,
                                service = currentService,
                                activity = context,
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
                                },
                                onJsAlertRequest = { message, result ->
                                    message?.let {
                                        jsDialog = JsDialog.Alert(it, result!!)
                                    }
                                },
                                onJsConfirmRequest = { message, result ->
                                    message?.let {
                                        jsDialog = JsDialog.Confirm(it, result!!)
                                    }
                                },
                                onJsPromptRequest = { message, result ->
                                    message?.let {
                                        jsDialog = JsDialog.Prompt(it, result!!)
                                    }
                                },
                                onJsBeforeUnloadRequest = { message, result ->
                                    message?.let {
                                        jsDialog = JsDialog.BeforeUnload(it, result!!)
                                    }
                                },
                            )

                            updateWebViewSettings(newWebView, settings, false)
                            webViews[currentService.id] = newWebView
                            root.addView(newWebView)
                            newWebView.visibility = View.VISIBLE
                            newWebView.bringToFront()
                        } else {
                            val currentWebView = webViews[currentService.id]!!
                            if (currentWebView.parent == null) {
                                root.addView(currentWebView)
                            }

                            val shouldBeVisible = serviceStates[currentService.id]?.error == null
                            currentWebView.visibility =
                                if (shouldBeVisible) View.VISIBLE else View.GONE
                            currentWebView.bringToFront()
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
            drawerState.isOpen -> {
                scope.launch { drawerState.close() }
            }

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

            else -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - backPressedTime < 2000L) {
                    context.finish()
                } else {
                    backPressedTime = currentTime
                    Toast.makeText(
                        context, context.getString(R.string.msg_press_again), Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    if (showLinkDialog) {
        selectedLink?.let { linkData ->
            LinkOptionsDialog(
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
                },
            )
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

    LaunchedEffect(
        settings, settings.blockUnnecessaryConnections
    ) {
        val connectionBlockingChanged =
            settings.blockUnnecessaryConnections != previousConnectionBlocking
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
            previousConnectionBlocking = settings.blockUnnecessaryConnections
        }
    }

    LaunchedEffect(showSettingsScreen) {
        if (!showSettingsScreen) {
            val desktopViewChanged = settings.desktopView != previousDesktopView
            val thirdPartyCookiesChanged = settings.thirdPartyCookies != previousThirdPartyCookies

            val reloadRequired = desktopViewChanged || thirdPartyCookiesChanged

            if (reloadRequired) {
                applySettingsToAllWebViews(true)
                Log.d("AI_HUB", "Reloading all webviews...")
            }

            previousDesktopView = settings.desktopView
            previousThirdPartyCookies = settings.thirdPartyCookies

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

    if (jsDialog != null) {
        BackHandler {
            when (val dialog = jsDialog) {
                is JsDialog.Alert -> dialog.result.cancel()
                is JsDialog.Confirm -> dialog.result.cancel()
                is JsDialog.Prompt -> dialog.result.cancel()
                is JsDialog.BeforeUnload -> dialog.result.cancel()
                null -> {}
            }
            jsDialog = null
        }

        JsDialogHandler(
            dialog = jsDialog, context = context, onDismiss = { jsDialog = null })
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
            onManageServices = { showManageServices = true },
            onCustomInjection = { showCustomInjectionScreen = true },
            onClearCache = {
                try {
                    context.cacheDir?.deleteRecursively()

                    val webViewCacheDir = File(context.cacheDir, "webviewCache")
                    webViewCacheDir.deleteRecursively()

                    val webViewDatabaseDir = File(context.filesDir, "webview")
                    webViewDatabaseDir.deleteRecursively()

                    WebView(context).apply {
                        clearCache(true)
                        clearHistory()
                        destroy()
                    }

                    scope.launch {
                        Toast.makeText(
                            context,
                            context.getString(R.string.msg_cache_cleared),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e("AI_HUB", "Error clearing cache", e)
                }
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

                val cookieManager = CookieManager.getInstance()

                cookieManager.removeAllCookies { _ ->
                    cookieManager.flush()

                    scope.launch {
                        reloadAllActiveTabs()
                        Toast.makeText(
                            context,
                            context.getString(R.string.msg_all_data_cleared),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
        )
    }

    if (showCustomInjectionScreen) {
        BackHandler { showCustomInjectionScreen = false }
        CustomInjectionScreen(
            context = context,
            onBack = { showCustomInjectionScreen = false },
            settingsManager = settingsManager
        )
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

    if (showAbout) {
        BackHandler { showAbout = false }
        AboutScreen(context, onBack = { showAbout = false })
    }
}