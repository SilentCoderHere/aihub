package com.foss.aihub.utils

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.head
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CloudDataHandler {
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000L
            connectTimeoutMillis = 10_000L
            socketTimeoutMillis = 15_000L
        }
    }

    suspend fun getEtag(url: String): String? {
        val response = client.head(url)
        return response.headers["ETag"]
    }

    private suspend fun getResponse(url: String, etag: String?): Pair<String, String?>? {
        val newEtag = getEtag(url)
        if (etag != null) {
            if (newEtag == etag) {
                return null
            }
        }

        val response = client.get(url)

        return when (response.status.value) {
            200 -> {
                val body = response.body<String>()
                body to newEtag
            }

            else -> throw ResponseException(response, "HTTP ${response.status}")
        }
    }

    suspend fun updateDomains(context: Context): Boolean {
        val settingsManager = SettingsManager(context)
        val etag = updateData(context, DOMAINS_FILE, settingsManager.getDomainsEtag())

        if (etag != null) {
            settingsManager.saveDomainsEtag(etag)
            return true
        }

        return false
    }

    suspend fun updateAiServices(context: Context): Boolean {
        val settingsManager = SettingsManager(context)
        val etag = updateData(context, AI_SERVICES_FILE, settingsManager.getAiServicesEtag())

        if (etag != null) {
            settingsManager.saveAiServicesEtag(etag)
            return true
        }

        return false
    }

    suspend fun updateData(context: Context, fileName: String, etag: String?): String? {
        return withContext(Dispatchers.IO) {
            val response = getResponse(url = CLOUD_BASE_URL + fileName, etag = etag)
            if (response != null) {
                val (body, newEtag) = response
                context.openFileOutput(fileName, Context.MODE_PRIVATE).use { outputStream ->
                    outputStream.write(body.toByteArray())
                }
                return@withContext newEtag
            }
            return@withContext null
        }
    }
}