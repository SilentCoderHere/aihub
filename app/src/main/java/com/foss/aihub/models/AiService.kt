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
    var enableZoom: Boolean = true,
    var loadLastOpenedAI: Boolean = true,
    var fontSize: String = "medium",
    var defaultServiceId: String = "chatgpt",
    var theme: String = "auto",
    var desktopView: Boolean = false,
    var thirdPartyCookies: Boolean = false,
    var maxKeepAlive: Int = 5,
    var enabledServices: Set<String> = emptySet(),
    var serviceOrder: List<String> = emptyList(),
    var favoriteServices: Set<String> = emptySet()
)