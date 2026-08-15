package com.example.ui.panels

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.utils.Locales
import com.example.viewmodels.CodeEditorViewModel

@Composable
fun SettingsPanel(
    viewModel: CodeEditorViewModel,
    lang: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTheme by viewModel.settingsManager.theme.collectAsState()
    val currentFontSize by viewModel.settingsManager.fontSize.collectAsState()
    val currentThemeColor by viewModel.settingsManager.themeColor.collectAsState()
    val autoSaveEnabled by viewModel.settingsManager.autoSave.collectAsState()

    var showClearConfirm by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = Locales.get("menu_settings", lang),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (showLicenses) {
            // Open Source Licenses screen
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLicenses = false }
                    .padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Locales.get("settings_licenses", lang),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    LicenseItem(
                        name = "Jetpack Compose",
                        author = "Google LLC",
                        license = "Apache License 2.0",
                        url = "https://developer.android.com/jetpack/compose"
                    )
                }
                item {
                    LicenseItem(
                        name = "Room Persistence Database",
                        author = "Google LLC",
                        license = "Apache License 2.0",
                        url = "https://developer.android.com/training/data-storage/room"
                    )
                }
                item {
                    LicenseItem(
                        name = "Kotlin Coroutines",
                        author = "JetBrains s.r.o.",
                        license = "Apache License 2.0",
                        url = "https://github.com/Kotlin/kotlinx.coroutines"
                    )
                }
                item {
                    LicenseItem(
                        name = "Retrofit Network Client",
                        author = "Square, Inc.",
                        license = "Apache License 2.0",
                        url = "https://github.com/square/retrofit"
                    )
                }
                item {
                    LicenseItem(
                        name = "Coil Image Loader",
                        author = "Coil Contributors",
                        license = "Apache License 2.0",
                        url = "https://github.com/coil-kt/coil"
                    )
                }
            }
        } else if (showPrivacy) {
            // Privacy Policy & Terms Screen
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPrivacy = false }
                    .padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "سياسة الخصوصية والشروط",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = "شروط الاستخدام وسياسة الخصوصية",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = """هذا التطبيق مخصص لمساعدة المطورين والمبرمجين على كتابة وتحرير الأكواد البرمجية مباشرة من خلال الهواتف والأجهزة الذكية.

جميع الملفات البرمجية وقواعد البيانات المضافة والمدخلات يتم حفظها وتخزينها بشكل محلي تماماً داخل بيئة التطبيق الآمنة على جهازك الخاص، ولا يتم مشاركتها أو رفعها لأي خوادم خارجية إلا عند قيامك صراحة بالمزامنة مع مستودع GitHub الخاص بك باستخدام الرمز السري الذي تقوم بتوفيره.

نحن نحترم خصوصيتك بالكامل ونلتزم بحماية الأكواد ومشاريعك.""",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f),
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3
                    )
                }
            }
        } else {
            // Main Settings Screen
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Appearance
                item {
                    SettingsSectionTitle(title = Locales.get("settings_appearance", lang))
                    Spacer(modifier = Modifier.height(6.dp))

                    // Theme selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Locales.get("settings_theme_mode", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        Row {
                            FilterChip(
                                selected = currentTheme == "dark",
                                onClick = { viewModel.settingsManager.setTheme("dark") },
                                label = { Text("داكن") },
                                modifier = Modifier.testTag("theme_dark_chip")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            FilterChip(
                                selected = currentTheme == "light",
                                onClick = { viewModel.settingsManager.setTheme("light") },
                                label = { Text("فاتح") },
                                modifier = Modifier.testTag("theme_light_chip")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Font Size Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = Locales.get("settings_font_size", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                            Text(
                                text = "${currentFontSize.toInt()}sp",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = currentFontSize,
                            onValueChange = { viewModel.settingsManager.setFontSize(it) },
                            valueRange = 10f..24f,
                            steps = 7,
                            modifier = Modifier.testTag("font_size_slider")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Accent Color custom picker (standard presets)
                    Column {
                        Text(
                            text = Locales.get("settings_accent_color", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("#007acc", "#68217a", "#e51400", "#1fa67a", "#f0a30a").forEach { colorHex ->
                                val parsedColor = try {
                                    Color(android.graphics.Color.parseColor(colorHex))
                                } catch (e: Exception) {
                                    Color.Blue
                                }
                                val isSelected = currentThemeColor == colorHex

                                Surface(
                                    color = parsedColor,
                                    shape = MaterialTheme.shapes.extraSmall,
                                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.onSecondary) else null,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable { viewModel.settingsManager.setThemeColor(colorHex) }
                                        .testTag("color_preset_$colorHex")
                                ) {}
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto save switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Locales.get("settings_auto_save", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        Switch(
                            checked = autoSaveEnabled,
                            onCheckedChange = { viewModel.settingsManager.setAutoSave(it) },
                            modifier = Modifier.testTag("auto_save_switch")
                        )
                    }
                }

                // Section 2: Language
                item {
                    SettingsSectionTitle(title = "اللغة والموقع / Language")
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Locales.get("settings_lang", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        Row {
                            FilterChip(
                                selected = lang == "ar",
                                onClick = { viewModel.settingsManager.setLanguage("ar") },
                                label = { Text("العربية") },
                                modifier = Modifier.testTag("lang_ar_chip")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            FilterChip(
                                selected = lang == "en",
                                onClick = { viewModel.settingsManager.setLanguage("en") },
                                label = { Text("English") },
                                modifier = Modifier.testTag("lang_en_chip")
                            )
                        }
                    }
                }

                // Section 3: Data & Storage
                item {
                    SettingsSectionTitle(title = Locales.get("settings_data_storage", lang))
                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_data_btn")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = Locales.get("settings_clear_data", lang))
                    }
                }

                // Section 4: About
                item {
                    SettingsSectionTitle(title = Locales.get("settings_about", lang))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = Locales.get("settings_desc", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f),
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.2f
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "نسخة التطبيق: 1.0.0 (بناء 20260813)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPrivacy = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "سياسة الخصوصية وشروط الخدمة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Section 5: Open Source Licenses
                item {
                    SettingsSectionTitle(title = Locales.get("settings_licenses", lang))
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLicenses = true }
                            .testTag("licenses_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "استعراض المكتبات مفتوحة المصدر",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                                Text(
                                    text = "قائمة بالمكونات والمكتبات المستخدمة وتراخيصها",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Section 6: Help & Feedback
                item {
                    SettingsSectionTitle(title = Locales.get("settings_help", lang))
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:support@codeeditor.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Code Editor Mobile - Feedback")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                viewModel.logWarning("Feedback", "لم يتم العثور على تطبيق بريد إلكتروني لإرسال التعليقات.")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_feedback_btn")
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = Locales.get("settings_help_btn", lang))
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(text = "تأكيد مسح البيانات") },
            text = { Text(text = Locales.get("settings_clear_confirm", lang)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_data_btn")
                ) {
                    Text(text = "مسح بالكامل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(text = Locales.get("cancel_btn", lang))
                }
            }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun LicenseItem(name: String, author: String, license: String, url: String) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondary
            )
            Text(
                text = "المؤلف: $author",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الترخيص: $license",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "زيارة الموقع ↗",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        }
    }
}
