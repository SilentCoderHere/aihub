package com.foss.aihub.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foss.aihub.R
import com.foss.aihub.models.AiService
import com.foss.aihub.models.loadServices
import com.foss.aihub.utils.CloudDataHandler
import com.foss.aihub.utils.SettingsManager
import com.foss.aihub.utils.capitalizeFirstLetter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    context: Context, settingsManager: SettingsManager, onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var downloadComplete by remember { mutableStateOf(false) }
    var aiServices by remember { mutableStateOf<List<AiService>?>(null) }

    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedPricing by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedPrivacy by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedLoginRequired by remember { mutableStateOf<Set<String>>(emptySet()) }

    var enableNewServices by remember { mutableStateOf(true) }

    var showPricingInfo by remember { mutableStateOf(false) }
    var showPrivacyInfo by remember { mutableStateOf(false) }
    var showLoginInfo by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage, downloadError, downloadComplete) {
        if (pagerState.currentPage == 2 && !isDownloading && !downloadComplete && downloadError == null) {
            isDownloading = true
            try {
                withContext(Dispatchers.IO) {
                    CloudDataHandler.updateAiServices(context)
                    CloudDataHandler.updateDomains(context)
                }
                val services = loadServices(context)
                aiServices = services
                downloadComplete = true
                if (services.isNotEmpty()) {
                    selectedCategories = services.map { it.category }.toSet()
                    selectedPricing = services.map { it.pricing }.toSet()
                    selectedPrivacy = services.map { it.privacy }.toSet()
                    selectedLoginRequired = emptySet()
                }
            } catch (e: Exception) {
                downloadError = e.message ?: "Unknown error"
            } finally {
                isDownloading = false
            }
        }
    }

    fun finishOnboarding() {
        val services = aiServices ?: emptyList()

        val filteredAiNames = services.filter { service ->
            val categoryOk = selectedCategories.contains(service.category)
            val pricingOk = selectedPricing.contains(service.pricing)
            val privacyOk = selectedPrivacy.contains(service.privacy)
            val loginOk = when {
                selectedLoginRequired.isEmpty() -> true
                selectedLoginRequired.contains("Required") -> service.loginRequired
                selectedLoginRequired.contains("Not Required") -> !service.loginRequired
                else -> true
            }
            categoryOk && pricingOk && privacyOk && loginOk
        }

        settingsManager.updateSettings { currentSettings ->
            currentSettings.preferredCategories = selectedCategories
            currentSettings.preferredPrices = selectedPricing
            currentSettings.preferredPrivacy = selectedPrivacy
            currentSettings.enableNewServicesByDefault = enableNewServices
            currentSettings.preferredLoginRequired = when {
                selectedLoginRequired.contains("Required") -> true
                selectedLoginRequired.contains("Not Required") -> false
                else -> null
            }

            currentSettings.enabledServices = filteredAiNames.map { it.name }.toSet()
            currentSettings.serviceOrder = services.map { it.name }
            currentSettings.defaultServiceName = filteredAiNames.firstOrNull()?.name ?: ""
        }

        settingsManager.saveDomainsLastUpdatedDate()
        settingsManager.saveLastUpdatedDate()

        onComplete()
    }

    fun canGoToNext(page: Int): Boolean {
        return when (page) {
            2 -> downloadComplete && downloadError == null
            3 -> selectedCategories.isNotEmpty() && selectedPricing.isNotEmpty() && selectedPrivacy.isNotEmpty()
            else -> true
        }
    }

    fun goToNext() {
        if (pagerState.currentPage < pagerState.pageCount - 1 && canGoToNext(pagerState.currentPage)) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, bottomBar = {
            if (pagerState.currentPage < 3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(pagerState.pageCount) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == pagerState.currentPage) 12.dp else 8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                    Button(
                        onClick = { goToNext() },
                        enabled = canGoToNext(pagerState.currentPage),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            if (pagerState.currentPage == 2) stringResource(R.string.onboarding_button_continue)
                            else stringResource(R.string.onboarding_button_next)
                        )
                    }
                }
            }
        }) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            userScrollEnabled = !(pagerState.currentPage == 2 && (isDownloading || downloadError != null))
        ) { page ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(400)) + slideInHorizontally(initialOffsetX = { it / 2 }),
                exit = fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { -it / 2 })
            ) {
                when (page) {
                    0 -> WelcomePage()
                    1 -> InfoPage()
                    2 -> DownloadPage(
                        isDownloading = isDownloading,
                        downloadError = downloadError,
                        downloadComplete = downloadComplete,
                        onRetry = {
                            downloadError = null
                            downloadComplete = false
                            aiServices = null
                        })

                    3 -> SelectionPage(
                        aiServices = aiServices ?: emptyList(),
                        selectedCategories = selectedCategories,
                        selectedPricing = selectedPricing,
                        selectedPrivacy = selectedPrivacy,
                        selectedLoginRequired = selectedLoginRequired,
                        enableNewServices = enableNewServices,
                        onEnableNewServicesChange = { enableNewServices = it },
                        onCategoryToggle = { category ->
                            if (!(selectedCategories.size == 1 && selectedCategories.contains(
                                    category
                                ))
                            ) {
                                selectedCategories = if (category in selectedCategories) {
                                    selectedCategories - category
                                } else {
                                    selectedCategories + category
                                }
                            }
                        },
                        onPricingToggle = { pricing ->
                            if (!(selectedPricing.size == 1 && selectedPricing.contains(pricing))) {
                                selectedPricing = if (pricing in selectedPricing) {
                                    selectedPricing - pricing
                                } else {
                                    selectedPricing + pricing
                                }
                            }
                        },
                        onPrivacyToggle = { privacy ->
                            if (!(selectedPrivacy.size == 1 && selectedPrivacy.contains(privacy))) {
                                selectedPrivacy = if (privacy in selectedPrivacy) {
                                    selectedPrivacy - privacy
                                } else {
                                    selectedPrivacy + privacy
                                }
                            }
                        },
                        onLoginToggle = { loginOption ->
                            selectedLoginRequired = if (loginOption in selectedLoginRequired) {
                                emptySet()
                            } else {
                                setOf(loginOption)
                            }
                        },
                        onPricingInfoClick = { showPricingInfo = true },
                        onPrivacyInfoClick = { showPrivacyInfo = true },
                        onLoginInfoClick = { showLoginInfo = true },
                        onFinish = { finishOnboarding() })
                }
            }
        }
    }

    if (showPricingInfo) {
        AlertDialog(
            onDismissRequest = { showPricingInfo = false },
            title = { Text(stringResource(R.string.onboarding_dialog_pricing_title)) },
            text = {
                Column {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(
                                    stringResource(R.string.onboarding_dialog_pricing_free)
                                )
                            }
                            append(stringResource(R.string.onboarding_dialog_pricing_free_desc))
                        }, fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(
                                    stringResource(R.string.onboarding_dialog_pricing_freemium)
                                )
                            }
                            append(stringResource(R.string.onboarding_dialog_pricing_freemium_desc))
                        }, fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(
                                    stringResource(R.string.onboarding_dialog_pricing_paid)
                                )
                            }
                            append(stringResource(R.string.onboarding_dialog_pricing_paid_desc))
                        }, fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPricingInfo = false }) {
                    Text(stringResource(R.string.onboarding_dialog_got_it))
                }
            })
    }

    if (showPrivacyInfo) {
        AlertDialog(
            onDismissRequest = { showPrivacyInfo = false },
            title = { Text(stringResource(R.string.onboarding_dialog_privacy_title)) },
            text = {
                Column {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(
                                    stringResource(R.string.onboarding_dialog_privacy_avoid)
                                )
                            }
                            append(stringResource(R.string.onboarding_dialog_privacy_avoid_desc))
                        }, fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(
                                    stringResource(R.string.onboarding_dialog_privacy_neutral)
                                )
                            }
                            append(stringResource(R.string.onboarding_dialog_privacy_neutral_desc))
                        }, fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(
                                    stringResource(R.string.onboarding_dialog_privacy_friendly)
                                )
                            }
                            append(stringResource(R.string.onboarding_dialog_privacy_friendly_desc))
                        }, fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyInfo = false }) {
                    Text(stringResource(R.string.onboarding_dialog_got_it))
                }
            })
    }

    if (showLoginInfo) {
        AlertDialog(
            onDismissRequest = { showLoginInfo = false },
            title = { Text(stringResource(R.string.onboarding_dialog_login_title)) },
            text = {
                Column {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(
                                    stringResource(R.string.onboarding_dialog_login_required)
                                )
                            }
                            append(stringResource(R.string.onboarding_dialog_login_required_desc))
                        }, fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(
                                    stringResource(R.string.onboarding_dialog_login_not_required)
                                )
                            }
                            append(stringResource(R.string.onboarding_dialog_login_not_required_desc))
                        }, fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(
                                    stringResource(R.string.onboarding_dialog_login_note)
                                )
                            }
                            append(stringResource(R.string.onboarding_dialog_login_note_desc))
                        }, fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLoginInfo = false }) {
                    Text(stringResource(R.string.onboarding_dialog_got_it))
                }
            })
    }
}


