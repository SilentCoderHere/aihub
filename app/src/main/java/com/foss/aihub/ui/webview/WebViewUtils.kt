package com.foss.aihub.ui.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import androidx.webkit.WebViewFeature.PROXY_OVERRIDE
import com.foss.aihub.MainActivity
import com.foss.aihub.R
import com.foss.aihub.models.AiService
import com.foss.aihub.models.AppSettings
import com.foss.aihub.models.LinkType
import com.foss.aihub.ui.webview.DownloadHandler.handleDownload
import com.foss.aihub.utils.USER_AGENT_DESKTOP
import com.foss.aihub.utils.USER_AGENT_MOBILE
import com.foss.aihub.utils.cleanTrackingParams
import com.foss.aihub.utils.extractLinkTitle
import java.util.concurrent.Executors
import kotlin.math.roundToInt

object WebViewProxyHelper {
    private val executor = Executors.newSingleThreadExecutor()

    fun setProxy(
        host: String, port: Int, proxyType: String,
        onSuccess: () -> Unit = {}, onError: (String) -> Unit = {},
    ) {
        if (!WebViewFeature.isFeatureSupported(PROXY_OVERRIDE)) {
            onError("PROXY_OVERRIDE not supported – fallback not reliable for WebView")
            setProxyViaSystemProperties(
                host, port, proxyType == "socks"
            )
            return
        }

        try {
            val builder = ProxyConfig.Builder()

            val proxyRule = when (proxyType.lowercase()) {
                "http" -> "http://$host:$port"
                "socks", "socks5" -> "socks://$host:$port"
                else -> throw IllegalArgumentException("Unsupported proxy type: $proxyType")
            }

            builder.addProxyRule(proxyRule)

            val proxyConfig = builder.build()
            ProxyController.getInstance().setProxyOverride(proxyConfig, executor) {
                onSuccess()
            }
        } catch (e: Exception) {
            onError("Failed to set proxy: ${e.message}")
        }
    }

    fun clearProxy(onCleared: () -> Unit = {}) {
        if (WebViewFeature.isFeatureSupported(PROXY_OVERRIDE)) {
            ProxyController.getInstance().clearProxyOverride(executor, onCleared)
        }
    }

    private fun setProxyViaSystemProperties(host: String, port: Int, isSocks: Boolean) {
        if (isSocks) {
            System.setProperty("socksProxyHost", host)
            System.setProperty("socksProxyPort", port.toString())
        } else {
            System.setProperty("http.proxyHost", host)
            System.setProperty("http.proxyPort", port.toString())
            System.setProperty("https.proxyHost", host)
            System.setProperty("https.proxyPort", port.toString())
        }
    }
}

