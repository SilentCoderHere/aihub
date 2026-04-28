package com.foss.aihub.ui.screens.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.foss.aihub.R
import com.foss.aihub.models.LinkData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkOptionsDialog(
    linkData: LinkData,
    onDismiss: () -> Unit,
    onOpenLinkInExternalBrowser: (String) -> Unit,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit
) {
    var isContentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isContentVisible = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 12.dp,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UrlDisplayContainer(
                    url = linkData.url, visible = isContentVisible
                )

                ActionButtonRow(
                    visible = isContentVisible,
                    delay = 100,
                    onClick = { onOpenLinkInExternalBrowser(linkData.url) },
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    text = stringResource(R.string.action_open_in_browser),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )

                ActionButtonRow(
                    visible = isContentVisible,
                    delay = 150,
                    onClick = onCopyLink,
                    icon = Icons.Default.ContentCopy,
                    text = stringResource(R.string.action_copy_link),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )

                ActionButtonRow(
                    visible = isContentVisible,
                    delay = 200,
                    onClick = onShareLink,
                    icon = Icons.Default.Share,
                    text = stringResource(R.string.action_share_link),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}

@Composable
private fun UrlDisplayContainer(
    url: String, visible: Boolean
) {
    AnimatedVisibility(
        visible = visible, enter = fadeIn(
            animationSpec = tween(
                durationMillis = 300, delayMillis = 50
            )
        ) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(durationMillis = 300, delayMillis = 50)
        ), exit = fadeOut(animationSpec = tween(durationMillis = 200)) + slideOutVertically(
            targetOffsetY = { it / 3 }, animationSpec = tween(durationMillis = 200)
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(18.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActionButtonRow(
    visible: Boolean,
    delay: Int,
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    AnimatedVisibility(
        visible = visible, enter = fadeIn(
            animationSpec = tween(
                durationMillis = 300, delayMillis = delay
            )
        ) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(durationMillis = 300, delayMillis = delay)
        ), exit = fadeOut(animationSpec = tween(durationMillis = 200)) + slideOutVertically(
            targetOffsetY = { it / 3 }, animationSpec = tween(durationMillis = 200)
        )
    ) {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = containerColor, contentColor = contentColor
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}