@Composable
fun WelcomePage() {
    Surface(
        modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
                    .clip(RoundedCornerShape(100))
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.onboarding_welcome_subtitle),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ), shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.onboarding_welcome_bullet1), fontSize = 14.sp)
                    Text(stringResource(R.string.onboarding_welcome_bullet2), fontSize = 14.sp)
                    Text(stringResource(R.string.onboarding_welcome_bullet3), fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.onboarding_welcome_swipe_hint),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun InfoPage() {
    Surface(
        modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Downloading,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.onboarding_info_title),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.onboarding_info_description),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ), shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.onboarding_info_bullet1), fontSize = 14.sp)
                    Text(stringResource(R.string.onboarding_info_bullet2), fontSize = 14.sp)
                    Text(stringResource(R.string.onboarding_info_bullet3), fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.onboarding_info_connection_note),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun DownloadPage(
    isDownloading: Boolean, downloadError: String?, downloadComplete: Boolean, onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.onboarding_download_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    isDownloading -> stringResource(R.string.onboarding_download_loading)
                    downloadComplete -> stringResource(R.string.onboarding_download_complete)
                    else -> stringResource(R.string.onboarding_download_error)
                }, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            when {
                isDownloading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(80.dp),
                        strokeWidth = 6.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.onboarding_download_downloading),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.onboarding_download_fetching),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                downloadError != null -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.onboarding_download_failed, downloadError),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.width(200.dp)
                    ) {
                        Text(stringResource(R.string.onboarding_download_retry))
                    }
                }

                downloadComplete -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.onboarding_download_complete_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_download_complete_sub),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionPage(
    aiServices: List<AiService>,
    selectedCategories: Set<String>,
    selectedPricing: Set<String>,
    selectedPrivacy: Set<String>,
    selectedLoginRequired: Set<String>,
    enableNewServices: Boolean,
    onEnableNewServicesChange: (Boolean) -> Unit,
    onCategoryToggle: (String) -> Unit,
    onPricingToggle: (String) -> Unit,
    onPrivacyToggle: (String) -> Unit,
    onLoginToggle: (String) -> Unit,
    onPricingInfoClick: () -> Unit,
    onPrivacyInfoClick: () -> Unit,
    onLoginInfoClick: () -> Unit,
    onFinish: () -> Unit
) {
    val categories = aiServices.map { it.category }.distinct()
    val pricings = aiServices.map { it.pricing }.distinct()
    val privacies = aiServices.map { it.privacy }.distinct()

    val isAnySelected =
        selectedCategories.isNotEmpty() && selectedPricing.isNotEmpty() && selectedPrivacy.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_selection_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.onboarding_selection_subtitle),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.onboarding_auto_enable_label),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.onboarding_auto_enable_desc),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enableNewServices,
                onCheckedChange = { onEnableNewServicesChange(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (enableNewServices) {
            LazyColumn(
                modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    FilterSection(
                        title = stringResource(R.string.onboarding_filter_category),
                        items = categories,
                        selectedItems = selectedCategories,
                        onToggle = onCategoryToggle,
                        showInfo = false,
                        onInfoClick = {})
                }
                item {
                    FilterSection(
                        title = stringResource(R.string.onboarding_filter_pricing),
                        items = pricings,
                        selectedItems = selectedPricing,
                        onToggle = onPricingToggle,
                        showInfo = true,
                        onInfoClick = onPricingInfoClick
                    )
                }
                item {
                    FilterSection(
                        title = stringResource(R.string.onboarding_filter_privacy),
                        items = privacies,
                        selectedItems = selectedPrivacy,
                        onToggle = onPrivacyToggle,
                        showInfo = true,
                        onInfoClick = onPrivacyInfoClick
                    )
                }
                item {
                    LoginFilterSection(
                        selectedLoginRequired = selectedLoginRequired,
                        onLoginToggle = onLoginToggle,
                        onInfoClick = onLoginInfoClick
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ), shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.info),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_filters_hidden_title),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.onboarding_filters_hidden_desc),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.onboarding_preferences_note),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(50),
            enabled = isAnySelected,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAnySelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        ) {
            Text(
                text = stringResource(R.string.onboarding_finish_button),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isAnySelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    }
}

