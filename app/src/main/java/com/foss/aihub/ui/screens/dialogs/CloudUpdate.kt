package com.foss.aihub.ui.screens.dialogs

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.foss.aihub.R
import com.foss.aihub.utils.DetailedUpdateDetails
import com.foss.aihub.utils.ServiceChanges
import com.foss.aihub.utils.UpdateResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
    showDialog: Boolean,
    updateResult: UpdateResult,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var isExiting by remember { mutableStateOf(false) }

    if (showDialog && !isExiting) {
        Dialog(
            onDismissRequest = { onDismiss() }, properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = true,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(tween(280)) + fadeIn(tween(250)),
                exit = scaleOut(tween(180)) + fadeOut(tween(180))
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = colorScheme.surfaceContainerHigh,
                    tonalElevation = 8.dp,
                    shadowElevation = 20.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .heightIn(max = 580.dp)
                        .clip(RoundedCornerShape(28.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        colorScheme.surfaceContainerHigh,
                                        colorScheme.surfaceContainer
                                    )
                                )
                            )
                    ) {
                        ShimmerBar()

                        UpdateHeader(
                            updateResult.message!!
                        )

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(max = 400.dp)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item { UpdateSummaryCard(updateResult.details) }
                            expandableCategories(updateResult.details)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        UpdateActionButtons(
                            onRestart = {
                                isExiting = true
                                onDismiss()
                                kotlinx.coroutines.MainScope().launch {
                                    delay(250)
                                    val intent =
                                        context.packageManager.getLaunchIntentForPackage(context.packageName)
                                            ?.let { Intent.makeRestartActivityTask(it.component) }
                                    intent?.let { context.startActivity(it) }
                                    exitProcess(0)
                                }
                            }, onLater = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShimmerBar() {
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing), repeatMode = RepeatMode.Restart
        )
    )
    val gradientStops = listOf(
        0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        0.3f to MaterialTheme.colorScheme.primary,
        0.7f to MaterialTheme.colorScheme.secondary,
        1f to MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(
                Brush.horizontalGradient(
                    colors = gradientStops.map { it.second },
                    startX = -shimmerProgress,
                    endX = 1f - shimmerProgress
                )
            )
    )
}

@Composable
private fun UpdateHeader(updateMessage: String) {
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f, animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse
        )
    )

    val boxSizePx = with(density) { 56.dp.toPx() }
    val gradientRadius = boxSizePx * 0.5f
    val gradientCenter = Offset(boxSizePx * 0.5f, boxSizePx * 0.5f)

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                colorScheme.primary.copy(alpha = 0.15f),
                                colorScheme.primary.copy(alpha = 0.05f)
                            ), radius = gradientRadius, center = gradientCenter
                        )
                    ), contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SystemUpdateAlt,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale })
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.label_update_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold, letterSpacing = 0.sp
                ),
                color = colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = updateMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun UpdateSummaryCard(details: DetailedUpdateDetails) {
    val totalChanges =
        details.serviceChanges.total() + details.serviceDomainChanges.total() + details.alwaysBlockedDomainChanges.total() + details.commonAuthDomainChanges.total() + details.trackingParamsChanges.total()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.label_update_summary),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.label_changes_summary, totalChanges),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun ServiceChanges.total(): Int = added.size + removed.size + changed.size

private fun LazyListScope.expandableCategories(details: DetailedUpdateDetails) {
    item {
        ExpandableCategoryCard(
            stringResource(R.string.label_ai_services),
            Icons.Rounded.SmartToy,
            details.serviceChanges
        )
    }
    item {
        ExpandableCategoryCard(
            stringResource(R.string.label_service_domains),
            Icons.Rounded.Public,
            details.serviceDomainChanges
        )
    }
    item {
        ExpandableCategoryCard(
            stringResource(R.string.label_always_blocked_domains),
            Icons.Rounded.Block,
            details.alwaysBlockedDomainChanges
        )
    }
    item {
        ExpandableCategoryCard(
            stringResource(R.string.label_common_auth_domains),
            Icons.Rounded.Lock,
            details.commonAuthDomainChanges
        )
    }
    item {
        ExpandableCategoryCard(
            stringResource(R.string.label_tracking_parameters),
            Icons.Rounded.TrackChanges,
            details.trackingParamsChanges
        )
    }
}

@Composable
private fun ExpandableCategoryCard(title: String, icon: ImageVector, changes: ServiceChanges) {
    val total = changes.total()
    if (total == 0) return

    var expanded by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "$total",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) stringResource(R.string.label_collapse) else stringResource(
                        R.string.label_expand
                    ),
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded, enter = expandVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200)), exit = shrinkVertically(
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (changes.added.isNotEmpty()) {
                        SectionHeader(
                            stringResource(R.string.label_added),
                            changes.added.size,
                            Icons.Rounded.AddCircle,
                            MaterialTheme.colorScheme.primary
                        )
                        changes.added.forEach { service ->
                            ServiceItem(service, false)
                        }
                    }
                    if (changes.removed.isNotEmpty()) {
                        SectionHeader(
                            stringResource(R.string.label_removed),
                            changes.removed.size,
                            Icons.Rounded.RemoveCircle,
                            MaterialTheme.colorScheme.error
                        )
                        changes.removed.forEach { service ->
                            ServiceItem(service, true)
                        }
                    }
                    if (changes.changed.isNotEmpty()) {
                        SectionHeader(
                            stringResource(R.string.label_changed),
                            changes.changed.size,
                            Icons.Rounded.Sync,
                            MaterialTheme.colorScheme.secondary
                        )
                        changes.changed.forEach { service ->
                            ServiceItem(service, false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, icon: ImageVector, iconTint: Color) {
    if (count == 0) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$title ($count)",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ServiceItem(service: String, isRemoved: Boolean = false) {
    val colorScheme = MaterialTheme.colorScheme

    val backgroundColor = if (isRemoved) {
        colorScheme.errorContainer.copy(alpha = 0.2f)
    } else {
        colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
    }

    val textColor = if (isRemoved) {
        colorScheme.error
    } else {
        colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isRemoved) colorScheme.errorContainer else colorScheme.primaryContainer,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isRemoved) Icons.Default.RemoveCircle else Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = if (isRemoved) colorScheme.error else colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = service,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun UpdateActionButtons(
    onRestart: () -> Unit, onLater: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onRestart,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Rounded.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    stringResource(R.string.action_restart),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
            OutlinedButton(
                onClick = onLater,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    stringResource(R.string.action_later),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}