fun createWebViewForService(
    context: Context,
    service: AiService,
    activity: MainActivity,
    settings: AppSettings,
    onProgressUpdate: (Int) -> Unit,
    onLoadingStateChange: (Boolean) -> Unit,
    onLinkLongPress: (String, String, LinkType) -> Unit,
    onError: (Int, String) -> Unit,
    onJsAlertRequest: (String?, JsResult?) -> Unit,
    onJsPromptRequest: (String?, JsResult?) -> Unit,
    onJsConfirmRequest: (String?, JsResult?) -> Unit,
    onJsBeforeUnloadRequest: (String?, JsResult?) -> Unit,
): WebView {
    val linkHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val bundle = msg.data
            val url = bundle.getString("url") ?: return
            val title = bundle.getString("title") ?: ""

            val cleanUrl = cleanTrackingParams(url)
            val displayTitle = title.ifBlank { extractLinkTitle(context, cleanUrl) }


            onLinkLongPress(cleanUrl, displayTitle, LinkType.HYPERLINK)
        }
    }

    return WebView(context).apply {
        addJavascriptInterface(BlobDownloadInterface(context), "AndroidBlobHandler")
        addJavascriptInterface(ShareInterface(context), "AndroidWebShare")
        addJavascriptInterface(
            BlobDownloadInterface(context), "BlobDownloader"
        )

        setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            handleDownload(this@apply, url, userAgent, contentDisposition, mimeType)
        }

        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        )

        webViewClient = CustomWebViewClient(
            context = activity,
            onProgressUpdate = onProgressUpdate,
            onLoadingStateChange = onLoadingStateChange,
            service = service,
            onError = onError,
        )

        webChromeClient = CustomWebChromeClient(
            context = activity,
            onProgressUpdate = onProgressUpdate,
            onJsAlertRequest = onJsAlertRequest,
            onJsPromptRequest = onJsPromptRequest,
            onJsConfirmRequest = onJsConfirmRequest,
            onJsBeforeUnloadRequest = onJsBeforeUnloadRequest,
            mainWebView = this,
        )

        setOnLongClickListener { view ->
            val webView = view as WebView
            val result = webView.hitTestResult

            when (result.type) {
                WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                    val url = result.extra?.let { cleanTrackingParams(it) }
                        ?: return@setOnLongClickListener false

                    val title = extractLinkTitle(context, url)
                    onLinkLongPress(url, title, LinkType.HYPERLINK)
                    true
                }

                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    val message = linkHandler.obtainMessage()
                    webView.requestFocusNodeHref(message)
                    true
                }

                WebView.HitTestResult.IMAGE_TYPE -> {
                    val url = result.extra ?: return@setOnLongClickListener false
                    onLinkLongPress(url, context.getString(R.string.label_image), LinkType.IMAGE)
                    true
                }

                WebView.HitTestResult.EMAIL_TYPE -> {
                    val email = result.extra ?: return@setOnLongClickListener false
                    onLinkLongPress(
                        "mailto:$email", context.getString(R.string.label_email), LinkType.EMAIL
                    )
                    true
                }

                WebView.HitTestResult.PHONE_TYPE -> {
                    val phone = result.extra ?: return@setOnLongClickListener false
                    onLinkLongPress(
                        "tel:$phone", context.getString(R.string.label_phone), LinkType.PHONE
                    )
                    true
                }

                else -> false
            }
        }

        setBackgroundColor(Color.TRANSPARENT)
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        isFocusable = true
        isFocusableInTouchMode = true
        post { requestFocus(View.FOCUS_DOWN) }

        updateWebViewSettings(this, settings, reload = false)
        Log.d("AI_HUB", "Loading URL for ${service.name}: ${service.url}")

        if (settings.isProxy && settings.proxyHost.isNotBlank() && settings.proxyPort.isNotBlank()) {
            val port = settings.proxyPort.toIntOrNull() ?: 9050
            WebViewProxyHelper.setProxy(
                host = settings.proxyHost,
                port = port,
                proxyType = settings.proxyType,
                onSuccess = {
                    Log.d(
                        "AI_HUB", "Proxy set: ${settings.proxyHost}:$port (${settings.proxyType})"
                    )
                    post {
                        loadUrl(service.url)
                    }
                },
                onError = { msg ->
                    post {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        loadUrl(service.url)
                    }
                },
            )
        } else {
            WebViewProxyHelper.clearProxy {
                Log.d("AI_HUB", "Proxy cleared")
                post {
                    loadUrl(service.url)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
fun updateWebViewSettings(
    webView: WebView, settings: AppSettings, reload: Boolean
) {
    webView.settings.apply {
        val percent = settings.fontSizePercentage
        val baseSize = 16
        val baseFixedSize = 15
        val scale = percent / 100f
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, settings.thirdPartyCookies)

        setSupportZoom(settings.enableZoom)
        builtInZoomControls = settings.enableZoom
        displayZoomControls = false
        textZoom = percent
        defaultFontSize = (baseSize * scale).roundToInt()
        defaultFixedFontSize = (baseFixedSize * scale).roundToInt()
        javaScriptEnabled = true
        domStorageEnabled = true
        mediaPlaybackRequiresUserGesture = false
        javaScriptCanOpenWindowsAutomatically = true
        setSupportMultipleWindows(true)
        loadWithOverviewMode = true
        useWideViewPort = true
        allowFileAccess = true
        allowContentAccess = true
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        cacheMode = WebSettings.LOAD_DEFAULT
        userAgentString = if (settings.desktopView) USER_AGENT_DESKTOP else USER_AGENT_MOBILE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isAlgorithmicDarkeningAllowed = true
        }
    }

    webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

    if (reload) webView.reload()
}