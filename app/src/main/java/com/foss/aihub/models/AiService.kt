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
    var theme: String = "auto",
    var loadLastOpenedAI: Boolean = true,
    var defaultServiceId: String = "chatgpt",
    var serviceOrder: List<String> = emptyList(),
    var enabledServices: Set<String> = emptySet(),
    var favoriteServices: Set<String> = emptySet(),
    var maxKeepAlive: Int = 5,
    var enableZoom: Boolean = true,
    var desktopView: Boolean = false,
    var thirdPartyCookies: Boolean = false,
    var fontSize: String = "medium",
    var updateFrequencyDays: Int = 3,
    var blockUnnecessaryConnections: Boolean = true,
    var customCss: String = "",
    var customJs: String = ""
)