package com.foss.aihub.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foss.aihub.R
import com.foss.aihub.models.AiService
import com.foss.aihub.models.WebViewState
import com.foss.aihub.utils.ConfigUpdater
import com.foss.aihub.utils.aiServices
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerContent(
    modifier: Modifier = Modifier,
    selectedService: AiService,
    onServiceSelected: (AiService) -> Unit,
    onServiceReload: (AiService) -> Unit,
    webViewStates: Map<String, WebViewState>,
    enabledServices: Set<String>,
    serviceOrder: List<String>,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var isUpdatingDomainRules by remember { mutableStateOf(false) }

    val orderedEnabledServices = remember(enabledServices, serviceOrder, aiServices.toList()) {
        serviceOrder.filter { it in enabledServices }
            .mapNotNull { id -> aiServices.find { it.id == id } }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth(0.86f)
            .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)),
        color = colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
        border = BorderStroke(0.5.dp, colorScheme.outlineVariant.copy(alpha = 0.18f))
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    colorScheme.primary.copy(alpha = 0.07f), Color.Transparent
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = WindowInsets.statusBars.asPaddingValues()
                                    .calculateTopPadding() + 24.dp,
                                start = 24.dp,
                                end = 24.dp,
                                bottom = 20.dp
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
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_launcher_foreground),
                                        contentDescription = "AI Hub Logo",
                                        tint = colorScheme.onPrimary,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "AI Hub",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp
                                    ),
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Your AI Companion Collection",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "AI Assistants",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(orderedEnabledServices) { service ->
                        val state = webViewStates[service.id] ?: WebViewState.IDLE
                        Md3ServiceCard(
                            service = service,
                            serviceColor = service.accentColor,
                            isSelected = selectedService.id == service.id,
                            state = state,
                            onClick = {
                                if (selectedService.id == service.id) {
                                    onServiceReload(service)
                                } else {
                                    onServiceSelected(service)
                                }
                            })
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${orderedEnabledServices.size}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Text(
                            text = "Update AI & services",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                isUpdatingDomainRules = true
                                try {
                                    val (domainUpdated, aiUpdated) = ConfigUpdater.updateBothIfNeeded(
                                        context
                                    )

                                    val message: String
                                    val showRestartAction: Boolean

                                    when {
                                        domainUpdated && aiUpdated -> {
                                            message = "Domain rules & AI services updated"
                                            showRestartAction = true
                                        }

                                        domainUpdated -> {
                                            message = "Domain rules updated"
                                            showRestartAction = true
                                        }

                                        aiUpdated -> {
                                            message = "AI services updated"
                                            showRestartAction = true
                                        }

                                        else -> {
                                            message = "Already up to date"
                                            showRestartAction = false
                                        }
                                    }

                                    launch {
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            actionLabel = if (showRestartAction) "Restart" else null,
                                            withDismissAction = true,
                                            duration = SnackbarDuration.Long
                                        ).let { result ->
                                            if (result == SnackbarResult.ActionPerformed && showRestartAction) {
                                                val packageManager = context.packageManager
                                                val intent =
                                                    packageManager.getLaunchIntentForPackage(context.packageName)
                                                val componentName = intent?.component
                                                val mainIntent =
                                                    Intent.makeRestartActivityTask(componentName)
                                                context.startActivity(mainIntent)
                                                exitProcess(0)
                                            }
                                        }
                                    }

                                } catch (_: Exception) {
                                    launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Update failed – check connection",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                } finally {
                                    isUpdatingDomainRules = false
                                }
                            }
                        },
                        enabled = !isUpdatingDomainRules,
                        modifier = Modifier
                            .height(40.dp)
                            .widthIn(min = 100.dp, max = 140.dp),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        if (isUpdatingDomainRules) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.6.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Updating…", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Text(
                                "Update",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}