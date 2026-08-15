package com.example.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.DbConnection
import com.example.data.TableColumnSchema
import com.example.utils.Locales
import com.example.viewmodels.CodeEditorViewModel

@Composable
fun DatabasePanel(
    viewModel: CodeEditorViewModel,
    lang: String,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = SQLite Schema & Query, 1 = Connections Manager

    val connections by viewModel.dbConnections.collectAsState()
    val selectedTable by viewModel.selectedDbTable.collectAsState()
    val tableSchema by viewModel.tableSchema.collectAsState()
    val resultHeaders by viewModel.queryResultHeaders.collectAsState()
    val resultRows by viewModel.queryResultRows.collectAsState()
    val queryInfo by viewModel.queryExecutionInfo.collectAsState()
    val isExecuting by viewModel.isQueryExecuting.collectAsState()

    var sqlInput by remember { mutableStateOf("SELECT * FROM users LIMIT 10;") }
    var showTableDropdown by remember { mutableStateOf(false) }

    val availableTables = listOf("users", "products", "editor_files", "git_commits", "debug_logs", "app_extensions")

    // Connections Dialog State
    var showAddDialog by remember { mutableStateOf(false) }
    var connName by remember { mutableStateOf("") }
    var connType by remember { mutableStateOf("SQLite") }
    var connHost by remember { mutableStateOf("/data/data/com.example/databases/app.db") }
    var connDbName by remember { mutableStateOf("main.sqlite") }
    var connUser by remember { mutableStateOf("admin") }
    var connTables by remember { mutableStateOf("users;orders;products;logs") }

    LaunchedEffect(Unit) {
        if (selectedTable != null) {
            viewModel.loadTableSchemaAndSampleData(selectedTable!!)
        } else {
            viewModel.selectDbTable("users")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Panel Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Locales.get("menu_database", lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondary
            )

            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.size(28.dp).testTag("add_db_conn_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Segmented Tabs
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("SQLite Browser", style = MaterialTheme.typography.labelSmall) },
                icon = { Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("الاتصالات (${connections.size})", style = MaterialTheme.typography.labelSmall) },
                icon = { Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (activeTab == 0) {
            // SQLite Browser Section: Table Selector + Schema Inspector + SQL Query Execution
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Table Selector Box
                item {
                    Column {
                        Text(
                            text = "اختر جدول SQLite لاستعراض المخطط:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableTables.forEach { tbl ->
                                val isSelected = selectedTable == tbl
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.selectDbTable(tbl)
                                        sqlInput = "SELECT * FROM $tbl LIMIT 10;"
                                    },
                                    label = { Text(tbl, style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.TableChart,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. Table Schema Inspector
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مخطط الحقول (Schema): ${selectedTable ?: "users"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${tableSchema.size} أعمدة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Schema Column list
                            tableSchema.forEach { col ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = col.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondary
                                        )
                                        if (col.isPrimaryKey) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = Color(0xFFFFB300).copy(alpha = 0.2f),
                                                shape = MaterialTheme.shapes.extraSmall
                                            ) {
                                                Text(
                                                    text = "PK",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFFFB300),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = col.type,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. SQL Query Editor & Presets
                item {
                    Column {
                        Text(
                            text = "تنفيذ استعلام SQL (Query Editor):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = sqlInput,
                            onValueChange = { sqlInput = it },
                            singleLine = false,
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.fillMaxWidth().testTag("sql_query_input")
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick SQL chips & Run button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    "SELECT *" to "SELECT * FROM ${selectedTable ?: "users"} LIMIT 20;",
                                    "COUNT" to "SELECT count(*) as total_rows FROM ${selectedTable ?: "users"};",
                                    "WHERE" to "SELECT * FROM ${selectedTable ?: "users"} WHERE id = 1;",
                                    "ORDER" to "SELECT * FROM ${selectedTable ?: "users"} ORDER BY id DESC;"
                                ).forEach { (label, query) ->
                                    AssistChip(
                                        onClick = { sqlInput = query },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = { viewModel.executeCustomSqlQuery(sqlInput) },
                                enabled = !isExecuting && sqlInput.isNotBlank(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("run_sql_query_btn")
                            ) {
                                if (isExecuting) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تشغيل", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // 4. Query Result Viewer (Tabular Data Grid)
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "نتائج الاستعلام (Query Results):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                            Text(
                                text = queryInfo,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Interactive Data Grid
                        val scrollStateHorizontal = rememberScrollState()
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .horizontalScroll(scrollStateHorizontal)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                // Header row
                                Row(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    resultHeaders.forEach { header ->
                                        Text(
                                            text = header,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.width(100.dp)
                                        )
                                    }
                                }

                                Divider()

                                // Data rows
                                resultRows.forEachIndexed { index, row ->
                                    val bg = if (index % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                    Row(
                                        modifier = Modifier
                                            .background(bg)
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        row.forEach { cell ->
                                            Text(
                                                text = cell,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSecondary,
                                                modifier = Modifier.width(100.dp)
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
            // Connections Manager Section
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (connections.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = Locales.get("db_no_connections", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    items(connections) { conn ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = conn.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondary
                                        )
                                        Text(
                                            text = "${conn.type} • ${conn.databaseName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deleteDbConnection(conn) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add connection dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = Locales.get("db_add_conn", lang)) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        OutlinedTextField(
                            value = connName,
                            onValueChange = { connName = it },
                            label = { Text(Locales.get("db_name", lang)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = connType,
                            onValueChange = { connType = it },
                            label = { Text(Locales.get("db_type", lang)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = connHost,
                            onValueChange = { connHost = it },
                            label = { Text(Locales.get("db_host", lang)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = connDbName,
                            onValueChange = { connDbName = it },
                            label = { Text(Locales.get("db_dbname", lang)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = connUser,
                            onValueChange = { connUser = it },
                            label = { Text(Locales.get("db_username", lang)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = connTables,
                            onValueChange = { connTables = it },
                            label = { Text(Locales.get("db_tables", lang)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (connName.isNotBlank() && connDbName.isNotBlank()) {
                            viewModel.addNewDbConnection(
                                DbConnection(
                                    name = connName,
                                    type = connType,
                                    host = connHost,
                                    databaseName = connDbName,
                                    username = connUser,
                                    tables = connTables
                                )
                            )
                            connName = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text(Locales.get("db_save_conn", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(Locales.get("cancel_btn", lang))
                }
            }
        )
    }
}
