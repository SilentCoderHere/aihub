package com.foss.aihub.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.foss.aihub.utils.SettingsManager

@Composable
fun AiHubTheme(
    context: Context, settingsManager: SettingsManager, content: @Composable () -> Unit
) {
    val settings by settingsManager.settingsFlow.collectAsState()

    val useDarkTheme = when (settings.theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()   // "auto"
    }

    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }

        else -> {
            if (useDarkTheme) darkColorScheme()
            else lightColorScheme()
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)

            val shouldUseLightIcons = !useDarkTheme

            if (controller.isAppearanceLightStatusBars != shouldUseLightIcons) {
                controller.isAppearanceLightStatusBars = shouldUseLightIcons
            }
            if (controller.isAppearanceLightNavigationBars != shouldUseLightIcons) {
                controller.isAppearanceLightNavigationBars = shouldUseLightIcons
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme, typography = Typography, content = content
    )
}