package com.foss.aihub.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.foss.aihub.models.AiService
import com.foss.aihub.utils.SettingsManager
import com.foss.aihub.utils.aiServices
import java.util.Collections

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAiServicesScreen(
    onBack: () -> Unit,
    enabledServices: Set<String>,
    defaultServiceId: String,
    loadLastAiEnabled: Boolean,
    onEnabledServicesChange: (Set<String>) -> Unit,
    settingsManager: SettingsManager
) {
    val settings by settingsManager.settingsFlow.collectAsState()
    val orderedServices = remember(settings, aiServices.toList()) {
        settings.serviceOrder.mapNotNull { id -> aiServices.find { it.id == id } }
    }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredServices = orderedServices.filter {
        if (searchQuery.isEmpty()) true
        else it.name.contains(
            searchQuery, ignoreCase = true
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(), topBar = {
            if (isSearching) {
                TopAppBar(
                    title = {
                    val focusRequester = remember { FocusRequester() }
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text("Search AI services") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        )
                    )
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                }, navigationIcon = {
                    IconButton(onClick = { isSearching = false; searchQuery = "" }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                }, actions = {
                    IconButton(onClick = { isSearching = true }) {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
                )
            } else {
                TopAppBar(
                    title = { Text("Manage AI Services") }, navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                }, actions = {
                    IconButton(onClick = { isSearching = true }) {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
                )
            }
        }, containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = filteredServices, key = { _, service -> service.id }) { _, service ->
                val originalIndex = orderedServices.indexOf(service)
                val isEnabled = service.id in enabledServices
                val isDefault = service.id == defaultServiceId
                val isOnlyEnabled = enabledServices.size == 1 && isEnabled
                val canDisable =
                    if (loadLastAiEnabled) !isOnlyEnabled else !isDefault && !isOnlyEnabled

                val isFirst = originalIndex == 0
                val isLast = originalIndex == orderedServices.lastIndex

                AiServiceItem(
                    service = service,
                    isEnabled = isEnabled,
                    canToggle = canDisable,
                    isDefault = isDefault,
                    loadLastAiEnabled = loadLastAiEnabled,
                    isFirst = isFirst,
                    isLast = isLast,
                    showReorder = searchQuery.isEmpty(),
                    onToggle = { enabled ->
                        val newSet = enabledServices.toMutableSet().apply {
                            if (enabled) add(service.id) else remove(service.id)
                        }
                        onEnabledServicesChange(newSet)
                    },
                    onMoveUp = {
                        if (!isFirst) {
                            val newOrder = settings.serviceOrder.toMutableList().apply {
                                Collections.swap(this, originalIndex, originalIndex - 1)
                            }
                            settingsManager.updateSettings { it.serviceOrder = newOrder }
                        }
                    },
                    onMoveDown = {
                        if (!isLast) {
                            val newOrder = settings.serviceOrder.toMutableList().apply {
                                Collections.swap(this, originalIndex, originalIndex + 1)
                            }
                            settingsManager.updateSettings { it.serviceOrder = newOrder }
                        }
                    })
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    tonalElevation = 0.5.dp
                ) {
                    Text(
                        text = when {
                            loadLastAiEnabled -> "At least one AI must remain enabled"
                            else -> "At least one AI must be enabled • Default cannot be disabled"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun AiServiceItem(
    service: AiService,
    isEnabled: Boolean,
    canToggle: Boolean,
    isDefault: Boolean,
    loadLastAiEnabled: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    showReorder: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Surface(
        onClick = { if (canToggle || !isEnabled) onToggle(!isEnabled) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = when {
            isEnabled -> service.accentColor.copy(alpha = 0.08f)
            else -> MaterialTheme.colorScheme.surfaceContainerLowest
        },
        border = when {
            !isEnabled -> BorderStroke(
                0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            else -> null
        },
        tonalElevation = if (isEnabled) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = service.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isEnabled) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isDefault && !loadLastAiEnabled) {
                        DefaultBadgeCompact()
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = service.accentColor.copy(alpha = if (isEnabled) 0.12f else 0.08f),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Text(
                            text = service.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = service.accentColor.copy(alpha = if (isEnabled) 0.9f else 0.6f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (!canToggle && isEnabled) {
                        Text(
                            text = if (isDefault && !loadLastAiEnabled) "• Default"
                            else "• Last enabled",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    } else {
                        Text(
                            text = "• ${service.description}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (isEnabled) 0.7f else 0.5f
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            TrailingControlsCompact(
                isEnabled = isEnabled,
                canToggle = canToggle,
                isFirst = isFirst,
                isLast = isLast,
                showReorder = showReorder,
                onToggle = onToggle,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown
            )
        }
    }
}

@Composable
private fun DefaultBadgeCompact() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.wrapContentWidth()
    ) {
        Text(
            "Default",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TrailingControlsCompact(
    isEnabled: Boolean,
    canToggle: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    showReorder: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Switch(
            checked = isEnabled,
            onCheckedChange = { if (canToggle || !isEnabled) onToggle(it) },
            enabled = canToggle || !isEnabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.scale(0.8f)
        )

        if (showReorder) {
            Column {
                IconButton(
                    onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Rounded.ArrowDropUp,
                        contentDescription = "Move up",
                        tint = if (!isFirst) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Rounded.ArrowDropDown,
                        contentDescription = "Move down",
                        tint = if (!isLast) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}