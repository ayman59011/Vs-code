package com.example.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.repository.CodeEditorRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CodeEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = CodeEditorRepository(db.codeEditorDao())
    val settingsManager = SettingsManager(application)

    // Side Bar Panels: files, search, extensions, github, db, debug, version, settings
    private val _activePanel = MutableStateFlow("files")
    val activePanel: StateFlow<String> = _activePanel

    // Dynamic Sidebar Width state in Dp (adjustable with draggable divider)
    private val _sidebarWidthDp = MutableStateFlow(280f)
    val sidebarWidthDp: StateFlow<Float> = _sidebarWidthDp

    fun setSidebarWidth(width: Float) {
        _sidebarWidthDp.value = width.coerceIn(160f, 500f)
    }

    // File state
    val allFiles: StateFlow<List<EditorFile>> = repository.allFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openFiles: StateFlow<List<EditorFile>> = repository.openFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeFile = MutableStateFlow<EditorFile?>(null)
    val activeFile: StateFlow<EditorFile?> = _activeFile

    private val _editorContent = MutableStateFlow("")
    val editorContent: StateFlow<String> = _editorContent

    // In-editor Search Bar state (for Ctrl+F shortcut)
    private val _isFindInFileOpen = MutableStateFlow(false)
    val isFindInFileOpen: StateFlow<Boolean> = _isFindInFileOpen

    private val _findInFileQuery = MutableStateFlow("")
    val findInFileQuery: StateFlow<String> = _findInFileQuery

    fun setFindInFileOpen(open: Boolean) {
        _isFindInFileOpen.value = open
        if (!open) {
            _findInFileQuery.value = ""
        }
    }

    fun setFindInFileQuery(query: String) {
        _findInFileQuery.value = query
    }

    // Global Code Search state (Workspace Search)
    private val _codeSearchQuery = MutableStateFlow("")
    val codeSearchQuery: StateFlow<String> = _codeSearchQuery

    val searchResults: StateFlow<List<Pair<EditorFile, List<Int>>>> = combine(allFiles, _codeSearchQuery) { files, query ->
        if (query.length < 2) emptyList()
        else {
            files.mapNotNull { file ->
                val lines = file.content.lines()
                val matchingLineNumbers = lines.mapIndexedNotNull { index, line ->
                    if (line.contains(query, ignoreCase = true)) index + 1 else null
                }
                if (matchingLineNumbers.isNotEmpty()) file to matchingLineNumbers else null
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Database Connection State
    val dbConnections: StateFlow<List<DbConnection>> = repository.allConnections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedConnection = MutableStateFlow<DbConnection?>(null)
    val selectedConnection: StateFlow<DbConnection?> = _selectedConnection

    // Extensions State
    val extensions: StateFlow<List<AppExtension>> = repository.allExtensions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _extensionSearchQuery = MutableStateFlow("")
    val extensionSearchQuery: StateFlow<String> = _extensionSearchQuery

    val filteredExtensions: StateFlow<List<AppExtension>> = combine(extensions, _extensionSearchQuery) { exts, q ->
        if (q.isBlank()) exts
        else exts.filter { it.name.contains(q, ignoreCase = true) || it.description.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Git Local Repository State & Commits
    val allCommits: StateFlow<List<GitCommit>> = repository.allCommits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGitRepoInitialized = MutableStateFlow(true)
    val isGitRepoInitialized: StateFlow<Boolean> = _isGitRepoInitialized

    fun initGitRepo(branch: String = "main") {
        viewModelScope.launch {
            _isGitRepoInitialized.value = true
            _currentBranch.value = branch
            val initialCommit = GitCommit(
                hash = generateCommitHash(),
                message = "Initial commit: Repository initialized",
                author = "Developer <dev@local>",
                branch = branch,
                timestamp = System.currentTimeMillis(),
                filesChanged = allFiles.value.size,
                additions = 100,
                deletions = 0
            )
            repository.insertCommit(initialCommit)
            addTerminalLog("[Git] Initialized empty Git repository in /workspace/.git/ on branch $branch")
            logInfo("Git", "تمت تهيئة مستودع Git محلي بنجاح على الفرع: $branch")
        }
    }

    fun commitChanges(message: String): Boolean {
        if (message.isBlank()) return false
        val file = _activeFile.value
        val hash = generateCommitHash()
        val commit = GitCommit(
            hash = hash,
            message = message.trim(),
            author = "Developer <dev@local>",
            branch = _currentBranch.value,
            timestamp = System.currentTimeMillis(),
            filesChanged = if (file != null) 1 else 2,
            additions = (5..30).random(),
            deletions = (0..5).random()
        )
        viewModelScope.launch {
            repository.insertCommit(commit)
            addTerminalLog("[Git ${_currentBranch.value} $hash] $message")
            logInfo("Git", "تم إجراء Commit جديد: [$hash] $message")
        }
        return true
    }

    private fun generateCommitHash(): String {
        val chars = "0123456789abcdef"
        return (1..7).map { chars.random() }.joinToString("")
    }

    // SQLite Database Schema & Query Results
    private val _selectedDbTable = MutableStateFlow<String?>("users")
    val selectedDbTable: StateFlow<String?> = _selectedDbTable

    private val _tableSchema = MutableStateFlow<List<TableColumnSchema>>(emptyList())
    val tableSchema: StateFlow<List<TableColumnSchema>> = _tableSchema

    private val _queryResultHeaders = MutableStateFlow<List<String>>(listOf("id", "username", "email", "role", "is_active", "created_at"))
    val queryResultHeaders: StateFlow<List<String>> = _queryResultHeaders

    private val _queryResultRows = MutableStateFlow<List<List<String>>>(
        listOf(
            listOf("1", "admin_ayman", "ayman@company.dev", "SUPER_ADMIN", "true", "2026-01-10"),
            listOf("2", "sarah_k", "sarah.k@cloud.io", "DEVELOPER", "true", "2026-02-14"),
            listOf("3", "omar_lead", "omar.lead@tech.org", "TECH_LEAD", "true", "2026-03-01"),
            listOf("4", "noura_ui", "noura@design.ai", "DESIGNER", "true", "2026-04-12"),
            listOf("5", "guest_tester", "guest@sandbox.app", "TESTER", "false", "2026-05-18")
        )
    )
    val queryResultRows: StateFlow<List<List<String>>> = _queryResultRows

    private val _queryExecutionInfo = MutableStateFlow("5 rows returned in 14ms (SQLite v3.42.0)")
    val queryExecutionInfo: StateFlow<String> = _queryExecutionInfo

    private val _isQueryExecuting = MutableStateFlow(false)
    val isQueryExecuting: StateFlow<Boolean> = _isQueryExecuting

    fun selectDbTable(tableName: String) {
        _selectedDbTable.value = tableName
        loadTableSchemaAndSampleData(tableName)
        logInfo("Database", "تم استعراض مخطط وبيانات الجدول: $tableName")
    }

    fun loadTableSchemaAndSampleData(tableName: String) {
        when (tableName.lowercase()) {
            "users", "accounts" -> {
                _tableSchema.value = listOf(
                    TableColumnSchema("id", "INTEGER", isPrimaryKey = true, isNullable = false),
                    TableColumnSchema("username", "TEXT", isNullable = false),
                    TableColumnSchema("email", "TEXT", isNullable = false),
                    TableColumnSchema("role", "VARCHAR(32)", isNullable = false, defaultValue = "'USER'"),
                    TableColumnSchema("is_active", "BOOLEAN", isNullable = false, defaultValue = "1"),
                    TableColumnSchema("created_at", "DATETIME", isNullable = false, defaultValue = "CURRENT_TIMESTAMP")
                )
                _queryResultHeaders.value = listOf("id", "username", "email", "role", "is_active", "created_at")
                _queryResultRows.value = listOf(
                    listOf("1", "admin_ayman", "ayman@company.dev", "SUPER_ADMIN", "true", "2026-01-10"),
                    listOf("2", "sarah_k", "sarah.k@cloud.io", "DEVELOPER", "true", "2026-02-14"),
                    listOf("3", "omar_lead", "omar.lead@tech.org", "TECH_LEAD", "true", "2026-03-01"),
                    listOf("4", "noura_ui", "noura@design.ai", "DESIGNER", "true", "2026-04-12"),
                    listOf("5", "guest_tester", "guest@sandbox.app", "TESTER", "false", "2026-05-18")
                )
                _queryExecutionInfo.value = "5 rows returned in 12ms"
            }
            "products", "items" -> {
                _tableSchema.value = listOf(
                    TableColumnSchema("id", "INTEGER", isPrimaryKey = true, isNullable = false),
                    TableColumnSchema("title", "TEXT", isNullable = false),
                    TableColumnSchema("price", "REAL", isNullable = false),
                    TableColumnSchema("category", "TEXT", isNullable = true),
                    TableColumnSchema("stock_qty", "INTEGER", isNullable = false, defaultValue = "0")
                )
                _queryResultHeaders.value = listOf("id", "title", "price", "category", "stock_qty")
                _queryResultRows.value = listOf(
                    listOf("101", "Kotlin IDE License", "$49.99", "Software", "999"),
                    listOf("102", "VS Code Dark Theme Pack", "$9.99", "Themes", "150"),
                    listOf("103", "Android Dev Mechanical Keyboard", "$129.00", "Hardware", "45"),
                    listOf("104", "USB-C Dual Hub", "$34.50", "Accessories", "88")
                )
                _queryExecutionInfo.value = "4 rows returned in 8ms"
            }
            "editor_files" -> {
                _tableSchema.value = listOf(
                    TableColumnSchema("id", "INTEGER", isPrimaryKey = true, isNullable = false),
                    TableColumnSchema("name", "TEXT", isNullable = false),
                    TableColumnSchema("language", "TEXT", isNullable = false),
                    TableColumnSchema("isCurrentlyOpen", "BOOLEAN", isNullable = false),
                    TableColumnSchema("lastModified", "INTEGER", isNullable = false)
                )
                _queryResultHeaders.value = listOf("id", "name", "language", "isCurrentlyOpen", "lastModified")
                _queryResultRows.value = allFiles.value.map {
                    listOf("${it.id}", it.name, it.language, "${it.isCurrentlyOpen}", "${it.lastModified}")
                }
                _queryExecutionInfo.value = "${allFiles.value.size} rows returned from Room DB in 5ms"
            }
            else -> {
                _tableSchema.value = listOf(
                    TableColumnSchema("id", "INTEGER", isPrimaryKey = true, isNullable = false),
                    TableColumnSchema("record_name", "TEXT", isNullable = false),
                    TableColumnSchema("status", "TEXT", isNullable = true),
                    TableColumnSchema("updated_at", "DATETIME", defaultValue = "CURRENT_TIMESTAMP")
                )
                _queryResultHeaders.value = listOf("id", "record_name", "status", "updated_at")
                _queryResultRows.value = listOf(
                    listOf("1", "$tableName #01", "COMPLETED", "2026-08-15 10:00"),
                    listOf("2", "$tableName #02", "IN_PROGRESS", "2026-08-15 11:30"),
                    listOf("3", "$tableName #03", "PENDING", "2026-08-15 12:15")
                )
                _queryExecutionInfo.value = "3 rows returned in 6ms"
            }
        }
    }

    fun executeCustomSqlQuery(rawSql: String) {
        viewModelScope.launch {
            _isQueryExecuting.value = true
            addTerminalLog("[SQL] Executing query: $rawSql")
            kotlinx.coroutines.delay(250) // simulate query execution latency
            _isQueryExecuting.value = false

            val trimmed = rawSql.trim()
            if (trimmed.startsWith("SELECT", ignoreCase = true)) {
                if (trimmed.contains("FROM editor_files", ignoreCase = true)) {
                    loadTableSchemaAndSampleData("editor_files")
                } else if (trimmed.contains("FROM products", ignoreCase = true)) {
                    loadTableSchemaAndSampleData("products")
                } else {
                    loadTableSchemaAndSampleData("users")
                }
                addTerminalLog("[SQL Success] Query executed successfully. Results updated.")
                logInfo("SQL", "تم تنفيذ الاستعلام بنجاح: $trimmed")
            } else if (trimmed.startsWith("INSERT", ignoreCase = true) || trimmed.startsWith("UPDATE", ignoreCase = true) || trimmed.startsWith("DELETE", ignoreCase = true)) {
                _queryExecutionInfo.value = "Query executed successfully: 1 row affected in 9ms"
                addTerminalLog("[SQL Success] 1 row affected.")
                logInfo("SQL", "تم تطبيق التعديل على قاعدة البيانات بنجاح: $trimmed")
            } else {
                _queryExecutionInfo.value = "Query executed: Statement processed in 10ms"
            }
        }
    }

    // Debugger Breakpoints & Code Simulation Engine
    private val _breakpoints = MutableStateFlow<Set<Int>>(setOf(4, 8))
    val breakpoints: StateFlow<Set<Int>> = _breakpoints

    private val _isDebugging = MutableStateFlow(false)
    val isDebugging: StateFlow<Boolean> = _isDebugging

    private val _isSimulationPaused = MutableStateFlow(false)
    val isSimulationPaused: StateFlow<Boolean> = _isSimulationPaused

    private val _currentExecutionLine = MutableStateFlow(0)
    val currentExecutionLine: StateFlow<Int> = _currentExecutionLine

    private val _callStack = MutableStateFlow<List<StackFrame>>(emptyList())
    val callStack: StateFlow<List<StackFrame>> = _callStack

    private val _localVariables = MutableStateFlow<List<DebugVariable>>(emptyList())
    val localVariables: StateFlow<List<DebugVariable>> = _localVariables

    fun toggleBreakpoint(line: Int) {
        val current = _breakpoints.value.toMutableSet()
        if (current.contains(line)) {
            current.remove(line)
            addTerminalLog("[Debugger] Removed breakpoint at line $line")
            logDebug("Debugger", "تمت إزالة نقطة التوقف عند السطر $line")
        } else {
            current.add(line)
            addTerminalLog("[Debugger] Added breakpoint at line $line")
            logInfo("Debugger", "تمت إضافة نقطة توقف عند السطر $line")
        }
        _breakpoints.value = current
    }

    fun clearAllBreakpoints() {
        _breakpoints.value = emptySet()
        addTerminalLog("[Debugger] All breakpoints cleared.")
    }

    fun startDebugSimulation() {
        val file = _activeFile.value ?: return
        viewModelScope.launch {
            _isDebugging.value = true
            _isSimulationPaused.value = true
            val initialLine = _breakpoints.value.minOrNull() ?: 1
            _currentExecutionLine.value = initialLine

            _callStack.value = listOf(
                StackFrame(1, "main()", file.name, initialLine, "com.example.app"),
                StackFrame(2, "initializeApp()", "Application.kt", 24, "com.example.core"),
                StackFrame(3, "invoke()", "CoroutineScheduler.kt", 112, "kotlinx.coroutines")
            )

            _localVariables.value = listOf(
                DebugVariable("editorName", "String", "\"VS Code Android\""),
                DebugVariable("isActive", "Boolean", "true"),
                DebugVariable("lineIndex", "Int", "$initialLine"),
                DebugVariable("openTabsCount", "Int", "${openFiles.value.size}"),
                DebugVariable("featureList", "List<String>", "[4 items]")
            )

            addTerminalLog("[Debugger] Debug session started for ${file.name}. Paused at line $initialLine")
            logInfo("Debugger", "بدأت جلسة تصحيح الأخطاء. توقف مؤقت عند السطر $initialLine", file.name)
        }
    }

    fun stepOverSimulation() {
        if (!_isDebugging.value) return
        val currentLine = _currentExecutionLine.value
        val nextLine = currentLine + 1
        _currentExecutionLine.value = nextLine

        val file = _activeFile.value
        _callStack.value = listOf(
            StackFrame(1, "main()", file?.name ?: "Main.kt", nextLine, "com.example.app"),
            StackFrame(2, "initializeApp()", "Application.kt", 24, "com.example.core")
        )

        _localVariables.value = listOf(
            DebugVariable("editorName", "String", "\"VS Code Android\""),
            DebugVariable("isActive", "Boolean", "true"),
            DebugVariable("lineIndex", "Int", "$nextLine"),
            DebugVariable("openTabsCount", "Int", "${openFiles.value.size}"),
            DebugVariable("resultStatus", "String", "\"STEP_OK_${nextLine}\""),
            DebugVariable("featureList", "List<String>", "[4 items]")
        )

        addTerminalLog("[Debugger Step Over] -> Line $nextLine")
        logDebug("Debugger", "تم الانتقال إلى السطر $nextLine")
    }

    fun stepIntoSimulation() {
        if (!_isDebugging.value) return
        val currentLine = _currentExecutionLine.value
        _currentExecutionLine.value = currentLine + 2

        _callStack.value = listOf(
            StackFrame(1, "formatString()", "StringUtils.kt", 12, "com.example.utils"),
            StackFrame(2, "main()", _activeFile.value?.name ?: "Main.kt", currentLine, "com.example.app")
        )

        _localVariables.value = listOf(
            DebugVariable("paramString", "String", "\"Input Value\""),
            DebugVariable("formatCode", "Int", "101")
        )

        addTerminalLog("[Debugger Step Into] -> StringUtils.formatString() at line 12")
    }

    fun stopDebugSimulation() {
        _isDebugging.value = false
        _isSimulationPaused.value = false
        _currentExecutionLine.value = 0
        _callStack.value = emptyList()
        _localVariables.value = emptyList()
        addTerminalLog("[Debugger] Debug session terminated.")
        logInfo("Debugger", "تم إنهاء جلسة تصحيح الأخطاء.")
    }

    // Live Debugger & Logs State
    val debugLogs: StateFlow<List<DebugLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // GitHub State & Git Branches
    val githubProfile: StateFlow<GithubProfile?> = repository.githubProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentBranch = MutableStateFlow("main")
    val currentBranch: StateFlow<String> = _currentBranch

    private val _branches = MutableStateFlow(listOf("main", "dev", "feature/auth", "bugfix/editor-rtl"))
    val branches: StateFlow<List<String>> = _branches

    fun createBranch(name: String) {
        val sanitized = name.trim().replace(" ", "-")
        if (sanitized.isNotEmpty() && !_branches.value.contains(sanitized)) {
            _branches.value = _branches.value + sanitized
            _currentBranch.value = sanitized
            addTerminalLog("[Git] Created and checked out new branch: $sanitized")
            logInfo("Git", "تم إنشاء الفرع الجديد والتحويل إليه: $sanitized")
        }
    }

    fun switchBranch(name: String) {
        if (_branches.value.contains(name)) {
            _currentBranch.value = name
            addTerminalLog("[Git] Switched to branch: $name")
            logInfo("Git", "تم التبديل إلى الفرع: $name")
        }
    }

    fun deleteBranch(name: String) {
        if (name != "main" && name != _currentBranch.value) {
            _branches.value = _branches.value.filter { it != name }
            addTerminalLog("[Git] Deleted branch: $name")
            logWarning("Git", "تم حذف الفرع: $name")
        }
    }

    // App Version control & Updates state
    private val _currentVersion = MutableStateFlow("v1.0.0")
    val currentVersion: StateFlow<String> = _currentVersion

    private val _isCheckingUpdates = MutableStateFlow(false)
    val isCheckingUpdates: StateFlow<Boolean> = _isCheckingUpdates

    private val _updateStatus = MutableStateFlow<String>("التطبيق محدث لآخر إصدار")
    val updateStatus: StateFlow<String> = _updateStatus

    // New additions for Git Diff, Command Palette, Terminal/Console, Sidebar full-screen control, and DataStore
    val autoSaveDataStore = AutoSaveDataStore(application)

    private val _isSidebarExpanded = MutableStateFlow(true)
    val isSidebarExpanded: StateFlow<Boolean> = _isSidebarExpanded

    private val _isDiffViewActive = MutableStateFlow(false)
    val isDiffViewActive: StateFlow<Boolean> = _isDiffViewActive

    private val _isCommandPaletteOpen = MutableStateFlow(false)
    val isCommandPaletteOpen: StateFlow<Boolean> = _isCommandPaletteOpen

    private val _terminalLogs = MutableStateFlow<List<String>>(
        listOf(
            "Terminal System Online v1.0.0",
            "Welcome to the built-in Sandbox Terminal Console.",
            "Type your code and press 'Run' to execute and see logs here..."
        )
    )
    val terminalLogs: StateFlow<List<String>> = _terminalLogs

    fun setSidebarExpanded(expanded: Boolean) {
        _isSidebarExpanded.value = expanded
    }

    fun toggleSidebar() {
        _isSidebarExpanded.value = !_isSidebarExpanded.value
    }

    fun setDiffViewActive(active: Boolean) {
        _isDiffViewActive.value = active
    }

    fun setCommandPaletteOpen(open: Boolean) {
        _isCommandPaletteOpen.value = open
    }

    fun addTerminalLog(log: String) {
        _terminalLogs.value = _terminalLogs.value + log
    }

    fun clearTerminalLogs() {
        _terminalLogs.value = listOf("Terminal cleared.")
    }

    init {
        // Automatically select the first open file or create one if db is already populated
        viewModelScope.launch {
            repository.openFiles.collectLatest { openList ->
                if (openList.isNotEmpty() && _activeFile.value == null) {
                    selectFile(openList.first())
                }
            }
        }

        // Check and restore DataStore autosave backup on startup
        viewModelScope.launch {
            val lastFileId = autoSaveDataStore.lastFileId.first()
            val backupText = autoSaveDataStore.unsavedContent.first()
            if (lastFileId != null && !backupText.isNullOrBlank()) {
                allFiles.collectLatest { files ->
                    val matchedFile = files.find { it.id == lastFileId }
                    if (matchedFile != null && _activeFile.value == null) {
                        selectFile(matchedFile)
                        _editorContent.value = backupText
                        addTerminalLog("Restored autosaved backup from DataStore for file: ${matchedFile.name}")
                        logInfo("AutoSave", "تم استرداد التعديلات غير المحفوظة من الجلسة السابقة لملف ${matchedFile.name}")
                    }
                }
            }
        }

        // Background auto-save interval loop (every 5 seconds)
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000)
                val currentFile = _activeFile.value
                val currentContent = _editorContent.value
                if (currentFile != null) {
                    // Save backup to preferences DataStore
                    autoSaveDataStore.saveBackup(currentFile.id, currentContent)
                    // Save to DB as well for complete consistency
                    val updated = currentFile.copy(content = currentContent, lastModified = System.currentTimeMillis())
                    repository.updateFile(updated)
                    addTerminalLog("[AutoSave] 5s Background persist completed for ${currentFile.name}")
                    logDebug("AutoSave", "تم الحفظ التلقائي للخلفية (DataStore) لملف: ${currentFile.name}")
                }
            }
        }
    }

    // Set side panel
    fun setActivePanel(panel: String) {
        _activePanel.value = panel
    }

    // Select file in editor
    fun selectFile(file: EditorFile) {
        viewModelScope.launch {
            // Save existing active file content if needed before switching
            val currentActive = _activeFile.value
            if (currentActive != null && currentActive.id != file.id) {
                val updated = currentActive.copy(content = _editorContent.value, lastModified = System.currentTimeMillis())
                repository.updateFile(updated)
            }

            _activeFile.value = file
            _editorContent.value = file.content

            // Mark as open
            if (!file.isCurrentlyOpen) {
                repository.updateFile(file.copy(isCurrentlyOpen = true))
            }
            logInfo("System", "تم فتح الملف: ${file.name}", file.name)
        }
    }

    // Update editor content in memory
    fun updateEditorContent(content: String) {
        _editorContent.value = content
        if (settingsManager.autoSave.value) {
            _activeFile.value?.let { file ->
                saveFile(file, content)
            }
        }
    }

    // Save active file explicitly
    fun saveFile(file: EditorFile, content: String) {
        viewModelScope.launch {
            val updated = file.copy(content = content, lastModified = System.currentTimeMillis())
            repository.updateFile(updated)
            _activeFile.value = updated
            addTerminalLog("[Editor] File saved: ${file.name}")
            logInfo("Save", "تم حفظ الملف بنجاح.", file.name)
        }
    }

    // Save all open files (for Command Palette 'Save All' action)
    fun saveAllFiles() {
        viewModelScope.launch {
            val currentActive = _activeFile.value
            val currentContent = _editorContent.value
            if (currentActive != null) {
                val updated = currentActive.copy(content = currentContent, lastModified = System.currentTimeMillis())
                repository.updateFile(updated)
                _activeFile.value = updated
            }
            addTerminalLog("[Editor] All workspace files saved.")
            logInfo("Save", "تم حفظ جميع الملفات المفتوحة بنجاح (Save All).")
        }
    }


    // Create a new file
    fun createNewFile(name: String, language: String) {
        viewModelScope.launch {
            val file = EditorFile(
                name = name,
                content = "// New $language file\n",
                language = language,
                isCurrentlyOpen = true
            )
            val id = repository.insertFile(file)
            val created = file.copy(id = id.toInt())
            selectFile(created)
            logInfo("Explorer", "تم إنشاء ملف جديد: $name", name)
        }
    }

    // Close tab/file
    fun closeFile(file: EditorFile) {
        viewModelScope.launch {
            val updated = file.copy(isCurrentlyOpen = false)
            repository.updateFile(updated)

            if (_activeFile.value?.id == file.id) {
                // Find another open file
                val remaining = repository.openFiles.first()
                val nextFile = remaining.firstOrNull { it.id != file.id }
                if (nextFile != null) {
                    selectFile(nextFile)
                } else {
                    _activeFile.value = null
                    _editorContent.value = ""
                }
            }
        }
    }

    // Close all other tabs except the current one
    fun closeOtherFiles(keepFile: EditorFile) {
        viewModelScope.launch {
            val openList = repository.openFiles.first()
            openList.forEach { f ->
                if (f.id != keepFile.id) {
                    repository.updateFile(f.copy(isCurrentlyOpen = false))
                }
            }
            selectFile(keepFile)
            logInfo("Tabs", "تم إغلاق كافة التبويبات الأخرى والإبقاء على: ${keepFile.name}")
        }
    }

    // Close all tabs
    fun closeAllFiles() {
        viewModelScope.launch {
            val openList = repository.openFiles.first()
            openList.forEach { f ->
                repository.updateFile(f.copy(isCurrentlyOpen = false))
            }
            _activeFile.value = null
            _editorContent.value = ""
            logInfo("Tabs", "تم إغلاق كافة ملفات العمل المفتوحة.")
        }
    }

    // Delete file completely
    fun deleteFile(file: EditorFile) {
        viewModelScope.launch {
            if (_activeFile.value?.id == file.id) {
                _activeFile.value = null
                _editorContent.value = ""
            }
            repository.deleteFile(file)
            logWarning("Explorer", "تم حذف الملف: ${file.name}", file.name)
        }
    }

    // Run Code simulation
    fun runActiveCode() {
        val file = _activeFile.value ?: return
        viewModelScope.launch {
            addTerminalLog("\n$ > run ${file.name}")
            addTerminalLog("[Runner] Compilation started for language: ${file.language}...")
            logInfo("Runner", "بدء تشغيل الملف: ${file.name}...", file.name)

            kotlinx.coroutines.delay(600)

            val hasSyntaxError = _editorContent.value.contains("err") || _editorContent.value.contains("خطأ")
            if (hasSyntaxError) {
                addTerminalLog("[Compiler ERROR] Syntax Error in ${file.name} at line 4:")
                addTerminalLog("    -> Unresolved reference or unexpected token")
                addTerminalLog("[Compiler ERROR] Process terminated with exit code 1.")
                logError("Compiler", "خطأ بريدي: Syntax Error at line 4.", file.name)
            } else {
                addTerminalLog("[Runner SUCCESS] Compiled successfully in 120ms.")
                addTerminalLog("Output logs:")
                if (file.language.lowercase() == "sql") {
                    addTerminalLog("  | id | name       | role      |")
                    addTerminalLog("  |----+------------+-----------|")
                    addTerminalLog("  |  1 | Admin      | Developer |")
                    addTerminalLog("  |  2 | Guest User | Viewer    |")
                    addTerminalLog("  (2 rows selected)")
                } else {
                    addTerminalLog("  Hello, VS Code Android IDE!")
                    addTerminalLog("  Execution finished with exit code 0.")
                }
                logInfo("Runner", "تم التحميل بنجاح في 120ms.", file.name)
                logDebug("Console", "مخرجات الكود: [تم التنفيذ بنجاح].", file.name)
            }
        }
    }

    // Search code
    fun updateCodeSearchQuery(query: String) {
        _codeSearchQuery.value = query
    }

    // Database Actions
    fun addNewDbConnection(conn: DbConnection) {
        viewModelScope.launch {
            repository.insertConnection(conn)
            logInfo("Database", "تمت إضافة اتصال قاعدة بيانات جديد: ${conn.name}")
        }
    }

    fun addDbConnection(name: String, type: String, host: String, dbName: String, user: String, tables: String) {
        viewModelScope.launch {
            val conn = DbConnection(
                name = name,
                type = type,
                host = host,
                databaseName = dbName,
                username = user,
                tables = tables
            )
            repository.insertConnection(conn)
            logInfo("Database", "تمت إضافة اتصال قاعدة بيانات جديد: $name")
        }
    }

    fun openFile(file: EditorFile) {
        selectFile(file)
    }

    fun clearDebugLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
            logInfo("Debugger", "تم مسح كافة سجلات مصحح الأخطاء.")
        }
    }

    fun deleteDbConnection(connection: DbConnection) {
        viewModelScope.launch {
            repository.deleteConnection(connection)
            if (_selectedConnection.value?.id == connection.id) {
                _selectedConnection.value = null
            }
            logWarning("Database", "تم حذف اتصال قاعدة البيانات: ${connection.name}")
        }
    }

    fun selectConnection(connection: DbConnection) {
        _selectedConnection.value = connection
        logInfo("Database", "تم استعراض جداول الاتصال: ${connection.name}")
    }

    // Extensions Actions
    fun toggleExtensionInstallation(ext: AppExtension) {
        viewModelScope.launch {
            val updated = ext.copy(isInstalled = !ext.isInstalled)
            repository.updateExtension(updated)
            if (updated.isInstalled) {
                logInfo("Extensions", "تم تثبيت الإضافة: ${ext.name}")
            } else {
                logWarning("Extensions", "تم إلغاء تثبيت الإضافة: ${ext.name}")
            }
        }
    }

    fun updateExtensionSearchQuery(query: String) {
        _extensionSearchQuery.value = query
    }

    // GitHub Actions
    fun saveGithubProfile(username: String, token: String, repo: String) {
        viewModelScope.launch {
            val profile = GithubProfile(
                username = username,
                token = token,
                repoName = repo,
                isSynced = true,
                lastSyncTime = System.currentTimeMillis()
            )
            repository.insertGithubProfile(profile)
            logInfo("GitHub", "تم ربط مستودع GitHub بنجاح: $repo")
        }
    }

    fun disconnectGithub() {
        viewModelScope.launch {
            repository.deleteGithubProfile()
            logWarning("GitHub", "تم إلغاء ربط مستودع GitHub.")
        }
    }

    fun syncGithubProject() {
        viewModelScope.launch {
            githubProfile.value?.let { profile ->
                logInfo("GitHub", "بدء مزامنة الملفات مع ${profile.repoName}...")
                repository.insertGithubProfile(profile.copy(lastSyncTime = System.currentTimeMillis()))
                logInfo("GitHub", "تم رفع التعديلات (Push) وجلب التغييرات (Pull) بنجاح!")
            }
        }
    }

    // Update check Actions
    fun checkAppUpdates() {
        viewModelScope.launch {
            _isCheckingUpdates.value = true
            _updateStatus.value = "جاري التحقق من التحديثات..."
            kotlinx.coroutines.delay(1500)
            _isCheckingUpdates.value = false
            _updateStatus.value = "التطبيق محدث بالكامل (الإصدار الحالي: ${_currentVersion.value})"
            logInfo("Updates", "تم التحقق من تحديثات النظام.")
        }
    }

    // Clear everything
    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllLogs()
            logInfo("System", "تم مسح كافة البيانات والإعدادات.")
        }
    }

    // Helpers to write logs
    fun logInfo(tag: String, msg: String, file: String = "") {
        viewModelScope.launch {
            repository.insertLog(DebugLog(level = "INFO", tag = tag, message = msg, fileName = file))
        }
    }

    fun logDebug(tag: String, msg: String, file: String = "") {
        viewModelScope.launch {
            repository.insertLog(DebugLog(level = "DEBUG", tag = tag, message = msg, fileName = file))
        }
    }

    fun logWarning(tag: String, msg: String, file: String = "") {
        viewModelScope.launch {
            repository.insertLog(DebugLog(level = "WARNING", tag = tag, message = msg, fileName = file))
        }
    }

    fun logError(tag: String, msg: String, file: String = "") {
        viewModelScope.launch {
            repository.insertLog(DebugLog(level = "ERROR", tag = tag, message = msg, fileName = file))
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
        }
    }
}
