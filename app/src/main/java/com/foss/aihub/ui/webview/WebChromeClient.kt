package com.foss.aihub.ui.webview

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.foss.aihub.MainActivity

open class CustomWebChromeClient(
    private val context: MainActivity,
    private val onProgressUpdate: (Int) -> Unit,
    private val onJsAlertRequest: (String?, JsResult?) -> Unit,
    private val mainWebView: WebView? = null
) : WebChromeClient() {
    private val activeWebViews = mutableListOf<WebView>()

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressUpdate(newProgress)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        context.launchFileChooser(filePathCallback, fileChooserParams)
        return true
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        val resources = request.resources
        Log.d("AI_HUB", "WebView requesting permission for: ${resources.joinToString()}")
        context.runOnUiThread {
            context.requestWebViewPermissions(request)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateWindow(
        view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?
    ): Boolean {
        Log.d(
            "AI_HUB", "onCreateWindow called - isDialog: $isDialog, isUserGesture: $isUserGesture"
        )

        val transport = resultMsg?.obj as? WebView.WebViewTransport

        return if (mainWebView != null) {
            Log.d("AI_HUB", "Creating dummy WebView for window.open")
            val dummyWebView = WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    setSupportMultipleWindows(true)
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    allowFileAccess = true
                    allowContentAccess = true
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?, request: WebResourceRequest?
                    ): Boolean {
                        request?.url?.let { url ->
                            Log.d("AI_HUB", "Redirecting window.open URL to main WebView: $url")
                            mainWebView.loadUrl(url.toString())
                        }
                        return true
                    }
                }

                activeWebViews.add(this)
            }

            transport?.webView = dummyWebView
            resultMsg?.sendToTarget()

            dummyWebView.postDelayed({
                activeWebViews.remove(dummyWebView)
                dummyWebView.destroy()
            }, 1000)

            true
        } else {
            Log.e("AI_HUB", "Failed to create dummy WebView for window.open")
            false
        }
    }

    override fun onCloseWindow(window: WebView?) {
        super.onCloseWindow(window)
        window?.destroy()
        activeWebViews.remove(window)
    }

    override fun onJsAlert(
        view: WebView?, url: String?, message: String?, result: JsResult?
    ): Boolean {
        context.runOnUiThread {
            onJsAlertRequest(message, result)
        }
        return true
    }
}