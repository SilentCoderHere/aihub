package com.foss.aihub.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Css
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foss.aihub.R
import com.foss.aihub.ui.components.Md3TopAppBar
import com.foss.aihub.utils.SettingsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomInjectionScreen(
    context: Context, onBack: () -> Unit, settingsManager: SettingsManager
) {
    val settings by settingsManager.settingsFlow.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var jsCode by remember { mutableStateOf(TextFieldValue(settings.customJs)) }
    var cssCode by remember { mutableStateOf(TextFieldValue(settings.customCss)) }

    LaunchedEffect(settings.customJs, settings.customCss) {
        jsCode = TextFieldValue(settings.customJs)
        cssCode = TextFieldValue(settings.customCss)
    }

    val hasUnsavedChanges by remember {
        derivedStateOf {
            jsCode.text != settings.customJs || cssCode.text != settings.customCss
        }
    }

    var showDiscardDialog by remember { mutableStateOf(false) }

    fun saveChanges() {
        settingsManager.updateSettings { current ->
            current.customJs = jsCode.text
            current.customCss = cssCode.text
        }
        scope.launch {
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.msg_changes_saved),
                duration = SnackbarDuration.Short
            )
        }
    }

    BackHandler(enabled = hasUnsavedChanges) {
        showDiscardDialog = true
    }

    Scaffold(
        topBar = {
        Md3TopAppBar(
            title = stringResource(R.string.setting_custom_injection),
            onBack = {
                if (hasUnsavedChanges) showDiscardDialog = true
                else onBack()
            },
            actions = {
                AnimatedVisibility(
                    visible = hasUnsavedChanges, enter = fadeIn(), exit = fadeOut()
                ) {
                    IconButton(onClick = ::saveChanges) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = stringResource(R.string.action_save),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
        )
    },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 4 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = MaterialTheme.shapes.small,
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.msg_custom_injection),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                CodeInputField(
                    value = jsCode,
                    onValueChange = { jsCode = it },
                    label = stringResource(R.string.label_custom_js),
                    icon = Icons.Default.Javascript,
                    placeholder = stringResource(R.string.msg_write_js_here),
                    modifier = Modifier.height(180.dp)
                )
                CodeInputField(
                    value = cssCode,
                    onValueChange = { cssCode = it },
                    label = stringResource(R.string.label_custom_css),
                    icon = Icons.Default.Css,
                    placeholder = stringResource(R.string.msg_write_css_here),
                    modifier = Modifier.height(180.dp)
                )
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text(stringResource(R.string.label_unsaved_changes)) },
            text = { Text(stringResource(R.string.msg_unsaved_changes)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    saveChanges()
                    onBack()
                }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) {
                    Text(stringResource(R.string.action_discard))
                }
            })
    }
}

@Composable
private fun CodeInputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = label, style = MaterialTheme.typography.titleMedium
                )
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace, lineHeight = 18.sp
                ),
                placeholder = {
                    Text(
                        placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}