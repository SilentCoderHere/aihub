package com.foss.aihub.ui.webview

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.foss.aihub.utils.SettingsManager

object WebViewSecurity {
    private lateinit var settings: SettingsManager

    fun init(context: Context) {
        if (!::settings.isInitialized) {
            settings = SettingsManager(context.applicationContext)
        }
    }

    fun allowConnectivityForService(serviceId: String, url: String): Boolean {
        if (!settings.settingsFlow.value.blockUnnecessaryConnections) return true
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

        if (host.isEmpty()) return true

        if (scheme != "https") return false

        val alwaysBlocked = settings.getAlwaysBlockedDomains().getOrDefault(serviceId, emptyList())

        if (alwaysBlocked.any { host == it || host.endsWith(".$it") }) {
            return false
        }

        val commonAuth = settings.getCommonAuthDomains()
        if (commonAuth.any { host == it || host.endsWith(".$it") || host.startsWith("accounts.google") }) {
            return true
        }

        val allowed = settings.getServiceDomains()[serviceId] ?: return false
        if (allowed.any { host == it || host.endsWith(".$it") }) {
            Log.d("AI_HUB", "✅ Allowed: $url")
            return true
        }
        return false
    }
}