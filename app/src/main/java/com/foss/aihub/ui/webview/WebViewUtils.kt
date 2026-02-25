package com.foss.aihub.ui.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import com.foss.aihub.MainActivity
import com.foss.aihub.models.AiService
import com.foss.aihub.models.AppSettings
import com.foss.aihub.models.LinkType
import com.foss.aihub.utils.USER_AGENT
import com.foss.aihub.utils.cleanTrackingParams
import com.foss.aihub.utils.extractLinkTitle

@SuppressLint("SetJavaScriptEnabled")
fun createWebViewForService(
    context: Context,
    service: AiService,
    activity: MainActivity,
    settings: AppSettings,
    onProgressUpdate: (Int) -> Unit,
    onLoadingStateChange: (Boolean) -> Unit,
    onLinkLongPress: (String, String, LinkType) -> Unit,
    onError: (Int, String) -> Unit
): WebView {
    return WebView(context).apply {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)

        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        )
        webViewClient = ProgressTrackingWebViewClient(
            onProgressUpdate = onProgressUpdate,
            onLoadingStateChange = onLoadingStateChange,
            service = service,
            onError = onError
        )

        webChromeClient = ProgressTrackingWebChromeClient(
            onProgressUpdate = onProgressUpdate, activity = activity
        )

        setOnLongClickListener { view ->
            val result = (view as WebView).hitTestResult
            when (result.type) {
                WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                    var url = result.extra ?: return@setOnLongClickListener false
                    url = cleanTrackingParams(context, url)
                    val title = extractLinkTitle(url)
                    onLinkLongPress(url, title, LinkType.HYPERLINK)
                    true
                }

                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    val url = result.extra ?: return@setOnLongClickListener false
                    onLinkLongPress(url, "Image Link", LinkType.IMAGE)
                    true
                }

                WebView.HitTestResult.IMAGE_TYPE -> {
                    val url = result.extra ?: return@setOnLongClickListener false
                    onLinkLongPress(url, "Image", LinkType.IMAGE)
                    true
                }

                WebView.HitTestResult.EMAIL_TYPE -> {
                    val email = result.extra ?: return@setOnLongClickListener false
                    onLinkLongPress("mailto:$email", "Email", LinkType.EMAIL)
                    true
                }

                WebView.HitTestResult.PHONE_TYPE -> {
                    val phone = result.extra ?: return@setOnLongClickListener false
                    onLinkLongPress("tel:$phone", "Phone", LinkType.PHONE)
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
        loadUrl(service.url)
    }
}

@SuppressLint("SetJavaScriptEnabled")
fun updateWebViewSettings(
    webView: WebView, settings: AppSettings, reload: Boolean
) {
    webView.settings.apply {
        setSupportZoom(settings.enableZoom)
        builtInZoomControls = settings.enableZoom
        displayZoomControls = false

        when (settings.fontSize.lowercase()) {
            "x-small" -> {
                textZoom = 80
                defaultFontSize = 14
                defaultFixedFontSize = 13
            }

            "small" -> {
                textZoom = 90
                defaultFontSize = 15
                defaultFixedFontSize = 14
            }

            "medium" -> {
                textZoom = 100
                defaultFontSize = 16
                defaultFixedFontSize = 15
            }

            "large" -> {
                textZoom = 110
                defaultFontSize = 18
                defaultFixedFontSize = 16
            }

            "x-large" -> {
                textZoom = 120
                defaultFontSize = 20
                defaultFixedFontSize = 17
            }

            else -> {
                textZoom = 100
                defaultFontSize = 16
                defaultFixedFontSize = 15
            }
        }

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
        userAgentString = USER_AGENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isAlgorithmicDarkeningAllowed = true
        }
    }
    webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

    if (reload) webView.reload()
}