@Composable
fun FilterSection(
    title: String,
    items: List<String>,
    selectedItems: Set<String>,
    onToggle: (String) -> Unit,
    showInfo: Boolean,
    onInfoClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp
            )
            if (showInfo) {
                IconButton(
                    onClick = onInfoClick, modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.info),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                val isSelected = item in selectedItems
                val displayName = item.capitalizeFirstLetter()
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(item) },
                    label = { Text(displayName, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = null,
                    shape = RoundedCornerShape(50)
                )
            }
        }
        if (selectedItems.size == 1) {
            Text(
                text = stringResource(R.string.onboarding_filter_keep_one),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun LoginFilterSection(
    selectedLoginRequired: Set<String>, onLoginToggle: (String) -> Unit, onInfoClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.onboarding_filter_login),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp
            )
            IconButton(
                onClick = onInfoClick, modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.info),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                stringResource(R.string.onboarding_login_required),
                stringResource(R.string.onboarding_login_not_required)
            ).forEach { option ->
                val isSelected = option in selectedLoginRequired
                FilterChip(
                    selected = isSelected,
                    onClick = { onLoginToggle(option) },
                    label = { Text(option, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = null,
                    shape = RoundedCornerShape(50)
                )
            }
        }
        if (selectedLoginRequired.isEmpty()) {
            Text(
                text = stringResource(R.string.onboarding_filter_login_none),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}