package com.example.ui.panels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.utils.Locales
import com.example.viewmodels.CodeEditorViewModel

@Composable
fun ExtensionsPanel(
    viewModel: CodeEditorViewModel,
    lang: String,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.extensionSearchQuery.collectAsState()
    val filteredExtensions by viewModel.filteredExtensions.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = Locales.get("menu_extensions", lang),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateExtensionSearchQuery(it) },
            placeholder = { Text(text = Locales.get("ext_search_placeholder", lang)) },
            leadingIcon = { Icon(Icons.Default.Extension, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("extensions_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredExtensions) { ext ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = when (ext.iconName) {
                                        "kotlin" -> Icons.Default.Code
                                        "database" -> Icons.Default.Storage
                                        "language" -> Icons.Default.Language
                                        "git" -> Icons.Default.SyncAlt
                                        "bug" -> Icons.Default.BugReport
                                        "ai" -> Icons.Default.Psychology
                                        else -> Icons.Default.Settings
                                    },
                                    contentDescription = ext.name,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = ext.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                    Text(
                                        text = "${ext.author} • v${ext.version}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            // Install/Uninstall Button
                            Button(
                                onClick = { viewModel.toggleExtensionInstallation(ext) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (ext.isInstalled) {
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    contentColor = if (ext.isInstalled) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onPrimary
                                    }
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("extension_install_btn_${ext.id}")
                            ) {
                                Text(
                                    text = if (ext.isInstalled) {
                                        Locales.get("ext_uninstall", lang)
                                    } else {
                                        Locales.get("ext_install", lang)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = ext.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f),
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.15
                        )
                    }
                }
            }
        }
    }
}
