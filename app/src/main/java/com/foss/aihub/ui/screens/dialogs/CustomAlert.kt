package com.foss.aihub.ui.screens.dialogs

import android.content.Context
import android.webkit.JsResult
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.foss.aihub.R

@Composable
fun CustomAlertDialog(
    context: Context,
    jsMessage: String?,
    jsResult: JsResult?,
    onDismiss: () -> Unit
) {
    if (jsMessage != null) {
        AlertDialog(
            onDismissRequest = {
                jsResult?.cancel()
                onDismiss()
            },
            title = {
                Text(
                    text = context.getString(R.string.label_alert),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = jsMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        jsResult?.confirm()
                        onDismiss()
                    },
                ) {
                    Text(context.getString(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        jsResult?.cancel()
                        onDismiss()
                    },
                ) {
                    Text(context.getString(R.string.action_cancel))
                }
            },
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        )
    }
}