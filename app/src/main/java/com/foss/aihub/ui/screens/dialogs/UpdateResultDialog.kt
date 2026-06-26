package com.foss.aihub.ui.screens.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foss.aihub.R
import com.foss.aihub.models.AiService
import com.foss.aihub.models.ModifiedServiceInfo

@Composable
fun UpdateResultDialog(
    added: List<AiService>,
    removed: List<AiService>,
    modified: List<ModifiedServiceInfo>,
    newCategories: Set<String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.update_result_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (added.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.update_added_services, added.size),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    added.forEach { service ->
                        Text(
                            text = "• ${service.name} (${service.category})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (removed.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.update_removed_services, removed.size),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    removed.forEach { service ->
                        Text(
                            text = "• ${service.name} (${service.category})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (modified.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.update_modified_services, modified.size),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    modified.forEach { info ->
                        Text(
                            text = "• ${info.service.name}:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        info.changes.forEach { change ->
                            Text(
                                text = "    - $change", style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                if (newCategories.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.update_new_categories, newCategories.size),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    newCategories.forEach { category ->
                        Text(
                            text = "• $category", style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (added.isEmpty() && removed.isEmpty() && modified.isEmpty() && newCategories.isEmpty()) {
                    Text(
                        text = stringResource(R.string.update_no_changes),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        },
    )
}