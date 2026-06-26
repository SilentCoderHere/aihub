package com.foss.aihub.utils

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.foss.aihub.R
import com.foss.aihub.models.AiService
import com.foss.aihub.models.ModifiedServiceInfo
import com.foss.aihub.models.UpdateResult
import com.foss.aihub.models.loadServices
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale
import kotlin.math.abs

fun String.capitalizeFirstLetter(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

fun Context.readAssetsFile(fileName: String): String {
    return try {
        val inputStream = assets.open(fileName)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            stringBuilder.append(line).append("\n")
        }
        reader.close()
        stringBuilder.toString()
    } catch (_: Exception) {
        ""
    }
}

fun generateAccentColorFromName(name: String): Color {
    val hash = name.hashCode().toUInt()
    val hue = (hash % 360u).toFloat()
    val lightness = 0.55f + ((hash / 360u) % 30u).toFloat() / 100f
    return hslToColor(hue, lightness)
}

private fun hslToColor(hue: Float, lightness: Float): Color {
    val c = (1 - abs(2 * lightness - 1)) * 0.7f
    val x = c * (1 - abs((hue / 60f) % 2 - 1))
    val m = lightness - c / 2

    val (r, g, b) = when (hue.toInt()) {
        in 0..59 -> Triple(c, x, 0f)
        in 60..119 -> Triple(x, c, 0f)
        in 120..179 -> Triple(0f, c, x)
        in 180..239 -> Triple(0f, x, c)
        in 240..299 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = r + m, green = g + m, blue = b + m, alpha = 1f
    )
}

suspend fun performServiceUpdate(
    context: Context, settingsManager: SettingsManager
): UpdateResult? = withContext(Dispatchers.IO) {
    val oldServices = loadServices(context)
    CloudDataHandler.updateAiServices(context)
    val newServices = loadServices(context)

    val oldMap = oldServices.associateBy { it.name }
    val newMap = newServices.associateBy { it.name }

    val added = newServices.filter { it.name !in oldMap.keys }
    val removed = oldServices.filter { it.name !in newMap.keys }

    val modified = buildList {
        oldServices.forEach { old ->
            newMap[old.name]?.let { new ->
                val changes = mutableListOf<String>()
                if (old.url != new.url) changes.add("URL: ${old.url} → ${new.url}")
                if (old.pricing != new.pricing) changes.add("Pricing: ${old.pricing} → ${new.pricing}")
                if (old.privacy != new.privacy) changes.add("Privacy: ${old.privacy} → ${new.privacy}")
                if (old.loginRequired != new.loginRequired) {
                    val oldVal = if (old.loginRequired) "Yes" else "No"
                    val newVal = if (new.loginRequired) "Yes" else "No"
                    changes.add("Login Required: $oldVal → $newVal")
                }
                if (old.bestFor != new.bestFor) {
                    val oldStr = old.bestFor.joinToString(", ")
                    val newStr = new.bestFor.joinToString(", ")
                    changes.add("Best For: $oldStr → $newStr")
                }
                if (changes.isNotEmpty()) {
                    add(ModifiedServiceInfo(new, changes.map { it.capitalizeFirstLetter() }))
                }
            }
        }
    }

    val newCategoriesSet =
        (newServices.map { it.category }.toSet() - oldServices.map { it.category }.toSet())

    val hasChanges =
        added.isNotEmpty() || removed.isNotEmpty() || modified.isNotEmpty() || newCategoriesSet.isNotEmpty()
    if (!hasChanges) return@withContext null

    settingsManager.updateSettings { currentSettings ->
        val newEnabled = currentSettings.enabledServices.toMutableSet()
        val newOrder = currentSettings.serviceOrder.toMutableList()

        removed.forEach { service ->
            newEnabled.remove(service.name)
            newOrder.removeAll { it == service.name }
        }

        added.forEach { service ->
            if (!newOrder.contains(service.name)) {
                newOrder.add(service.name)
            }
        }

        if (currentSettings.enableNewServicesByDefault) {
            val preferredCategories = currentSettings.preferredCategories
            val preferredPrices = currentSettings.preferredPrices
            val preferredPrivacy = currentSettings.preferredPrivacy
            val preferredLoginRequired = currentSettings.preferredLoginRequired

            added.forEach { service ->
                val catOk =
                    preferredCategories.isEmpty() || preferredCategories.contains(service.category)
                val priceOk = preferredPrices.isEmpty() || preferredPrices.contains(service.pricing)
                val privacyOk =
                    preferredPrivacy.isEmpty() || preferredPrivacy.contains(service.privacy)
                val loginOk =
                    preferredLoginRequired == null || service.loginRequired == preferredLoginRequired
                if (catOk && priceOk && privacyOk && loginOk) {
                    newEnabled.add(service.name)
                }
            }
        }

        currentSettings.enabledServices = newEnabled
        currentSettings.serviceOrder = newOrder
    }

    UpdateResult(added, removed, modified, newCategoriesSet)
}

fun getUpdateErrorMessage(context: Context, error: Exception): String {
    return when {
        error.isNoNetworkError() -> context.getString(R.string.error_no_connection_message)
        error is SocketTimeoutException || error is HttpRequestTimeoutException -> context.getString(
            R.string.error_timeout_message
        )

        error is SerializationException -> context.getString(R.string.error_seriliaziation_message)
        error is ResponseException -> {
            val code = error.response.status.value
            context.getString(R.string.error_request_failed_message, code)
        }

        error is IOException -> context.getString(R.string.error_no_connection_message)
        else -> context.getString(R.string.error_something_went_wrong_message)
    }
}

fun normalizeUrl(url: String): String {
    return url.trim().lowercase().removePrefix("https://").removePrefix("http://").trimEnd('/')
}

fun isDuplicateName(name: String, existingServices: List<AiService>): Boolean {
    val normalized = name.trim().lowercase()
    return existingServices.any { it.name.trim().lowercase() == normalized }
}

fun isDuplicateUrl(url: String, existingServices: List<AiService>): Boolean {
    val normalized = normalizeUrl(url)
    if (normalized.isBlank()) return false
    return existingServices.any { normalizeUrl(it.url) == normalized }
}
