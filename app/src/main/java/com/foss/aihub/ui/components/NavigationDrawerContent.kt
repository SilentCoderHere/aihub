package com.foss.aihub.ui.components

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foss.aihub.R
import com.foss.aihub.models.AiService
import com.foss.aihub.models.UpdateResult
import com.foss.aihub.models.WebViewState
import com.foss.aihub.models.loadServices
import com.foss.aihub.ui.screens.dialogs.FilterDialog
import com.foss.aihub.ui.screens.dialogs.UpdateResultDialog
import com.foss.aihub.utils.SettingsManager
import com.foss.aihub.utils.getUpdateErrorMessage
import com.foss.aihub.utils.performServiceUpdate
import kotlinx.coroutines.launch

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerContent(
    aiServices: List<AiService>,
    onServicesUpdated: (List<AiService>) -> Unit,
    modifier: Modifier = Modifier,
    selectedService: AiService,
    onServiceSelected: (AiService) -> Unit,
    onServiceReload: (AiService) -> Unit,
    webViewStates: Map<String, WebViewState>,
    enabledServices: Set<String>,
    serviceOrder: List<String>,
    favoriteServices: Set<String>,
    onToggleFavorite: (String) -> Unit,
    settingsManager: SettingsManager,
) {
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val appContext = LocalContext.current
    var isUpdatingData by remember { mutableStateOf(false) }

    val settings by settingsManager.settingsFlow.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf(settings.filterCategories) }
    var selectedPrices by remember { mutableStateOf(settings.filterPrices) }
    var selectedPrivacy by remember { mutableStateOf(settings.filterPrivacy) }
    var selectedLoginRequired by remember { mutableStateOf(settings.filterLoginRequired) }

    var showFilterDialog by remember { mutableStateOf(false) }
    var showSearchField by remember { mutableStateOf(false) }

    var showUpdateResultDialog by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateResult?>(null) }

    LaunchedEffect(settings) {
        selectedCategories = settings.filterCategories
        selectedPrices = settings.filterPrices
        selectedPrivacy = settings.filterPrivacy
        selectedLoginRequired = settings.filterLoginRequired
    }

    LaunchedEffect(selectedCategories) {
        settingsManager.updateSettings { it.filterCategories = selectedCategories }
    }
    LaunchedEffect(selectedPrices) {
        settingsManager.updateSettings { it.filterPrices = selectedPrices }
    }
    LaunchedEffect(selectedPrivacy) {
        settingsManager.updateSettings { it.filterPrivacy = selectedPrivacy }
    }
    LaunchedEffect(selectedLoginRequired) {
        settingsManager.updateSettings { it.filterLoginRequired = selectedLoginRequired }
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

    val orderedEnabledServices = remember(aiServices, enabledServices, serviceOrder) {
        val enabledList = aiServices.filter { it.name in enabledServices }
        val orderMap = serviceOrder.withIndex().associate { it.value to it.index }
        enabledList.sortedBy { service ->
            orderMap[service.name] ?: Int.MAX_VALUE
        }
    }

    val availableCategories by derivedStateOf {
        orderedEnabledServices.map { it.category }.distinct().sorted()
    }
    val availablePrices by derivedStateOf {
        orderedEnabledServices.map { it.pricing }.distinct().sorted()
    }
    val availablePrivacy by derivedStateOf {
        orderedEnabledServices.map { it.privacy }.distinct().sorted()
    }

    val categoryOptions = remember(availableCategories) {
        listOf("Favorites") + availableCategories
    }

    val filteredServices by derivedStateOf {
        orderedEnabledServices.filter { service ->
            val matchesSearch =
                searchQuery.isBlank() || service.name.contains(searchQuery, ignoreCase = true)

            val matchesCategory = if (selectedCategories.isEmpty()) {
                true
            } else {
                val favSelected = "Favorites" in selectedCategories
                val catSelected = selectedCategories.any { it != "Favorites" }
                val matchesFav = if (favSelected) service.name in favoriteServices else true
                val matchesCat = if (catSelected) selectedCategories.any {
                    it.equals(service.category, ignoreCase = true)
                } else true
                matchesFav && matchesCat
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

    val showFavoritesOnly = "Favorites" in selectedCategories
    val favoriteFiltered by derivedStateOf {
        if (showFavoritesOnly) filteredServices else filteredServices.filter { it.name in favoriteServices }
    }
    val nonFavoriteFiltered by derivedStateOf {
        if (showFavoritesOnly) emptyList() else filteredServices.filter { it.name !in favoriteServices }
    }

    fun clearFilters() {
        searchQuery = ""
        settingsManager.updateSettings {
            it.filterCategories = emptySet()
            it.filterPrices = emptySet()
            it.filterPrivacy = emptySet()
            it.filterLoginRequired = null
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth(0.86f)
            .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)),
        color = colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
        border = BorderStroke(0.5.dp, colorScheme.outlineVariant.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colorScheme.primary.copy(alpha = 0.08f), Color.Transparent
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = WindowInsets.statusBars.asPaddingValues()
                                .calculateTopPadding() + 20.dp,
                            start = 24.dp,
                            end = 24.dp,
                            bottom = 8.dp
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primary,
                            tonalElevation = 4.dp,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_launcher_foreground),
                                    contentDescription = stringResource(R.string.app_logo_description),
                                    tint = colorScheme.onPrimary,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp
                            ),
                            color = colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { showSearchField = !showSearchField },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = stringResource(R.string.action_search),
                                tint = colorScheme.onSurfaceVariant
                            )
                        }

                        BadgedBox(
                            badge = {
                                if (activeFilterCount.value > 0) {
                                    Badge(
                                        count = activeFilterCount.value,
                                        color = colorScheme.primary,
                                        onColor = colorScheme.onPrimary,
                                        modifier = Modifier.offset(x = 4.dp, y = (-4).dp)
                                    )
                                }
                            }) {
                            IconButton(
                                onClick = { showFilterDialog = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FilterList,
                                    contentDescription = stringResource(R.string.action_filter),
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (showSearchField) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(stringResource(R.string.hint_search_services))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" }, modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.action_clear),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outlineVariant,
                        focusedContainerColor = colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = colorScheme.surfaceContainerLow
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                if (filteredServices.isEmpty()) {
                    item(key = "no_results") {
                        NoResultsView(onReset = { clearFilters() })
                    }
                } else if (showFavoritesOnly) {
                    items(favoriteFiltered, key = { it.name }) { service ->
                        ServiceCard(
                            service = service,
                            isSelected = selectedService.name == service.name,
                            state = webViewStates[service.name] ?: WebViewState.IDLE,
                            isFavorite = true,
                            onFavoriteToggle = { onToggleFavorite(service.name) },
                            onClick = {
                                if (selectedService.name == service.name) {
                                    onServiceReload(service)
                                } else {
                                    onServiceSelected(service)
                                }
                            },
                        )
                    }
                } else {
                    if (favoriteFiltered.isNotEmpty()) {
                        item(key = "favorites_header") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.tab_favorites),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = colorScheme.onSurface
                                )
                            }
                        }
                        items(favoriteFiltered, key = { "fav_${it.name}" }) { service ->
                            ServiceCard(
                                service = service,
                                isSelected = selectedService.name == service.name,
                                state = webViewStates[service.name] ?: WebViewState.IDLE,
                                isFavorite = true,
                                onFavoriteToggle = { onToggleFavorite(service.name) },
                                onClick = {
                                    if (selectedService.name == service.name) {
                                        onServiceReload(service)
                                    } else {
                                        onServiceSelected(service)
                                    }
                                })
                        }
                    }

                    if (nonFavoriteFiltered.isNotEmpty()) {
                        if (favoriteFiltered.isNotEmpty()) {
                            item(key = "all_header") {
                                Text(
                                    text = stringResource(R.string.tab_all_services),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = colorScheme.onSurface,
                                    modifier = Modifier.padding(
                                        start = 4.dp, top = 12.dp, bottom = 4.dp
                                    )
                                )
                            }
                        }
                        items(nonFavoriteFiltered, key = { it.name }) { service ->
                            ServiceCard(
                                service = service,
                                isSelected = selectedService.name == service.name,
                                state = webViewStates[service.name] ?: WebViewState.IDLE,
                                isFavorite = false,
                                onFavoriteToggle = { onToggleFavorite(service.name) },
                                onClick = {
                                    if (selectedService.name == service.name) {
                                        onServiceReload(service)
                                    } else {
                                        onServiceSelected(service)
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${orderedEnabledServices.size}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                                ),
                                color = colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.action_update_services),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            isUpdatingData = true
                            try {
                                val result = performServiceUpdate(appContext, settingsManager)
                                if (result != null) {
                                    updateResult = result
                                    showUpdateResultDialog = true
                                    onServicesUpdated(loadServices(appContext))
                                } else {
                                    Toast.makeText(
                                        appContext, R.string.update_no_changes, Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    appContext,
                                    getUpdateErrorMessage(appContext, e),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                isUpdatingData = false
                            }
                        }
                    },
                    enabled = !isUpdatingData,
                    modifier = Modifier
                        .height(40.dp)
                        .widthIn(min = 100.dp, max = 140.dp),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    if (isUpdatingData) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.6.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.status_updating),
                            style = MaterialTheme.typography.labelMedium
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.action_update),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            categories = categoryOptions,
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
            },
        )
    }

    if (showUpdateResultDialog && updateResult != null) {
        UpdateResultDialog(
            added = updateResult!!.added,
            removed = updateResult!!.removed,
            modified = updateResult!!.modified,
            newCategories = updateResult!!.newCategories,
            onDismiss = {
                showUpdateResultDialog = false
                updateResult = null
            },
        )
    }
}

@Composable
private fun Badge(
    count: Int, color: Color, onColor: Color, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(), style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp, fontWeight = FontWeight.Bold, color = onColor
            )
        )
    }
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
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.action_reset_filters),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ServiceCard(
    service: AiService,
    isSelected: Boolean,
    state: WebViewState,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit
) {
    val accentColor = service.accentColor
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp, pressedElevation = 4.dp
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isSelected) accentColor else colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(accentColor, accentColor.copy(alpha = 0.5f))
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when (state) {
                            WebViewState.LOADING -> Color(0xFFFFA726)
                            WebViewState.ERROR -> Color(0xFFEF5350)
                            WebViewState.SUCCESS -> Color(0xFF66BB6A)
                            WebViewState.IDLE -> Color.Gray
                        }
                    )
            )

            Text(
                text = service.name, style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp
                ), color = colorScheme.onSurface, modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFavorite) accentColor.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
            ) {
                IconButton(
                    onClick = onFavoriteToggle, modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = if (isFavorite) stringResource(R.string.action_unfavorite)
                        else stringResource(R.string.action_favorite),
                        tint = if (isFavorite) Color(0xFFFFB300) else colorScheme.onSurfaceVariant.copy(
                            alpha = 0.6f
                        ),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}