package com.foss.aihub.utils

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.foss.aihub.models.AiService

private fun generateId(name: String): String {
    return name.lowercase().replace("\\s+".toRegex(), "")
}

fun loadAiServices(rawList: List<List<String>>) {
    aiServices = rawList.mapNotNull { row ->
        if (row.size < 5) return@mapNotNull null

        val name = row[0].trim()
        if (name.isBlank()) return@mapNotNull null

        val id = generateId(name)
        val url = row[1].trim()
        val category = row.getOrNull(2)?.trim() ?: "Unknown"
        val description = row.getOrNull(3)?.trim() ?: "Unknown"
        val hex = row[4].trim().removePrefix("#").uppercase()

        val accentColor = try {
            val colorHex = when (hex.length) {
                6 -> "FF$hex"
                8 -> hex
                else -> "FF6366F1"
            }
            Color(colorHex.toLong(16))
        } catch (_: Exception) {
            Color(0xFF6366F1)
        }

        AiService(
            id = id,
            name = name,
            url = url,
            category = category,
            description = description,
            accentColor = accentColor
        )
    }
}

fun refreshAiServicesFromSettings(context: Context) {
    val settingsManager = SettingsManager(context)
    val raw = settingsManager.getAiServices()

    loadAiServices(raw)
}