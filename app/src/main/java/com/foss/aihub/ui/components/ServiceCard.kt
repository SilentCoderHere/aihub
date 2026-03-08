package com.foss.aihub.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    val colorScheme = colorScheme

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        shape = MaterialTheme.shapes.medium,
        color = when {
            state == WebViewState.ERROR -> colorScheme.errorContainer.copy(alpha = 0.08f)
            isSelected -> serviceColor.copy(alpha = 0.06f)
            else -> colorScheme.surfaceContainerLow
        },
        border = when {
            state == WebViewState.ERROR -> BorderStroke(
                1.dp, colorScheme.error.copy(alpha = 0.25f)
            )

            isSelected -> BorderStroke(
                1.5.dp, serviceColor.copy(alpha = 0.35f)
            )

            else -> null
        },
        tonalElevation = when {
            isSelected -> 2.dp
            else -> 1.dp
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = service.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = when {
                            state == WebViewState.ERROR -> colorScheme.error
                            isSelected -> serviceColor
                            else -> colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    when {
                        isSelected -> {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Selected",
                                tint = serviceColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        state == WebViewState.ERROR -> {
                            Icon(
                                imageVector = Icons.Rounded.Error,
                                contentDescription = "Error",
                                tint = colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        state == WebViewState.LOADING -> {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        state == WebViewState.SUCCESS -> {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Ready",
                                tint = serviceColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = serviceColor.copy(alpha = 0.10f),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Text(
                            text = service.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = serviceColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
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
                                WebViewState.LOADING -> "Connecting..."
                                WebViewState.ERROR -> "Connection failed"
                                WebViewState.SUCCESS -> "Ready"
                                else -> ""
                            }

                            else -> service.description
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            state != WebViewState.IDLE -> when (state) {
                                WebViewState.LOADING -> colorScheme.secondary
                                WebViewState.ERROR -> colorScheme.error
                                WebViewState.SUCCESS -> serviceColor
                                else -> colorScheme.onSurfaceVariant
                            }

                            else -> colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            val starColor by animateColorAsState(
                targetValue = if (isFavorite) Color(0xFFFFB300) else colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                animationSpec = tween(durationMillis = 250),
                label = "starColor"
            )

            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = starColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
