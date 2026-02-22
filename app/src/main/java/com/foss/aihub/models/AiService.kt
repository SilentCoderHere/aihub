package com.foss.aihub.models


import androidx.compose.ui.graphics.Color

data class AiService(
    val id: String,
    val name: String,
    val url: String,
    val category: String,
    val description: String,
    val accentColor: Color
)

data class AppSettings(
    var enableCookies: Boolean = true,
    var enableZoom: Boolean = true,
    var userAgent: String = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36",
    var loadLastOpenedAI: Boolean = true,
    var fontSize: String = "medium",
    var defaultServiceId: String = "chatgpt",
    var theme: String = "auto",
    var maxKeepAlive: Int = 5,
    var enabledServices: Set<String> = emptySet(),
    var serviceOrder: List<String> = emptyList()
)