package com.foss.aihub.ui.screens.dialogs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.foss.aihub.R
import com.foss.aihub.utils.capitalizeFirstLetter

@Composable
fun FilterDialog(
    categories: List<String>,
    prices: List<String>,
    privacyOptions: List<String>,
    selectedCategories: Set<String>,
    selectedPrices: Set<String>,
    selectedPrivacy: Set<String>,
    selectedLoginRequired: Boolean?,
    onCategoriesChange: (Set<String>) -> Unit,
    onPricesChange: (Set<String>) -> Unit,
    onPrivacyChange: (Set<String>) -> Unit,
    onLoginRequiredChange: (Boolean?) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.action_filter).capitalizeFirstLetter(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Rounded.Clear,
                            contentDescription = stringResource(R.string.action_close)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                FilterSection(
                    label = stringResource(R.string.filter_category).capitalizeFirstLetter(),
                    options = categories.map { it.capitalizeFirstLetter() },
                    selected = selectedCategories,
                    onSelect = { onCategoriesChange(it) })
                Spacer(modifier = Modifier.height(16.dp))

                FilterSection(
                    label = stringResource(R.string.filter_price).capitalizeFirstLetter(),
                    options = prices.map { it.capitalizeFirstLetter() },
                    selected = selectedPrices,
                    onSelect = { onPricesChange(it) })
                Spacer(modifier = Modifier.height(16.dp))

                FilterSection(
                    label = stringResource(R.string.filter_privacy).capitalizeFirstLetter(),
                    options = privacyOptions.map { it.capitalizeFirstLetter() },
                    selected = selectedPrivacy,
                    onSelect = { onPrivacyChange(it) })
                Spacer(modifier = Modifier.height(16.dp))

                LoginRequiredFilterSection(
                    selected = selectedLoginRequired, onSelect = onLoginRequiredChange
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        onClear()
                        onDismiss()
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        Icons.Rounded.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_clear_filters))
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    label: String, options: List<String>, selected: Set<String>, onSelect: (Set<String>) -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = option in selected
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onSelect(
                            if (isSelected) selected - option else selected + option
                        )
                    },
                    label = {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        enabled = true,
                        selected = false
                    )
                )
            }
        }
    }
}

@Composable
private fun LoginRequiredFilterSection(
    selected: Boolean?, onSelect: (Boolean?) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.label_login_required),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val options = listOf("Required", "Not Required")
            options.forEach { option ->
                val isSelected = when (option) {
                    "Required" -> selected == true
                    "Not Required" -> selected == false
                    else -> false
                }
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) {
                            onSelect(null)
                        } else {
                            onSelect(option == "Required")
                        }
                    },
                    label = {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        enabled = true,
                        selected = false
                    )
                )
            }
        }
    }
}