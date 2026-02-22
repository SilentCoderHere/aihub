package com.foss.aihub.utils

import android.content.Context
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object ConfigUpdater {
    private const val BASE_URL = "https://silentcoderhere.github.io/aihub-config-data/"
    private const val AI_SERVICES_FILE = "ai_services_list.json"
    private const val DOMAIN_AND_RULES_FILE = "domain_filtering_rules.json"
    private val client = HttpClient(CIO)

    suspend fun updateBothIfNeeded(context: Context): Pair<Boolean, Boolean> =
        withContext(Dispatchers.IO) {
            val domainMessage = updateDomainRules(context)
            val servicesMessage = updateAiServices(context)
            domainMessage to servicesMessage
        }

    private suspend fun updateDomainRules(context: Context): Boolean {
        try {
            val response = client.get(BASE_URL + DOMAIN_AND_RULES_FILE)
            if (!response.status.isSuccess()) {
                return false
            }

            val json = response.bodyAsText()
            val remote = Json.decodeFromString<RemoteDomainConfig>(json)

            val manager = SettingsManager(context)
            val current = manager.getDomainConfigVersion() ?: "0.0.0"

            return if (remote.version == current) {
                false
            } else {
                manager.saveDomainConfig(
                    version = remote.version,
                    serviceDomains = remote.service_domains,
                    alwaysBlockedDomains = remote.always_blocked_domains,
                    commonAuthDomains = remote.common_auth_domains,
                    trackingParams = remote.tracking_params
                )
                true
            }
        } catch (_: Exception) {
            return false
        }
    }

    private suspend fun updateAiServices(context: Context): Boolean {
        try {
            val response = client.get(BASE_URL + AI_SERVICES_FILE)
            if (!response.status.isSuccess()) {
                return false
            }

            val json = response.bodyAsText()
            val remote = Json.decodeFromString<AiServiceConfig>(json)

            val manager = SettingsManager(context)
            val currentVersion = manager.getAiVersion() ?: "0.0.0"

            if (remote.version == currentVersion) {
                return false
            }

            val remoteServices = remote.ai_services

            manager.saveAiServices(
                version = remote.version, aiServices = remoteServices
            )

            manager.cleanupAndFixServices(context)

            Log.d("AI_HUB", "AI services updated to v${aiServices}")

            return true
        } catch (_: Exception) {
            return false
        }
    }
}