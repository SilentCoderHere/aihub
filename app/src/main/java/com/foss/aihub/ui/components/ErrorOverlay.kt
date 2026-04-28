package com.foss.aihub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foss.aihub.R

@Composable
fun ErrorOverlay(
    modifier: Modifier = Modifier,
    errorType: ErrorType,
    errorCode: Int = -1,
    errorMessage: String? = null,
    serviceName: String,
    accentColor: Color,
    onRetry: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.38f),
                        shape = CircleShape
                    ), contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = errorType.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(errorType.titleRes),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = errorType.getDescription(
                    serviceName = serviceName, errorCode = errorCode
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3f
            )

            Spacer(modifier = Modifier.height(32.dp))

            FilledTonalButton(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = accentColor, contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.action_retry),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.material3.CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_details),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(
                    R.string.error_service_info_format, serviceName, errorCode
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

enum class ErrorType(
    val icon: ImageVector, val titleRes: Int
) {
    NETWORK_ERROR(
        icon = Icons.Outlined.WifiOff, titleRes = R.string.error_title_no_connection
    ),
    PROXY_ERROR(
        icon = Icons.Outlined.Wifi, titleRes = R.string.error_title_proxy_error
    ),
    HTTP_ERROR(
        icon = Icons.Outlined.ErrorOutline, titleRes = R.string.error_title_server_error
    ),
    SSL_ERROR(
        icon = Icons.Outlined.ErrorOutline, titleRes = R.string.error_title_security_issue
    );

    @Composable
    fun getDescription(
        serviceName: String, errorCode: Int
    ): String = when (this) {
        NETWORK_ERROR -> stringResource(
            R.string.error_desc_no_connection, serviceName
        )

        PROXY_ERROR -> stringResource(R.string.error_desc_proxy_error)

        HTTP_ERROR -> stringResource(
            R.string.error_desc_http_error, serviceName, errorCode
        )

        SSL_ERROR -> stringResource(
            R.string.error_desc_ssl_issue, serviceName
        )
    }

    companion object {
        fun shouldShowOverlay(errorCode: Int): Boolean = when (errorCode) {
            in 500..599 -> false
            -2, -4, -5, -6, -7, -8, -10, -11 -> true
            -3 -> true
            in 400..499 -> true
            else -> false
        }

        fun fromErrorCode(errorCode: Int): ErrorType = when (errorCode) {
            -2, -4, -6, -7, -8, -10, -11 -> NETWORK_ERROR
            -3 -> SSL_ERROR
            -5 -> PROXY_ERROR
            in 400..499 -> HTTP_ERROR
            else -> NETWORK_ERROR
        }
    }
}