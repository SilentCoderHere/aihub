package com.foss.aihub.ui.webview

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.foss.aihub.MainActivity
import com.foss.aihub.models.AiService
import com.foss.aihub.utils.readAssetsFile
import java.io.ByteArrayInputStream

class CustomWebViewClient(
    val context: MainActivity,
    private val onProgressUpdate: (Int) -> Unit,
    private val onLoadingStateChange: (Boolean) -> Unit,
    private val service: AiService,
    private val onError: (Int, String) -> Unit
) : WebViewClient() {
    private var hasErrorOccurred = false

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        hasErrorOccurred = false
        onLoadingStateChange(true)
        onProgressUpdate(0)
        Log.d("AI_HUB", "Page started: ${service.name} - $url")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (!hasErrorOccurred) {
            onProgressUpdate(100)
            onLoadingStateChange(false)

            if (view != null) {
                injectBlobInterceptor(view)
                injectShareInterceptor(view)
                injectCustomJs(view)
                injectCustomCss(view)
            }

            Log.d("AI_HUB", "Page finished: ${service.name} - $url")
        }
    }

    override fun onReceivedError(
        view: WebView?, request: WebResourceRequest?, error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            hasErrorOccurred = true
            onProgressUpdate(0)
            onLoadingStateChange(false)
            val errorCode = error?.errorCode ?: return
            val errorDescription = error.description?.toString() ?: "Unknown error"
            onError(errorCode, errorDescription)
            Log.e("WEBVIEW", "❌ Error loading ${service.name}: $errorCode - $errorDescription")
        }
    }

    override fun onReceivedHttpError(
        view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        // Ignoring server error
        if (errorResponse?.statusCode in 500..599) return
        if (request?.isForMainFrame == true) {
            hasErrorOccurred = true
            onProgressUpdate(0)
            onLoadingStateChange(false)
            val statusCode = errorResponse?.statusCode ?: return
            onError(errorResponse.statusCode, "HTTP Error $statusCode")
            Log.e("WEBVIEW", "❌ HTTP Error loading ${service.name}: $statusCode")
        }
    }

    override fun shouldInterceptRequest(
        view: WebView?, request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null
        if (!WebViewSecurity.allowConnectivityForService(url)) {
            return WebResourceResponse(
                "text/html",
                "UTF-8",
                403,
                "Forbidden",
                emptyMap(),
                ByteArrayInputStream(ByteArray(0))
            )
        }
        return null
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?, request: WebResourceRequest?
    ): Boolean {
        val url = request?.url?.toString() ?: return false
        return !WebViewSecurity.allowConnectivityForService(url)
    }

    private fun injectBlobInterceptor(view: WebView) {
        val script = context.readAssetsFile("blobDownloadInterceptor.txt").trimIndent()
        view.evaluateJavascript(script) { result ->
            Log.d("WebView", "Blob interceptor injection result: $result")
        }
    }

    private fun injectShareInterceptor(view: WebView) {
        val script = context.readAssetsFile("webSharePolyfill.txt").trimIndent()
        view.evaluateJavascript(script) { result ->
            Log.d("WebView", "Share injection result: $result")
        }
    }

    private fun injectCustomJs(view: WebView) {
        val script = context.settingsManager.settingsFlow.value.customJs.trimIndent()
        if (script.isNotEmpty()) {
            view.evaluateJavascript(script) { result ->
                Log.d("WebView", "Custom js injection result: $result")
            }
        }
    }

    private fun injectCustomCss(view: WebView) {
        val css = context.settingsManager.settingsFlow.value.customCss.trimIndent()
        if (css.isNotEmpty()) {
            val escapedCss = css.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
            val script = context.readAssetsFile("customCss.txt").replace("{{CSS}}", escapedCss)
            view.evaluateJavascript(script, null)
        }
    }
}