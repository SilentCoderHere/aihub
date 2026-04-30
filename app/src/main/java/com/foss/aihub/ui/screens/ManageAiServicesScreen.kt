package com.foss.aihub.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.foss.aihub.R
import com.foss.aihub.models.AiService
import com.foss.aihub.utils.SettingsManager
import com.foss.aihub.utils.aiServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAiServicesScreen(
    onBack: () -> Unit,
    enabledServices: Set<String>,
    defaultServiceId: String,
    loadLastAiEnabled: Boolean,
    onEnabledServicesChange: (Set<String>) -> Unit,
    settingsManager: SettingsManager,
    onRequestNewAi: () -> Unit
) {
    val settings by settingsManager.settingsFlow.collectAsState()
    val baseServices = remember { aiServices.toList() }

    val orderedServices = remember(settings.serviceOrder, baseServices) {
        settings.serviceOrder.mapNotNull { id -> baseServices.find { it.id == id } }
    }

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isReorderMode by remember { mutableStateOf(false) }

    remember(orderedServices, searchQuery) {
        if (searchQuery.isEmpty()) orderedServices else orderedServices.filter {
            it.name.contains(
                searchQuery, ignoreCase = true
            )
        }
    }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var currentOrder by remember(orderedServices) { mutableStateOf(orderedServices) }

    LaunchedEffect(settings.serviceOrder) {
        currentOrder = settings.serviceOrder.mapNotNull { id -> baseServices.find { it.id == id } }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(), topBar = {
        if (isSearching) {
            SearchTopAppBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onCloseSearch = { isSearching = false; searchQuery = "" })
        } else {
            RegularTopAppBar(
                title = stringResource(R.string.setting_manage_ai_services),
                onBack = onBack,
                onSearchClick = { isSearching = true },
                isReorderMode = isReorderMode,
                onToggleReorder = { isReorderMode = !isReorderMode })
        }
    }, floatingActionButton = {
        ExtendedFloatingActionButton(
            onClick = onRequestNewAi,
            icon = {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
            },
            text = { Text(stringResource(R.string.label_request_new_ai)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(10.dp),
            modifier = Modifier.padding(16.dp)
        )
    }, containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val displayedServices = remember(orderedServices, searchQuery) {
            if (searchQuery.isEmpty()) orderedServices else orderedServices.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = displayedServices, key = { _, service -> service.id },
            ) { index, service ->
                val isEnabled = service.id in enabledServices
                val isDefault = service.id == defaultServiceId
                val isOnlyEnabled = enabledServices.size == 1 && isEnabled
                val canDisable =
                    if (loadLastAiEnabled) !isOnlyEnabled else !isDefault && !isOnlyEnabled

                val showReorderControls = searchQuery.isEmpty() && isReorderMode

                AiServiceCard(
                    service = service,
                    isEnabled = isEnabled,
                    canToggle = canDisable,
                    isDefault = isDefault,
                    loadLastAiEnabled = loadLastAiEnabled,
                    showReorderControls = showReorderControls,
                    isFirst = index == 0,
                    isLast = index == currentOrder.lastIndex,
                    onToggle = { enabled ->
                        val newSet = enabledServices.toMutableSet().apply {
                            if (enabled) add(service.id) else remove(service.id)
                        }
                        onEnabledServicesChange(newSet)
                    },
                    onMoveUp = {
                        if (index > 0) moveItem(
                            index,
                            index - 1,
                            currentOrder,
                            onCurrentOrderChange = { currentOrder = it },
                            settingsManager = settingsManager,
                            lazyListState = lazyListState,
                            coroutineScope = coroutineScope
                        )
                    },
                    onMoveDown = {
                        if (index < currentOrder.lastIndex) moveItem(
                            index,
                            index + 1,
                            currentOrder,
                            onCurrentOrderChange = { currentOrder = it },
                            settingsManager = settingsManager,
                            lazyListState = lazyListState,
                            coroutineScope = coroutineScope
                        )
                    },
                    modifier = Modifier.animateItem(
                        fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                )
            }
        }
    }
}

private fun moveItem(
    from: Int,
    to: Int,
    currentOrder: List<AiService>,
    onCurrentOrderChange: (List<AiService>) -> Unit,
    settingsManager: SettingsManager,
    lazyListState: LazyListState,
    coroutineScope: CoroutineScope
) {
    val newList = currentOrder.toMutableList().apply {
        val item = removeAt(from)
        add(to, item)
    }

    onCurrentOrderChange(newList)

    settingsManager.updateSettings { it ->
        it.serviceOrder = newList.map { it.id }
    }

    coroutineScope.launch {
        lazyListState.animateScrollToItem(to)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegularTopAppBar(
    title: String,
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    isReorderMode: Boolean,
    onToggleReorder: () -> Unit
) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.action_search)
                )
            }
            IconButton(onClick = onToggleReorder) {
                Icon(
                    if (isReorderMode) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                    contentDescription = if (isReorderMode) {
                        stringResource(R.string.msg_disable_record_mode)
                    } else {
                        stringResource(R.string.msg_enable_record_mode)
                    },
                    tint = if (isReorderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopAppBar(
    searchQuery: String, onSearchQueryChange: (String) -> Unit, onCloseSearch: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    TopAppBar(
        title = {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.hint_search_services)) },
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                Icons.Rounded.Clear,
                                contentDescription = stringResource(R.string.action_clear)
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseSearch) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_close)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun AiServiceCard(
    service: AiService,
    isEnabled: Boolean,
    canToggle: Boolean,
    isDefault: Boolean,
    loadLastAiEnabled: Boolean,
    showReorderControls: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isEnabled) {
        service.accentColor.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    Card(
        onClick = { if (canToggle || !isEnabled) onToggle(!isEnabled) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (!isEnabled) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = service.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isEnabled) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isDefault && !loadLastAiEnabled) {
                        DefaultBadge()
                    }

                    if (!canToggle && isEnabled && loadLastAiEnabled) {
                        LastEnabledBadge()
                    }
                }

                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { if (canToggle || !isEnabled) onToggle(it) },
                    enabled = canToggle || !isEnabled,
                    modifier = Modifier.scale(0.9f)
                )

                if (showReorderControls) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.KeyboardArrowUp,
                                contentDescription = stringResource(R.string.action_move_up),
                                tint = if (isFirst) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.action_move_down),
                                tint = if (isLast) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultBadge() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Text(
            text = stringResource(R.string.label_default_ai),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun LastEnabledBadge() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer,
        tonalElevation = 2.dp
    ) {
        Text(
            text = stringResource(R.string.label_last_enabled),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}