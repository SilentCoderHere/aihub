package com.foss.aihub.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.foss.aihub.R
import com.foss.aihub.models.AiService
import com.foss.aihub.models.WebViewState

@Composable
fun Md3ServiceCard(
    service: AiService,
    serviceColor: Color,
    isSelected: Boolean,
    state: WebViewState,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    val starColor by animateColorAsState(
        targetValue = if (isFavorite) Color(0xFFFFB300) else colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 250),
        label = "starColor"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = when {
                state == WebViewState.ERROR -> colorScheme.errorContainer.copy(alpha = 0.08f)
                isSelected -> serviceColor.copy(alpha = 0.12f)
                else -> colorScheme.surfaceContainerLow
            }
        ),
        border = when (state) {
            WebViewState.ERROR -> BorderStroke(
                1.dp, colorScheme.error.copy(alpha = 0.25f)
            )
            WebViewState.LOADING -> BorderStroke(
                1.dp, colorScheme.secondary.copy(alpha = 0.25f)
            )
            WebViewState.SUCCESS -> BorderStroke(
                1.dp, serviceColor.copy(alpha = 0.25f)
            )
            else -> null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = when {
                        state == WebViewState.ERROR -> colorScheme.error
                        isSelected -> serviceColor
                        else -> colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = serviceColor.copy(alpha = 0.12f),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Text(
                            text = service.category,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = serviceColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.outline.copy(alpha = 0.5f)
                    )

                    Text(
                        text = when {
                            state != WebViewState.IDLE -> when (state) {
                                WebViewState.LOADING -> stringResource(R.string.label_connecting)
                                WebViewState.ERROR -> stringResource(R.string.label_connection_failed)
                                WebViewState.SUCCESS -> stringResource(R.string.label_ready)
                            }

                            else -> service.description
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            state != WebViewState.IDLE -> when (state) {
                                WebViewState.LOADING -> colorScheme.secondary
                                WebViewState.ERROR -> colorScheme.error
                                WebViewState.SUCCESS -> serviceColor
                            }

                            else -> colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    isSelected -> {
                        Surface(
                            shape = CircleShape,
                            color = serviceColor.copy(alpha = 0.16f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = stringResource(R.string.label_selected),
                                    tint = serviceColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    state == WebViewState.ERROR -> {
                        Icon(
                            imageVector = Icons.Rounded.Error,
                            contentDescription = stringResource(R.string.label_error),
                            tint = colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    state == WebViewState.LOADING -> {
                        CircularProgressIndicator(
                            strokeWidth = 2.5.dp,
                            color = colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    state == WebViewState.SUCCESS -> {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = stringResource(R.string.label_ready),
                            tint = serviceColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconToggleButton(
                    checked = isFavorite,
                    onCheckedChange = { onFavoriteToggle() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        contentDescription = if (isFavorite) stringResource(R.string.msg_remove_from_fav) else stringResource(
                            R.string.msg_add_to_fav
                        ),
                        tint = starColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}