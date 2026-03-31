package com.foss.aihub.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foss.aihub.R
import com.foss.aihub.ui.components.Md3TopAppBar
import com.foss.aihub.utils.SettingsManager
import com.foss.aihub.utils.aiServices
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settingsManager: SettingsManager,
    onManageServices: () -> Unit,
    onCustomInjection: () -> Unit,
    onClearCache: () -> Unit,
    onClearData: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by settingsManager.settingsFlow.collectAsState()

    var showFontSizeOptions by remember { mutableStateOf(false) }
    var loadLastAi by remember { mutableStateOf(settings.loadLastOpenedAI) }
    var selectedFontSize by remember { mutableStateOf(settings.fontSize) }
    var defaultServiceId by remember { mutableStateOf(settings.defaultServiceId) }
    var enabledServices by remember { mutableStateOf(settings.enabledServices) }
    var enableZoom by remember { mutableStateOf(settings.enableZoom) }
    var limitSimultaneousAIs by remember { mutableStateOf(settings.maxKeepAlive != Int.MAX_VALUE) }
    var maxKeepAlive by remember {
        mutableIntStateOf(
            if (settings.maxKeepAlive == Int.MAX_VALUE) 5 else settings.maxKeepAlive
        )
    }
    var desktopView by remember { mutableStateOf(settings.desktopView) }
    var thirdPartyCookies by remember { mutableStateOf(settings.thirdPartyCookies) }
    var blockUnnecessaryConnections by remember {
        mutableStateOf(settings.blockUnnecessaryConnections)
    }

    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    val fontSizeOptions = listOf("x-small", "small", "medium", "large", "x-large")

    val orderedServices = remember(settings) {
        settings.serviceOrder.filter { it in settings.enabledServices }
            .mapNotNull { id -> aiServices.find { it.id == id } }
    }

    LaunchedEffect(loadLastAi) {
        settingsManager.updateSettings { it.loadLastOpenedAI = loadLastAi }
    }

    LaunchedEffect(defaultServiceId) {
        settingsManager.updateSettings { it.defaultServiceId = defaultServiceId }
    }

    LaunchedEffect(enabledServices) {
        settingsManager.updateSettings { it.enabledServices = enabledServices }
        if (defaultServiceId !in enabledServices && enabledServices.isNotEmpty()) {
            defaultServiceId = enabledServices.first()
        }
    }

    LaunchedEffect(selectedFontSize) {
        settingsManager.updateSettings { it.fontSize = selectedFontSize }
    }

    LaunchedEffect(enableZoom) {
        settingsManager.updateSettings { it.enableZoom = enableZoom }
    }

    LaunchedEffect(limitSimultaneousAIs, maxKeepAlive) {
        val value = if (limitSimultaneousAIs) maxKeepAlive else Int.MAX_VALUE
        settingsManager.updateSettings { it.maxKeepAlive = value }
    }

    LaunchedEffect(desktopView) {
        settingsManager.updateSettings { it.desktopView = desktopView }
    }

    LaunchedEffect(thirdPartyCookies) {
        settingsManager.updateSettings { it.thirdPartyCookies = thirdPartyCookies }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(), topBar = {
        Md3TopAppBar(
            title = stringResource(R.string.title_settings), onBack = onBack
        )
    }, snackbarHost = {
        SnackbarHost(hostState = snackbarHostState)
    }, containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
//            item {
//                SettingsCard {
//                    Column {
//                        SettingItem(
//                            title = stringResource(R.string.setting_theme),
//                            description = stringResource(R.string.setting_theme_description),
//                            icon = Icons.Outlined.Palette,
//                            iconColor = MaterialTheme.colorScheme.primary,
//                        )
//
//                        val themeOptions = listOf("auto", "light", "dark")
//
//                        var selectedTheme by remember { mutableStateOf(settings.theme) }
//
//                        LaunchedEffect(selectedTheme) {
//                            settingsManager.updateSettings { it.theme = selectedTheme }
//                        }
//
//                        LaunchedEffect(settings) {
//                            selectedTheme = settings.theme
//                        }
//
//                        SingleChoiceSegmentedButtonRow(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(horizontal = 16.dp, vertical = 12.dp)
//                        ) {
//                            themeOptions.forEachIndexed { index, option ->
//                                SegmentedButton(
//                                    selected = selectedTheme == option,
//                                    onClick = { selectedTheme = option },
//                                    shape = SegmentedButtonDefaults.itemShape(
//                                        index = index, count = themeOptions.size
//                                    ),
//                                    colors = SegmentedButtonDefaults.colors(
//                                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
//                                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
//                                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
//                                    ),
//                                    enabled = false
//                                ) {
//                                    Text(
//                                        text = when (option) {
//                                        "auto" -> stringResource(R.string.theme_auto)
//                                        "light" -> stringResource(R.string.theme_light)
//                                        "dark" -> stringResource(R.string.theme_dark)
//                                        else -> option.replaceFirstChar {
//                                            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
//                                        }
//                                    }, style = MaterialTheme.typography.labelLarge)
//                                }
//                            }
//                        }
//
//                        Text(
//                            text = stringResource(R.string.msg_theme_switch_disabled),
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = MaterialTheme.colorScheme.error,
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(horizontal = 16.dp, vertical = 8.dp)
//                        )
//                    }
//                }
//            }

            item {
                Text(
                    text = stringResource(R.string.section_preferences),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                SettingsCard {
                    Column {
                        SettingItem(
                            title = stringResource(R.string.setting_restore_last_ai),
                            description = stringResource(R.string.setting_restore_last_ai_description),
                            icon = Icons.Outlined.Restore,
                            iconColor = MaterialTheme.colorScheme.primary
                        ) {
                            Switch(
                                checked = loadLastAi,
                                onCheckedChange = { loadLastAi = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }

                        AnimatedVisibility(
                            visible = !loadLastAi,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = stringResource(R.string.setting_default_ai),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    supportingContent = {
                                        Text(stringResource(R.string.setting_default_ai_description))
                                    },
                                    leadingContent = {
                                        Icon(
                                            Icons.Outlined.Home,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    orderedServices.forEach { service ->
                                        ElevatedFilterChip(
                                            selected = defaultServiceId == service.id,
                                            onClick = { defaultServiceId = service.id },
                                            label = {
                                                Text(
                                                    service.name,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    maxLines = 1
                                                )
                                            },
                                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                                containerColor = service.accentColor.copy(alpha = 0.08f),
                                                labelColor = service.accentColor
                                            )
                                        )
                                    }

                                    if (orderedServices.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.msg_no_ai_services_enabled),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        SettingItem(
                            title = stringResource(R.string.setting_manage_ai_services),
                            description = stringResource(R.string.setting_manage_ai_services_description),
                            icon = Icons.Outlined.Apps,
                            iconColor = MaterialTheme.colorScheme.primary,
                            onClick = onManageServices,
                            trailingContent = {
                                Icon(
                                    Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.section_performance),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                SettingsCard {
                    Column {
                        SettingItem(
                            title = stringResource(R.string.setting_memory_management),
                            description = stringResource(R.string.setting_memory_management_description),
                            icon = Icons.Outlined.Layers,
                            iconColor = MaterialTheme.colorScheme.primary
                        ) {
                            Switch(
                                checked = limitSimultaneousAIs,
                                onCheckedChange = { limitSimultaneousAIs = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }

                        AnimatedVisibility(
                            visible = limitSimultaneousAIs,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    start = 56.dp, end = 16.dp, bottom = 8.dp
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        "${stringResource(R.string.label_max)}:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    SingleChoiceSegmentedButtonRow(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        (1..5).forEach { value ->
                                            SegmentedButton(
                                                selected = maxKeepAlive == value,
                                                onClick = { maxKeepAlive = value },
                                                shape = SegmentedButtonDefaults.itemShape(
                                                    index = value - 1, count = 5
                                                ),
                                                colors = SegmentedButtonDefaults.colors(
                                                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            ) {
                                                Text(
                                                    "$value",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.section_webview),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            item {
                SettingsCard {
                    Column {
                        SettingItem(
                            title = stringResource(R.string.setting_pinch_to_zoom),
                            description = stringResource(R.string.setting_pinch_to_zoom_description),
                            icon = Icons.Outlined.ZoomIn,
                            iconColor = MaterialTheme.colorScheme.primary
                        ) {
                            Switch(
                                checked = enableZoom,
                                onCheckedChange = { enableZoom = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        SettingItem(
                            title = stringResource(R.string.setting_desktop_mode),
                            description = stringResource(R.string.setting_desktop_mode_description),
                            icon = Icons.Outlined.Computer,
                            iconColor = MaterialTheme.colorScheme.primary
                        ) {
                            Switch(
                                checked = desktopView,
                                onCheckedChange = { desktopView = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        SettingItem(
                            title = stringResource(R.string.setting_third_party_cookies),
                            description = stringResource(R.string.setting_third_party_cookies_description),
                            icon = Icons.Outlined.Cookie,
                            iconColor = MaterialTheme.colorScheme.primary
                        ) {
                            Switch(
                                checked = thirdPartyCookies,
                                onCheckedChange = { thirdPartyCookies = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        SettingItem(
                            title = stringResource(R.string.setting_custom_injection),
                            description = stringResource(R.string.setting_custom_injection_description),
                            icon = Icons.Outlined.Code,
                            iconColor = MaterialTheme.colorScheme.primary,
                            onClick = onCustomInjection,
                            trailingContent = {
                                Icon(
                                    Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            })

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        SettingItem(
                            title = stringResource(R.string.setting_font_size),
                            description = stringResource(R.string.setting_font_size_description),
                            icon = Icons.Outlined.TextIncrease,
                            iconColor = MaterialTheme.colorScheme.primary,
                            onClick = { showFontSizeOptions = !showFontSizeOptions }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    selectedFontSize.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Icon(
                                    if (showFontSizeOptions) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = showFontSizeOptions,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    start = 56.dp, end = 16.dp, bottom = 12.dp
                                )
                            ) {
                                fontSizeOptions.forEach { option ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedFontSize = option }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = when (option) {
                                                "x-small" -> stringResource(R.string.label_font_size_xsmall)
                                                "small" -> stringResource(R.string.label_font_size_small)
                                                "medium" -> stringResource(R.string.label_font_size_medium)
                                                "large" -> stringResource(R.string.label_font_size_large)
                                                "x-large" -> stringResource(R.string.label_font_size_xlarge)
                                                else -> option
                                            },
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (selectedFontSize == option) {
                                            Icon(
                                                Icons.Outlined.CheckCircle,
                                                null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.section_security_privacy),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            item {
                SettingsCard {
                    SettingItem(
                        title = stringResource(R.string.setting_block_trackers_ads),
                        description = stringResource(R.string.setting_block_trackers_ads_description),
                        icon = Icons.Outlined.Block,
                        iconColor = MaterialTheme.colorScheme.primary
                    ) {
                        Switch(
                            checked = blockUnnecessaryConnections, onCheckedChange = {
                                blockUnnecessaryConnections = it
                                settingsManager.updateSettings { settings ->
                                    settings.blockUnnecessaryConnections = it
                                }
                            }, colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.section_cloud_updates),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            item {
                SettingsCard {
                    Column {
                        val isCloudUpdatesEnabled = settings.updateFrequencyDays != -1
                        var isEnabled by remember { mutableStateOf(isCloudUpdatesEnabled) }
                        var frequencyDays by remember {
                            mutableIntStateOf(if (isCloudUpdatesEnabled) settings.updateFrequencyDays else 1)
                        }
                        var showFrequencyOptions by remember { mutableStateOf(false) }

                        LaunchedEffect(isEnabled, frequencyDays) {
                            val days = if (isEnabled) frequencyDays else -1
                            settingsManager.updateSettings { it.updateFrequencyDays = days }
                        }

                        SettingItem(
                            title = stringResource(R.string.setting_enable_cloud_updates),
                            description = stringResource(R.string.setting_enable_cloud_updates_description),
                            icon = Icons.Outlined.CloudSync,
                            iconColor = MaterialTheme.colorScheme.primary
                        ) {
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { isEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }

                        AnimatedVisibility(
                            visible = isEnabled,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                SettingItem(
                                    title = stringResource(R.string.setting_update_frequency),
                                    description = stringResource(R.string.setting_update_frequency_description),
                                    icon = Icons.Outlined.CloudSync,
                                    iconColor = MaterialTheme.colorScheme.primary,
                                    onClick = { showFrequencyOptions = !showFrequencyOptions }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = when (frequencyDays) {
                                                1 -> stringResource(R.string.label_frequency_1_day)
                                                3 -> stringResource(R.string.label_frequency_3_days)
                                                7 -> stringResource(R.string.label_frequency_1_week)
                                                else -> stringResource(R.string.label_frequency_1_day)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Icon(
                                            if (showFrequencyOptions) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                AnimatedVisibility(
                                    visible = showFrequencyOptions,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(
                                            start = 56.dp, end = 16.dp, bottom = 12.dp
                                        )
                                    ) {
                                        val frequencyOptions = listOf(1, 3, 7)
                                        frequencyOptions.forEach { days ->
                                            Row(modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    frequencyDays = days
                                                    showFrequencyOptions = false
                                                }
                                                .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = when (days) {
                                                        1 -> stringResource(R.string.label_frequency_1_day)
                                                        3 -> stringResource(R.string.label_frequency_3_days)
                                                        7 -> stringResource(R.string.label_frequency_1_week)
                                                        else -> ""
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (frequencyDays == days) {
                                                    Icon(
                                                        Icons.Outlined.CheckCircle,
                                                        null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.section_storage),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            item {
                SettingsCard {
                    Column {
                        SettingItem(
                            title = stringResource(R.string.action_clear_all_cache),
                            description = stringResource(R.string.action_clear_all_cache_description),
                            icon = Icons.Outlined.Delete,
                            iconColor = MaterialTheme.colorScheme.primary,
                            onClick = { showClearCacheDialog = true },
                            trailingContent = {
                                Icon(Icons.Outlined.ChevronRight, null)
                            },
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        SettingItem(
                            title = stringResource(R.string.action_clear_all_data),
                            description = stringResource(R.string.action_clear_all_data_description),
                            icon = Icons.Outlined.DeleteSweep,
                            iconColor = MaterialTheme.colorScheme.error,
                            onClick = { showClearDataDialog = true },
                            trailingContent = {
                                Icon(
                                    Icons.Outlined.ChevronRight,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.action_clear_cache)) },

            text = { Text(stringResource(R.string.msg_clear_all_cache_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearCache()
                        showClearCacheDialog = false
                    },
                ) {
                    Text(stringResource(R.string.action_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = {
                Text(
                    stringResource(R.string.action_clear_all_data),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    stringResource(R.string.msg_clear_all_data_confirmation)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearData()
                        showClearDataDialog = false
                    },
                ) {
                    Text(
                        stringResource(R.string.action_clear_everything),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingItem(
    title: String,
    description: String? = null,
    icon: ImageVector,
    iconColor: Color,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            Text(
                title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium
            )
        },
        supportingContent = description?.let {
            {
                Text(
                    it, style = MaterialTheme.typography.bodySmall
                )
            }
        },
        leadingContent = {
            Icon(
                icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = trailingContent,
        modifier = Modifier
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 4.dp),
        colors = ListItemDefaults.colors(
            headlineColor = MaterialTheme.colorScheme.onSurface,
            supportingColor = MaterialTheme.colorScheme.onSurfaceVariant,
            leadingIconColor = iconColor,
            trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
    )
}

@Composable
private fun SettingsCard(
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        tonalElevation = 1.dp
    ) {
        Column(content = content)
    }
}