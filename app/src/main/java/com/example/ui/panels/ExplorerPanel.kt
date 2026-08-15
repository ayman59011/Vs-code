package com.example.ui.panels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.EditorFile
import com.example.utils.Locales
import com.example.viewmodels.CodeEditorViewModel

@Composable
fun ExplorerPanel(
    viewModel: CodeEditorViewModel,
    lang: String,
    modifier: Modifier = Modifier
) {
    val allFiles by viewModel.allFiles.collectAsState()
    val activeFile by viewModel.activeFile.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newFileLang by remember { mutableStateOf("kotlin") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Locales.get("menu_explorer", lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondary
            )
            IconButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("new_file_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = Locales.get("new_file", lang),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (allFiles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد ملفات. اضغط + لإنشاء ملف.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(allFiles) { file ->
                    val isSelected = activeFile?.id == file.id
                    val bgColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    }

                    Surface(
                        color = bgColor,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .testTag("file_item_${file.name}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectFile(file) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = when (file.language.lowercase()) {
                                        "kotlin", "kt" -> Icons.Default.Code
                                        "javascript", "js" -> Icons.Default.Javascript
                                        "css" -> Icons.Default.Css
                                        "sql" -> Icons.Default.Storage
                                        else -> Icons.Outlined.Description
                                    },
                                    contentDescription = file.language,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondary
                                )
                            }

                            // Delete button
                            IconButton(
                                onClick = { viewModel.deleteFile(file) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = Locales.get("delete", lang),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(text = Locales.get("new_file", lang)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text(text = Locales.get("file_name_label", lang)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_file_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = Locales.get("file_lang_label", lang),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("kotlin", "javascript", "css", "sql").forEach { langOption ->
                            val selected = newFileLang == langOption
                            FilterChip(
                                selected = selected,
                                onClick = { newFileLang = langOption },
                                label = { Text(langOption.uppercase()) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            viewModel.createNewFile(newFileName, newFileLang)
                            newFileName = ""
                            showCreateDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_file_button")
                ) {
                    Text(text = Locales.get("create_btn", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(text = Locales.get("cancel_btn", lang))
                }
            }
        )
    }
}
