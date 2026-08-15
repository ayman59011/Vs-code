package com.example.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BreakpointItem
import com.example.data.DebugLog
import com.example.data.DebugVariable
import com.example.data.StackFrame
import com.example.utils.Locales
import com.example.viewmodels.CodeEditorViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DebuggerPanel(
    viewModel: CodeEditorViewModel,
    lang: String,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Stepper & Inspect, 1 = Console Logs

    val logs by viewModel.debugLogs.collectAsState()
    val isDebugging by viewModel.isDebugging.collectAsState()
    val isPaused by viewModel.isSimulationPaused.collectAsState()
    val currentLine by viewModel.currentExecutionLine.collectAsState()
    val breakpoints by viewModel.breakpoints.collectAsState()
    val callStack by viewModel.callStack.collectAsState()
    val localVariables by viewModel.localVariables.collectAsState()
    val activeFile by viewModel.activeFile.collectAsState()

    var newBreakpointLine by remember { mutableStateOf("") }
    val sdf = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Locales.get("menu_debugger", lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondary
            )

            // Debugger state badge
            Surface(
                color = if (isDebugging) Color(0xFF4CAF50).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (isDebugging) Color(0xFF4CAF50) else Color.Gray,
                                shape = MaterialTheme.shapes.extraSmall
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isDebugging) "Paused: Line $currentLine" else "Ready (Idle)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDebugging) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Simulation Toolbar Controls (Start, Step Over, Step Into, Stop)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Run / Start Simulation
                IconButton(
                    onClick = { viewModel.startDebugSimulation() },
                    modifier = Modifier.size(36.dp).testTag("debug_start_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Simulation (F5)",
                        tint = Color(0xFF4CAF50)
                    )
                }

                // Step Over
                IconButton(
                    onClick = { viewModel.stepOverSimulation() },
                    enabled = isDebugging,
                    modifier = Modifier.size(36.dp).testTag("debug_step_over_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Redo,
                        contentDescription = "Step Over (F10)",
                        tint = if (isDebugging) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                // Step Into
                IconButton(
                    onClick = { viewModel.stepIntoSimulation() },
                    enabled = isDebugging,
                    modifier = Modifier.size(36.dp).testTag("debug_step_into_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.South,
                        contentDescription = "Step Into (F11)",
                        tint = if (isDebugging) Color(0xFFFF9800) else Color.Gray
                    )
                }

                // Stop Simulation
                IconButton(
                    onClick = { viewModel.stopDebugSimulation() },
                    enabled = isDebugging,
                    modifier = Modifier.size(36.dp).testTag("debug_stop_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop (Shift+F5)",
                        tint = if (isDebugging) MaterialTheme.colorScheme.error else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sub Tabs Switcher: Inspector vs Console
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("المتغيرات والتتبع", style = MaterialTheme.typography.labelSmall) },
                icon = { Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(14.dp)) }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("سجل التصحيح (${logs.size})", style = MaterialTheme.typography.labelSmall) },
                icon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(14.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (activeSubTab == 0) {
            // Variables, Call Stack & Breakpoints Inspector
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Breakpoints Management Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "نقاط التوقف (Breakpoints)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = { viewModel.clearAllBreakpoints() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Text("مسح الكل", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                            }

                            // Add breakpoint row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newBreakpointLine,
                                    onValueChange = { newBreakpointLine = it },
                                    placeholder = { Text("رقم السطر (e.g. 6)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("breakpoint_input")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        newBreakpointLine.toIntOrNull()?.let {
                                            viewModel.toggleBreakpoint(it)
                                            newBreakpointLine = ""
                                        }
                                    },
                                    enabled = newBreakpointLine.toIntOrNull() != null
                                ) {
                                    Text("إضافة")
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Breakpoint items list
                            if (breakpoints.isEmpty()) {
                                Text(
                                    text = "لا توجد نقاط توقف محددة.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    breakpoints.sorted().forEach { line ->
                                        Surface(
                                            color = Color(0xFFF44336).copy(alpha = 0.15f),
                                            shape = MaterialTheme.shapes.extraSmall,
                                            modifier = Modifier.clickable { viewModel.toggleBreakpoint(line) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(Color(0xFFF44336), shape = MaterialTheme.shapes.extraSmall)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Line $line",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFF44336)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF44336),
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Local Variables Inspector
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "المتغيرات المحلية (Local Variables)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isDebugging) "${localVariables.size} vars" else "Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (localVariables.isEmpty()) {
                                Text(
                                    text = if (isDebugging) "لا توجد متغيرات محلية في النطاق الحالي." else "اضغط على زر التشغيل (F5) لمحاكاة الكود واستعراض المتغيرات.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    localVariables.forEach { variable ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = variable.name,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = ": ${variable.type}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
                                                )
                                            }

                                            Text(
                                                text = variable.value,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF4CAF50)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Call Stack Section
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مكدس الاستدعاءات (Call Stack)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${callStack.size} frames",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (callStack.isEmpty()) {
                                Text(
                                    text = "المكدس فارغ حالياً.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    callStack.forEachIndexed { idx, frame ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (idx == 0) Icons.Default.ArrowRight else Icons.Default.DragHandle,
                                                contentDescription = null,
                                                tint = if (idx == 0) Color(0xFFFFB300) else Color.Gray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${frame.functionName} (${frame.fileName}:${frame.lineNumber})",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Normal,
                                                color = if (idx == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Debug Logs / Console Stream
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سجل الأحداث والرسائل:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    IconButton(
                        onClick = { viewModel.clearDebugLogs() },
                        modifier = Modifier.size(24.dp).testTag("debug_clear_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = Locales.get("debug_clear", lang),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Locales.get("debug_empty", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logs) { log ->
                            val color = when (log.level) {
                                "ERROR" -> Color(0xFFF44336)
                                "WARNING" -> Color(0xFFFF9800)
                                "DEBUG" -> Color(0xFF2196F3)
                                else -> Color(0xFF4CAF50)
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                                    .padding(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sdf.format(Date(log.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = color.copy(alpha = 0.15f),
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Text(
                                            text = " ${log.level} ",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = color
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "[${log.tag}]",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }

                                Text(
                                    text = log.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
