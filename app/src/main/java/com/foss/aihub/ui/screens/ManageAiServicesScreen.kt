package com.foss.aihub.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foss.aihub.R
import com.foss.aihub.models.AiService
import com.foss.aihub.ui.screens.dialogs.FilterDialog
import com.foss.aihub.utils.SettingsManager
import com.foss.aihub.utils.capitalizeFirstLetter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAiServicesScreen(
    onBack: () -> Unit,
    aiServices: List<AiService>,
    enabledServices: Set<String>,
    defaultServiceId: String,
    loadLastAiEnabled: Boolean,
    onEnabledServicesChange: (Set<String>) -> Unit,
    settingsManager: SettingsManager,
    onRequestNewAi: () -> Unit
) {
    val settings by settingsManager.settingsFlow.collectAsState()
    val baseServices = remember(aiServices) { aiServices }

    val orderedServices = remember(settings.serviceOrder, baseServices) {
        if (settings.serviceOrder.isEmpty()) {
            baseServices
        } else {
            settings.serviceOrder.mapNotNull { name -> baseServices.find { it.name == name } }
        }
    }

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isReorderMode by remember { mutableStateOf(false) }

    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var selectedPrices by remember { mutableStateOf(setOf<String>()) }
    var selectedPrivacy by remember { mutableStateOf(emptySet<String>()) }
    var selectedLoginRequired: Boolean? by remember { mutableStateOf(null) }

    var selectedForReorder by remember { mutableStateOf<String?>(null) }
    var expandedServiceName by remember { mutableStateOf<String?>(null) }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var currentOrder by remember(orderedServices) { mutableStateOf(orderedServices) }

    LaunchedEffect(settings.serviceOrder, baseServices) {
        currentOrder =
            settings.serviceOrder.mapNotNull { name -> baseServices.find { it.name == name } }
    }

    val activeFilterCount = remember {
        derivedStateOf {
            var count = 0
            if (searchQuery.isNotBlank()) count++
            if (selectedCategories.isNotEmpty()) count++
            if (selectedPrices.isNotEmpty()) count++
            if (selectedPrivacy.isNotEmpty()) count++
            if (selectedLoginRequired != null) count++
            count
        }
    }

    val filteredServices = remember(
        currentOrder,
        searchQuery,
        selectedCategories,
        selectedPrices,
        selectedPrivacy,
        selectedLoginRequired
    ) {
        currentOrder.filter { service ->
            val matchesSearch =
                searchQuery.isEmpty() || service.name.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategories.isEmpty() || selectedCategories.any {
                it.equals(service.category, ignoreCase = true)
            }
            val matchesPrice = selectedPrices.isEmpty() || selectedPrices.any {
                it.equals(service.pricing, ignoreCase = true)
            }
            val matchesPrivacy = selectedPrivacy.isEmpty() || selectedPrivacy.any {
                it.equals(service.privacy, ignoreCase = true)
            }
            val matchesLoginRequired = when (selectedLoginRequired) {
                null -> true
                else -> service.loginRequired == selectedLoginRequired
            }
            matchesSearch && matchesCategory && matchesPrice && matchesPrivacy && matchesLoginRequired
        }
    }

    val selectedIndex = selectedForReorder?.let { name ->
        filteredServices.indexOfFirst { it.name == name }
    } ?: -1
    val canMoveUp = selectedIndex > 0
    val canMoveDown = selectedIndex >= 0 && selectedIndex < filteredServices.lastIndex

    fun clearFilters() {
        searchQuery = ""
        selectedCategories = emptySet()
        selectedPrices = emptySet()
        selectedPrivacy = emptySet()
        selectedLoginRequired = null
    }

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        if (isSearching && !isReorderMode) {
            SearchTopAppBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onCloseSearch = { isSearching = false; searchQuery = "" })
        } else {
            RegularTopAppBar(
                title = stringResource(R.string.setting_manage_ai_services),
                onBack = onBack,
                onSearchClick = {
                    if (!isReorderMode) {
                        isSearching = true
                    }
                },
                isReorderMode = isReorderMode,
                onToggleReorder = {
                    isReorderMode = !isReorderMode
                    if (!isReorderMode) {
                        selectedForReorder = null
                    }
                },
                showSearch = !isReorderMode,
                showFilter = !isReorderMode,
                onFilterClick = { showFilterDialog = true },
                activeFilterCount = activeFilterCount.value
            )
        }
    }, floatingActionButton = {
        if (!isReorderMode) {
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
        }
    }, containerColor = MaterialTheme.colorScheme.background, bottomBar = {
        if (isReorderMode) {
            ReorderBottomContainer(
                selectedServiceName = selectedForReorder,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onMoveUp = {
                    if (canMoveUp && selectedForReorder != null) {
                        val from = selectedIndex
                        val to = from - 1
                        moveItem(
                            from = from,
                            to = to,
                            currentOrder = filteredServices,
                            onCurrentOrderChange = {
                                val newFullOrder = currentOrder.toMutableList().apply {
                                    val item = removeAt(from)
                                    add(to, item)
                                }
                                currentOrder = newFullOrder
                                settingsManager.updateSettings {
                                    it.serviceOrder = newFullOrder.map { service -> service.name }
                                }
                            },
                            settingsManager = settingsManager,
                            lazyListState = lazyListState,
                            coroutineScope = coroutineScope
                        )
                    }
                },
                onMoveDown = {
                    if (canMoveDown && selectedForReorder != null) {
                        val from = selectedIndex
                        val to = from + 1
                        moveItem(
                            from = from,
                            to = to,
                            currentOrder = filteredServices,
                            onCurrentOrderChange = {
                                val newFullOrder = currentOrder.toMutableList().apply {
                                    val item = removeAt(from)
                                    add(to, item)
                                }
                                currentOrder = newFullOrder
                                settingsManager.updateSettings {
                                    it.serviceOrder = newFullOrder.map { service -> service.name }
                                }
                            },
                            settingsManager = settingsManager,
                            lazyListState = lazyListState,
                            coroutineScope = coroutineScope
                        )
                    }
                },
                onDismissSelection = { selectedForReorder = null })
        }
    }) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val displayList = if (isReorderMode) currentOrder else filteredServices
            if (displayList.isEmpty() && !isReorderMode) {
                item(key = "no_results") {
                    NoResultsView(onReset = { clearFilters() })
                }
            } else {
                itemsIndexed(
                    items = displayList, key = { _, service -> service.name }) { _, service ->
                    val isEnabled = service.name in enabledServices
                    val isDefault = service.name == defaultServiceId
                    val isOnlyEnabled = enabledServices.size == 1 && isEnabled
                    val canDisable =
                        if (loadLastAiEnabled) !isOnlyEnabled else !isDefault && !isOnlyEnabled
                    val isExpanded = expandedServiceName == service.name
                    val isSelectedForReorder = selectedForReorder == service.name

                    MinimalServiceCard(
                        service = service,
                        isEnabled = isEnabled,
                        canToggle = canDisable,
                        isDefault = isDefault,
                        loadLastAiEnabled = loadLastAiEnabled,
                        isExpanded = isExpanded,
                        isSelectedForReorder = isSelectedForReorder,
                        isReorderMode = isReorderMode,
                        onToggleEnabled = { enabled ->
                            val newSet = enabledServices.toMutableSet().apply {
                                if (enabled) add(service.name) else remove(service.name)
                            }
                            onEnabledServicesChange(newSet)
                        },
                        onCardClick = {
                            if (isReorderMode) {
                                selectedForReorder =
                                    if (isSelectedForReorder) null else service.name
                            } else {
                                expandedServiceName = if (isExpanded) null else service.name
                            }
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

    if (showFilterDialog && !isReorderMode) {
        val availableCategories = currentOrder.map { it.category }.distinct().sorted()
        val availablePrices = currentOrder.map { it.pricing }.distinct().sorted()
        val availablePrivacy = currentOrder.map { it.privacy }.distinct().sorted()

        FilterDialog(
            categories = availableCategories,
            prices = availablePrices,
            privacyOptions = availablePrivacy,
            selectedCategories = selectedCategories,
            selectedPrices = selectedPrices,
            selectedPrivacy = selectedPrivacy,
            selectedLoginRequired = selectedLoginRequired,
            onCategoriesChange = { selectedCategories = it },
            onPricesChange = { selectedPrices = it },
            onPrivacyChange = { selectedPrivacy = it },
            onLoginRequiredChange = { selectedLoginRequired = it },
            onDismiss = { showFilterDialog = false },
            onClear = {
                selectedCategories = emptySet()
                selectedPrices = emptySet()
                selectedPrivacy = emptySet()
                selectedLoginRequired = null
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegularTopAppBar(
    title: String,
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    isReorderMode: Boolean,
    onToggleReorder: () -> Unit,
    showSearch: Boolean,
    showFilter: Boolean,
    onFilterClick: () -> Unit,
    activeFilterCount: Int = 0
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
            if (showSearch) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.action_search)
                    )
                }
            }
            if (showFilter) {
                Box {
                    IconButton(onClick = onFilterClick) {
                        Icon(
                            Icons.Rounded.FilterList,
                            contentDescription = stringResource(R.string.action_filter)
                        )
                    }
                    if (activeFilterCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeFilterCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onToggleReorder) {
                Icon(
                    if (isReorderMode) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                    contentDescription = if (isReorderMode) {
                        stringResource(R.string.msg_disable_record_mode)
                    } else {
                        stringResource(R.string.msg_enable_record_mode)
                    },
                    tint = if (isReorderMode) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
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
    }, navigationIcon = {
        IconButton(onClick = onCloseSearch) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.action_close)
            )
        }
    }, colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun NoResultsView(onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.msg_no_services_found),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilledTonalButton(
            onClick = onReset,
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Clear,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.action_clear_filters),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun MinimalServiceCard(
    service: AiService,
    isEnabled: Boolean,
    canToggle: Boolean,
    isDefault: Boolean,
    loadLastAiEnabled: Boolean,
    isExpanded: Boolean,
    isSelectedForReorder: Boolean,
    isReorderMode: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isEnabled) {
        service.accentColor.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (isSelectedForReorder) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else if (!isEnabled) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        } else {
            BorderStroke(1.dp, service.accentColor.copy(alpha = 0.3f))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = service.name.capitalizeFirstLetter(),
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
                    }
                    Text(
                        text = service.category.capitalizeFirstLetter(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { if (canToggle || !isEnabled) onToggleEnabled(it) },
                    enabled = canToggle || !isEnabled,
                    modifier = Modifier.scale(0.9f)
                )
            }

            AnimatedVisibility(
                visible = isExpanded && !isReorderMode, enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ), exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow(
                        label = stringResource(R.string.label_pricing).capitalizeFirstLetter(),
                        value = service.pricing.capitalizeFirstLetter(),
                        icon = Icons.Rounded.Add
                    )
                    DetailRow(
                        label = stringResource(R.string.label_privacy).capitalizeFirstLetter(),
                        value = service.privacy.capitalizeFirstLetter(),
                        icon = Icons.Rounded.Shield
                    )
                    DetailRow(
                        label = stringResource(R.string.label_login_required).capitalizeFirstLetter(),
                        value = if (service.loginRequired) {
                            stringResource(R.string.label_yes).capitalizeFirstLetter()
                        } else {
                            stringResource(R.string.label_no).capitalizeFirstLetter()
                        },
                        icon = if (service.loginRequired) Icons.Rounded.Lock else Icons.Rounded.LockOpen
                    )
                    if (service.bestFor.isNotEmpty()) {
                        DetailRow(
                            label = stringResource(R.string.label_best_for).capitalizeFirstLetter(),
                            value = service.bestFor.joinToString(", ") { it.capitalizeFirstLetter() },
                            icon = Icons.Rounded.Star
                        )
                    }
                    DetailRow(
                        label = stringResource(R.string.label_website).capitalizeFirstLetter(),
                        value = service.url,
                        isUrl = true,
                        icon = Icons.Rounded.Star
                    )
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
private fun ReorderBottomContainer(
    selectedServiceName: String?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDismissSelection: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (selectedServiceName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = selectedServiceName.capitalizeFirstLetter(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onDismissSelection() })
                    IconButton(
                        onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.action_move_up),
                            tint = if (canMoveUp) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.action_move_down),
                            tint = if (canMoveDown) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.msg_select_service_to_move),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String, value: String, isUrl: Boolean = false, icon: ImageVector? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(0.35f)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isUrl) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.65f),
            maxLines = if (isUrl) 2 else 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun moveItem(
    from: Int,
    to: Int,
    currentOrder: List<AiService>,
    onCurrentOrderChange: () -> Unit,
    settingsManager: SettingsManager,
    lazyListState: LazyListState,
    coroutineScope: CoroutineScope
) {
    val newList = currentOrder.toMutableList().apply {
        val item = removeAt(from)
        add(to, item)
    }

    onCurrentOrderChange()

    settingsManager.updateSettings { it ->
        it.serviceOrder = newList.map { it.name }
    }

    coroutineScope.launch {
        val layoutInfo = lazyListState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        val targetIndex = to
        val isVisible = visibleItems.any { it.index == targetIndex }
        if (!isVisible) {
            lazyListState.animateScrollToItem(targetIndex)
        }
    }
}