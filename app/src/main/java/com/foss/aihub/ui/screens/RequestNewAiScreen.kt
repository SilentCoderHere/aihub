package com.foss.aihub.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.foss.aihub.R
import com.foss.aihub.models.AiService
import com.foss.aihub.utils.GITHUB_REPO_NAME
import com.foss.aihub.utils.GITHUB_USER_NAME
import com.foss.aihub.utils.isDuplicateName
import com.foss.aihub.utils.isDuplicateUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestNewAiScreen(
    onBack: () -> Unit,
    allServices: List<AiService>,
    onSubmit: (content: String, method: String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var showMethodSheet by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val nameError = name.isNotBlank() && isDuplicateName(name, allServices)
    val urlError = website.isNotBlank() && isDuplicateUrl(website, allServices)
    val formValid = name.isNotBlank() && website.isNotBlank() && !nameError && !urlError

    fun buildEmailContent(): String {
        return """
            Hi, I'd like to request a new AI service.
            
            Name: ${name.trim()}
            Chat / Website Link: ${website.trim()}
        """.trimIndent()
    }

    fun buildGithubIssueUrl(): String {
        return "https://github.com/$GITHUB_USER_NAME/$GITHUB_REPO_NAME/issues/new".toUri()
            .buildUpon().appendQueryParameter("template", "request_new_ai.yml")
            .appendQueryParameter("website", website.trim()).build().toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.label_request_new_ai)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }, containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_suggest_missing_ai),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.msg_tell_which_ai),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            showError = false
                        },
                        label = { Text(stringResource(R.string.label_service_name)) },
                        placeholder = { Text("ChatGPT") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        isError = nameError,
                        supportingText = {
                            if (nameError) {
                                Text(
                                    text = stringResource(R.string.msg_service_already_exists),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors()
                    )

                    OutlinedTextField(
                        value = website,
                        onValueChange = {
                            website = it
                            showError = false
                        },
                        label = { Text(stringResource(R.string.label_service_link)) },
                        placeholder = { Text("www.example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        isError = urlError,
                        supportingText = {
                            if (urlError) {
                                Text(
                                    text = stringResource(R.string.msg_service_already_exists),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors()
                    )

                    Button(
                        onClick = {
                            if (formValid) {
                                showMethodSheet = true
                            } else {
                                showError = true
                            }
                        },
                        enabled = name.isNotBlank() && website.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            stringResource(R.string.action_submit_request),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    if (showError && (nameError || urlError)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.msg_fix_errors_before_submit),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.msg_your_request_matters),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }

    if (showMethodSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMethodSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_submit_via),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                MethodCard(
                    title = stringResource(R.string.label_github_issue),
                    description = stringResource(R.string.desc_github_issue),
                    onClick = {
                        showMethodSheet = false
                        val url = buildGithubIssueUrl()
                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                        onSubmit(url, "github")
                    })
                MethodCard(
                    title = stringResource(R.string.label_email_method),
                    description = stringResource(R.string.desc_email_method),
                    onClick = {
                        showMethodSheet = false
                        onSubmit(buildEmailContent(), "email")
                    })
            }
        }
    }
}

@Composable
private fun MethodCard(
    title: String, description: String, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}