package com.foss.aihub.ui.webview

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.foss.aihub.utils.SettingsManager

object WebViewSecurity {
    private lateinit var settings: SettingsManager
    private lateinit var domains: HashSet<String>

    fun init(context: Context, domains: HashSet<String>) {
        if (!::settings.isInitialized) {
            settings = SettingsManager(context.applicationContext)
            this.domains = domains
        }
    }

    fun allowConnectivityForService(url: String): Boolean {
        if (!settings.settingsFlow.value.blockAdsAndTrackers) return true
        if (url.isBlank()) return false

        if (arrayOf("blob:", "about:blank", "data:", "file:", "content:").any(
                url::startsWith
            )
        ) {
            return true
        }

        val uri = url.toUri()
        val scheme = uri.scheme ?: ""
        val host = uri.host ?: ""

        if (host.isEmpty()) return false

        if (scheme != "https") return false

        if (domains.contains(host)) {
            return false
        }

        return true
    }
}