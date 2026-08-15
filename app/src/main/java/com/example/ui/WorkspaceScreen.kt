package com.example.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EditorFile
import com.example.ui.components.CodeMiniMap
import com.example.ui.panels.*
import com.example.utils.Locales
import com.example.utils.SyntaxHighlighter
import com.example.viewmodels.CodeEditorViewModel
import kotlinx.coroutines.launch

/**
 * Custom Visual Transformation for real-time syntax coloring using regex tokenization.
 */
class SyntaxHighlightingTransformation(
    private val language: String,
    private val keywordColor: Color,
    private val stringColor: Color,
    private val commentColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = SyntaxHighlighter.highlight(
            code = text.text,
            language = language,
            customKeywordColor = keywordColor,
            customStringColor = stringColor,
            customCommentColor = commentColor
        )
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

@Composable
fun WorkspaceScreen(
    viewModel: CodeEditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    val activePanel by viewModel.activePanel.collectAsState()
    val openFiles by viewModel.openFiles.collectAsState()
    val activeFile by viewModel.activeFile.collectAsState()
    val editorContent by viewModel.editorContent.collectAsState()
    val currentFontSize by viewModel.settingsManager.fontSize.collectAsState()
    val currentLanguage by viewModel.settingsManager.language.collectAsState()
    val isMiniMapEnabled by viewModel.settingsManager.isMiniMapEnabled.collectAsState()

    // Syntax colors from settings
    val keywordColorHex by viewModel.settingsManager.keywordColor.collectAsState()
    val stringColorHex by viewModel.settingsManager.stringColor.collectAsState()
    val commentColorHex by viewModel.settingsManager.commentColor.collectAsState()

    val keywordColor = remember(keywordColorHex) { parseHexColor(keywordColorHex, SyntaxHighlighter.DEFAULT_KEYWORD_COLOR) }
    val stringColor = remember(stringColorHex) { parseHexColor(stringColorHex, SyntaxHighlighter.DEFAULT_STRING_COLOR) }
    val commentColor = remember(commentColorHex) { parseHexColor(commentColorHex, SyntaxHighlighter.DEFAULT_COMMENT_COLOR) }

    // Observe state variables from ViewModel
    val isSidebarExpanded by viewModel.isSidebarExpanded.collectAsState()
    val sidebarWidthDp by viewModel.sidebarWidthDp.collectAsState()
    val isDiffViewActive by viewModel.isDiffViewActive.collectAsState()
    val isCommandPaletteOpen by viewModel.isCommandPaletteOpen.collectAsState()
    val terminalLogs by viewModel.terminalLogs.collectAsState()
    val isFindInFileOpen by viewModel.isFindInFileOpen.collectAsState()
    val findInFileQuery by viewModel.findInFileQuery.collectAsState()

    // Local toggles & state
    var isTerminalVisible by remember { mutableStateOf(false) }
    var isDividerDragging by remember { mutableStateOf(false) }
    val editorScrollState = rememberScrollState()

    // Debugger and Workspace states from ViewModel
    val breakpoints by viewModel.breakpoints.collectAsState()
    val currentExecutionLine by viewModel.currentExecutionLine.collectAsState()
    val isDebugging by viewModel.isDebugging.collectAsState()
    val autoSaveEnabled by viewModel.settingsManager.autoSave.collectAsState()
    val allFiles by viewModel.allFiles.collectAsState()

    // Focus requester for global keyboard events
    val rootFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        rootFocusRequester.requestFocus()
    }

    // Back press twice to exit logic
    var lastBackPressTime by remember { mutableStateOf(0L) }
    BackHandler {
        if (isSidebarExpanded && activePanel != "files") {
            viewModel.setActivePanel("files")
        } else if (isSidebarExpanded) {
            viewModel.setSidebarExpanded(false)
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, Locales.get("press_back_again", currentLanguage), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Modal dialog trigger states
    var showCreateDialogByPalette by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newFileLang by remember { mutableStateOf("kotlin") }

    // Helper to handle shortcut actions
    fun handleSaveShortcut() {
        activeFile?.let { file ->
            viewModel.saveFile(file, editorContent)
            Toast.makeText(context, "تم حفظ الملف: ${file.name} (Ctrl+S)", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleSaveAllShortcut() {
        viewModel.saveAllFiles()
        Toast.makeText(context, "تم حفظ جميع الملفات المفتوحة بنجاح (Save All)", Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(rootFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                // Global keyboard event handler for standard IDE shortcuts
                if (keyEvent.type == KeyEventType.KeyDown) {
                    val isCtrl = keyEvent.isCtrlPressed || keyEvent.isMetaPressed
                    val isShift = keyEvent.isShiftPressed

                    when {
                        // Ctrl + Shift + S -> Save All
                        isCtrl && isShift && keyEvent.key == Key.S -> {
                            handleSaveAllShortcut()
                            true
                        }

                        // Ctrl + S -> Save active file
                        isCtrl && !isShift && keyEvent.key == Key.S -> {
                            handleSaveShortcut()
                            true
                        }

                        // Ctrl + P -> Open Command Palette
                        isCtrl && !isShift && keyEvent.key == Key.P -> {
                            viewModel.setCommandPaletteOpen(!isCommandPaletteOpen)
                            true
                        }

                        // Ctrl + Shift + P -> Open Command Palette
                        isCtrl && isShift && keyEvent.key == Key.P -> {
                            viewModel.setCommandPaletteOpen(true)
                            true
                        }

                        // Ctrl + F -> In-Editor Find
                        isCtrl && !isShift && keyEvent.key == Key.F -> {
                            viewModel.setFindInFileOpen(!isFindInFileOpen)
                            true
                        }

                        // Ctrl + Shift + F -> Global Workspace Search
                        isCtrl && isShift && keyEvent.key == Key.F -> {
                            viewModel.setActivePanel("search")
                            viewModel.setSidebarExpanded(true)
                            true
                        }

                        // Ctrl + B -> Toggle Sidebar
                        isCtrl && !isShift && keyEvent.key == Key.B -> {
                            viewModel.toggleSidebar()
                            true
                        }

                        // Ctrl + N -> New File
                        isCtrl && !isShift && keyEvent.key == Key.N -> {
                            showCreateDialogByPalette = true
                            true
                        }

                        // Ctrl + W -> Close Current Tab
                        isCtrl && !isShift && keyEvent.key == Key.W -> {
                            activeFile?.let { viewModel.closeFile(it) }
                            true
                        }

                        // Ctrl + ` or Ctrl + J -> Toggle Terminal
                        isCtrl && (keyEvent.key == Key.Grave || keyEvent.key == Key.J) -> {
                            isTerminalVisible = !isTerminalVisible
                            true
                        }

                        // Ctrl + Enter or F5 -> Run Active Code
                        (isCtrl && keyEvent.key == Key.Enter) || keyEvent.key == Key.F5 -> {
                            viewModel.runActiveCode()
                            isTerminalVisible = true
                            true
                        }

                        // Escape -> Close find bar or palette
                        keyEvent.key == Key.Escape -> {
                            if (isFindInFileOpen) {
                                viewModel.setFindInFileOpen(false)
                                true
                            } else if (isCommandPaletteOpen) {
                                viewModel.setCommandPaletteOpen(false)
                                true
                            } else false
                        }

                        else -> false
                    }
                } else false
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. VS Code Side Activity Bar (Fixed left menu: 62.dp)
            ActivityBar(
                activePanel = activePanel,
                isSidebarExpanded = isSidebarExpanded,
                onPanelSelect = { panel ->
                    if (panel == "editor") {
                        viewModel.setSidebarExpanded(false)
                    } else {
                        if (activePanel == panel) {
                            viewModel.setSidebarExpanded(!isSidebarExpanded)
                        } else {
                            viewModel.setActivePanel(panel)
                            viewModel.setSidebarExpanded(true)
                        }
                    }
                },
                lang = currentLanguage
            )

            VerticalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))

            // 2. VS Code Secondary Sidebar with Draggable Divider
            if (isSidebarExpanded) {
                Column(
                    modifier = Modifier
                        .width(sidebarWidthDp.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.secondary)
                ) {
                    // Header with back to editor button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VS Code Workspace",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f)
                        )
                        IconButton(
                            onClick = { viewModel.setSidebarExpanded(false) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Collapse Sidebar",
                                tint = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

                    Box(modifier = Modifier.weight(1f)) {
                        when (activePanel) {
                            "files" -> ExplorerPanel(viewModel = viewModel, lang = currentLanguage)
                            "search" -> SearchPanel(viewModel = viewModel, lang = currentLanguage)
                            "extensions" -> ExtensionsPanel(viewModel = viewModel, lang = currentLanguage)
                            "github" -> GithubPanel(viewModel = viewModel, lang = currentLanguage)
                            "db" -> DatabasePanel(viewModel = viewModel, lang = currentLanguage)
                            "debug" -> DebuggerPanel(viewModel = viewModel, lang = currentLanguage)
                            "updates" -> UpdatesPanel(viewModel = viewModel, lang = currentLanguage)
                            "settings" -> SettingsPanel(viewModel = viewModel, lang = currentLanguage)
                            else -> ExplorerPanel(viewModel = viewModel, lang = currentLanguage)
                        }
                    }
                }

                // Draggable Divider between Sidebar and Editor Area
                DraggableSplitDivider(
                    isDragging = isDividerDragging,
                    onDrag = { deltaPx ->
                        val deltaDp = with(density) { deltaPx.toDp().value }
                        val adjustedDelta = if (isRtl) -deltaDp else deltaDp
                        viewModel.setSidebarWidth(sidebarWidthDp + adjustedDelta)
                    },
                    onDragStart = { isDividerDragging = true },
                    onDragEnd = { isDividerDragging = false },
                    onDoubleClick = { viewModel.setSidebarWidth(280f) }
                )
            }

            // 3. Central Editor Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Multi-tab System Row
                MultiTabRow(
                    openFiles = openFiles,
                    activeFile = activeFile,
                    editorContent = editorContent,
                    onSelect = { viewModel.selectFile(it) },
                    onClose = { viewModel.closeFile(it) },
                    onCloseOthers = { viewModel.closeOtherFiles(it) },
                    onCloseAll = { viewModel.closeAllFiles() },
                    onNewTab = { showCreateDialogByPalette = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

                // Editor Controls Bar
                EditorControls(
                    activeFile = activeFile,
                    isDiffViewActive = isDiffViewActive,
                    isTerminalVisible = isTerminalVisible,
                    isFindInFileOpen = isFindInFileOpen,
                    isMiniMapEnabled = isMiniMapEnabled,
                    autoSaveEnabled = autoSaveEnabled,
                    onToggleAutoSave = { viewModel.settingsManager.setAutoSave(!autoSaveEnabled) },
                    onRun = {
                        viewModel.runActiveCode()
                        isTerminalVisible = true
                    },
                    onSave = { file -> viewModel.saveFile(file, editorContent) },
                    onToggleDiff = { viewModel.setDiffViewActive(!isDiffViewActive) },
                    onToggleTerminal = { isTerminalVisible = !isTerminalVisible },
                    onToggleFind = { viewModel.setFindInFileOpen(!isFindInFileOpen) },
                    onToggleMiniMap = { viewModel.settingsManager.setMiniMapEnabled(!isMiniMapEnabled) },
                    onTriggerPalette = { viewModel.setCommandPaletteOpen(true) },
                    lang = currentLanguage
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))

                // In-Editor Find & Replace Bar (when Ctrl+F is pressed)
                AnimatedVisibility(visible = isFindInFileOpen) {
                    InEditorFindBar(
                        query = findInFileQuery,
                        content = editorContent,
                        onQueryChange = { viewModel.setFindInFileQuery(it) },
                        onClose = { viewModel.setFindInFileOpen(false) }
                    )
                }

                // Editor Body (Text Area + Code Mini-map + Diff View)
                Box(modifier = Modifier.weight(1f)) {
                    if (activeFile != null) {
                        if (isDiffViewActive) {
                            SideBySideDiffView(
                                originalContent = activeFile!!.content,
                                currentContent = editorContent,
                                fontSize = currentFontSize,
                                lang = currentLanguage
                            )
                        } else {
                            Row(modifier = Modifier.fillMaxSize()) {
                                // Code Text Area
                                Box(modifier = Modifier.weight(1f)) {
                                    CodeTextEditor(
                                        content = editorContent,
                                        onValueChange = { viewModel.updateEditorContent(it) },
                                        language = activeFile!!.language,
                                        fontSize = currentFontSize,
                                        placeholder = Locales.get("editor_placeholder", currentLanguage),
                                        scrollState = editorScrollState,
                                        keywordColor = keywordColor,
                                        stringColor = stringColor,
                                        commentColor = commentColor,
                                        searchHighlight = if (isFindInFileOpen) findInFileQuery else "",
                                        breakpoints = breakpoints,
                                        currentExecutionLine = currentExecutionLine,
                                        isDebugging = isDebugging,
                                        onToggleBreakpoint = { viewModel.toggleBreakpoint(it) }
                                    )
                                }

                                // Code Mini-map on the right
                                if (isMiniMapEnabled) {
                                    VerticalDivider(color = Color(0xFF2A2A2A))
                                    CodeMiniMap(
                                        content = editorContent,
                                        scrollState = editorScrollState,
                                        coroutineScope = coroutineScope,
                                        keywordColor = keywordColor,
                                        stringColor = stringColor,
                                        commentColor = commentColor
                                    )
                                }
                            }
                        }
                    } else {
                        // Empty workspace placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = Locales.get("no_open_files", currentLanguage),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    lineHeight = 22.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        viewModel.setSidebarExpanded(true)
                                        viewModel.setActivePanel("files")
                                    }
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("افتح مستعرض الملفات (Ctrl+B)")
                                }
                            }
                        }
                    }
                }

                // Terminal Console Output Pane
                if (isTerminalVisible) {
                    TerminalPane(
                        logs = terminalLogs,
                        onClear = { viewModel.clearTerminalLogs() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Floating Command Palette trigger button
        FloatingActionButton(
            onClick = { viewModel.setCommandPaletteOpen(true) },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(46.dp)
                .testTag("floating_palette_fab")
        ) {
            Icon(Icons.Default.Terminal, contentDescription = "Command Palette (Ctrl+P)")
        }

        // Command Palette Dialog
        CommandPalette(
            isOpen = isCommandPaletteOpen,
            workspaceFiles = allFiles,
            onSelectFile = { file -> viewModel.openFile(file) },
            onDismiss = { viewModel.setCommandPaletteOpen(false) },
            onExecute = { cmdId ->
                when (cmdId) {
                    "save" -> handleSaveShortcut()
                    "save_all" -> handleSaveAllShortcut()
                    "new_file" -> showCreateDialogByPalette = true
                    "toggle_terminal" -> isTerminalVisible = !isTerminalVisible
                    "close_all_tabs" -> {
                        viewModel.closeAllFiles()
                        Toast.makeText(context, "تم إغلاق كافة الملفات المفتوحة", Toast.LENGTH_SHORT).show()
                    }
                    "close_other_tabs" -> {
                        activeFile?.let {
                            viewModel.closeOtherFiles(it)
                            Toast.makeText(context, "تم إغلاق الملفات الأخرى", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "run" -> {
                        viewModel.runActiveCode()
                        isTerminalVisible = true
                    }
                    "find" -> viewModel.setFindInFileOpen(true)
                    "global_search" -> {
                        viewModel.setActivePanel("search")
                        viewModel.setSidebarExpanded(true)
                    }
                    "clear_console" -> viewModel.clearTerminalLogs()
                    "toggle_diff" -> viewModel.setDiffViewActive(!isDiffViewActive)
                    "toggle_sidebar" -> viewModel.toggleSidebar()
                    "toggle_minimap" -> viewModel.settingsManager.setMiniMapEnabled(!isMiniMapEnabled)
                    "git_init" -> {
                        viewModel.initGitRepo()
                        viewModel.setActivePanel("github")
                        viewModel.setSidebarExpanded(true)
                    }
                    "git_commit" -> {
                        viewModel.setActivePanel("github")
                        viewModel.setSidebarExpanded(true)
                    }
                    "git_log" -> {
                        viewModel.setActivePanel("github")
                        viewModel.setSidebarExpanded(true)
                    }
                    "db_sqlite" -> {
                        viewModel.setActivePanel("db")
                        viewModel.setSidebarExpanded(true)
                    }
                    "debug_start" -> {
                        viewModel.startDebugSimulation()
                        viewModel.setActivePanel("debug")
                        viewModel.setSidebarExpanded(true)
                        isTerminalVisible = true
                    }
                    "debug_step_over" -> viewModel.stepOverSimulation()
                    "debug_stop" -> viewModel.stopDebugSimulation()
                    "toggle_autosave" -> {
                        val current = viewModel.settingsManager.autoSave.value
                        viewModel.settingsManager.setAutoSave(!current)
                        Toast.makeText(context, if (!current) "تم تفعيل الحفظ التلقائي" else "تم تعطيل الحفظ التلقائي", Toast.LENGTH_SHORT).show()
                    }
                    "toggle_theme" -> {
                        val current = viewModel.settingsManager.theme.value
                        viewModel.settingsManager.setTheme(if (current == "dark") "light" else "dark")
                    }
                    "switch_lang" -> {
                        val current = viewModel.settingsManager.language.value
                        viewModel.settingsManager.setLanguage(if (current == "ar") "en" else "ar")
                    }
                    "open_settings" -> {
                        viewModel.setActivePanel("settings")
                        viewModel.setSidebarExpanded(true)
                    }
                    "clear_data" -> viewModel.clearAllData()
                    "sync_github" -> viewModel.syncGithubProject()
                }
            },
            lang = currentLanguage
        )

        // New File Creation Dialog
        if (showCreateDialogByPalette) {
            AlertDialog(
                onDismissRequest = { showCreateDialogByPalette = false },
                title = { Text(text = Locales.get("new_file", currentLanguage)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newFileName,
                            onValueChange = { newFileName = it },
                            placeholder = { Text("مثال: Main.kt أو index.js") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("palette_new_file_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "اختر اللغة البرمجية:", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("kotlin", "javascript", "python", "sql", "html", "css").forEach { l ->
                                FilterChip(
                                    selected = newFileLang == l,
                                    onClick = { newFileLang = l },
                                    label = { Text(l) }
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
                                showCreateDialogByPalette = false
                                newFileName = ""
                            }
                        }
                    ) {
                        Text(text = Locales.get("create_btn", currentLanguage))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialogByPalette = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

/**
 * Draggable divider between sidebar and editor area using Modifier.draggable.
 */
@Composable
fun DraggableSplitDivider(
    isDragging: Boolean,
    onDrag: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDoubleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dividerColor = if (isDragging) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
    }

    Box(
        modifier = modifier
            .width(14.dp)
            .fillMaxHeight()
            .draggable(
                state = rememberDraggableState { delta -> onDrag(delta) },
                orientation = Orientation.Horizontal,
                onDragStarted = { onDragStart() },
                onDragStopped = { onDragEnd() }
            )
            .clickable { onDoubleClick() }
            .testTag("workspace_draggable_divider"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle central line
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(dividerColor)
        )

        // Visual grabber handle in the center
        Surface(
            color = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.extraSmall,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
            modifier = Modifier
                .size(width = 6.dp, height = 36.dp)
        ) {}
    }
}

/**
 * Multi-tab System supporting simultaneous open files, dirty state indicator,
 * tab switching, context menus, and new tab creation.
 */
@Composable
fun MultiTabRow(
    openFiles: List<EditorFile>,
    activeFile: EditorFile?,
    editorContent: String,
    onSelect: (EditorFile) -> Unit,
    onClose: (EditorFile) -> Unit,
    onCloseOthers: (EditorFile) -> Unit,
    onCloseAll: () -> Unit,
    onNewTab: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(Color(0xFF1E1E1E))
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        openFiles.forEach { file ->
            val isActive = activeFile?.id == file.id
            val isModified = isActive && file.content != editorContent

            var showMenu by remember { mutableStateOf(false) }

            Box {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(if (isActive) Color(0xFF252526) else Color(0xFF1E1E1E))
                        .clickable { onSelect(file) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Language Icon
                    Icon(
                        imageVector = when (file.language.lowercase()) {
                            "kotlin", "kt", "kts" -> Icons.Default.Code
                            "javascript", "js", "ts", "typescript" -> Icons.Default.Javascript
                            "python", "py" -> Icons.Default.Terminal
                            "css", "scss" -> Icons.Default.Css
                            "sql" -> Icons.Default.Storage
                            "html", "xml" -> Icons.Default.Language
                            else -> Icons.Default.Article
                        },
                        contentDescription = null,
                        tint = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF969696),
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // File Name
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) Color.White else Color(0xFF969696)
                    )

                    // Dirty / Unsaved dot indicator
                    if (isModified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Close Tab Button
                    IconButton(
                        onClick = { onClose(file) },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isActive) Color.White.copy(alpha = 0.7f) else Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Active Tab bottom highlight line
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.primary)
                            .align(Alignment.BottomCenter)
                    )
                }

                // Dropdown context menu
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("إغلاق هذا التبويب") },
                        onClick = {
                            showMenu = false
                            onClose(file)
                        },
                        leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("إغلاق التبويبات الأخرى") },
                        onClick = {
                            showMenu = false
                            onCloseOthers(file)
                        },
                        leadingIcon = { Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("إغلاق الكل") },
                        onClick = {
                            showMenu = false
                            onCloseAll()
                        },
                        leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            VerticalDivider(color = Color(0xFF2B2B2B), modifier = Modifier.height(24.dp))
        }

        // New Tab '+' button
        IconButton(
            onClick = onNewTab,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(28.dp)
                .testTag("tab_new_file_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Tab",
                tint = Color(0xFF969696),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * In-editor quick find bar (triggered by Ctrl+F).
 */
@Composable
fun InEditorFindBar(
    query: String,
    content: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val matchCount = remember(query, content) {
        if (query.length < 2) 0
        else {
            var count = 0
            var idx = 0
            while (idx != -1) {
                idx = content.indexOf(query, idx, ignoreCase = true)
                if (idx != -1) {
                    count++
                    idx += query.length
                }
            }
            count
        }
    }

    Surface(
        color = Color(0xFF252526),
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .border(1.dp, Color(0xFF3C3C3C), shape = MaterialTheme.shapes.extraSmall)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(Color(0xFF007ACC)),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("in_editor_find_input")
                )
                if (query.isNotEmpty()) {
                    Text(
                        text = if (matchCount > 0) "$matchCount نتيجة" else "لا توجد نتائج",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (matchCount > 0) MaterialTheme.colorScheme.primary else Color.Red,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Find",
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun EditorControls(
    activeFile: EditorFile?,
    isDiffViewActive: Boolean,
    isTerminalVisible: Boolean,
    isFindInFileOpen: Boolean,
    isMiniMapEnabled: Boolean,
    autoSaveEnabled: Boolean,
    onToggleAutoSave: () -> Unit,
    onRun: () -> Unit,
    onSave: (EditorFile) -> Unit,
    onToggleDiff: () -> Unit,
    onToggleTerminal: () -> Unit,
    onToggleFind: () -> Unit,
    onToggleMiniMap: () -> Unit,
    onTriggerPalette: () -> Unit,
    lang: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = activeFile?.name ?: "محرر الأكواد",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Auto-Save Status Badge
            Surface(
                color = if (autoSaveEnabled) Color(0xFF4CAF50).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier
                    .clickable { onToggleAutoSave() }
                    .testTag("auto_save_toggle_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (autoSaveEnabled) Color(0xFF4CAF50) else Color.Gray,
                                shape = MaterialTheme.shapes.extraSmall
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (autoSaveEnabled) "Auto-Save: ON" else "Auto-Save: OFF",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (autoSaveEnabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Find in File button (Ctrl+F)
            IconButton(
                onClick = onToggleFind,
                modifier = Modifier.size(30.dp).testTag("trigger_find_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Find (Ctrl+F)",
                    tint = if (isFindInFileOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Mini-map toggle button
            IconButton(
                onClick = onToggleMiniMap,
                modifier = Modifier.size(30.dp).testTag("toggle_minimap_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Toggle Mini-map",
                    tint = if (isMiniMapEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Command Palette quick trigger button (Ctrl+P)
            IconButton(
                onClick = onTriggerPalette,
                modifier = Modifier.size(30.dp).testTag("trigger_palette_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Palette (Ctrl+P)",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (activeFile != null) {
                // Git Diff Side-by-side View toggle
                IconButton(
                    onClick = onToggleDiff,
                    modifier = Modifier.size(30.dp).testTag("toggle_diff_view_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Compare,
                        contentDescription = "Toggle Diff",
                        tint = if (isDiffViewActive) Color.Green else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Terminal panel visibility toggle
                IconButton(
                    onClick = onToggleTerminal,
                    modifier = Modifier.size(30.dp).testTag("toggle_terminal_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Wysiwyg,
                        contentDescription = "Toggle Terminal",
                        tint = if (isTerminalVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Save button (Ctrl+S)
                IconButton(
                    onClick = { onSave(activeFile) },
                    modifier = Modifier.size(30.dp).testTag("save_file_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = Locales.get("save", lang),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Run Code Button
                Button(
                    onClick = onRun,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("run_code_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = Locales.get("run", lang),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = Locales.get("run", lang),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * Text Editor area with line numbering, dynamic regex syntax highlighting,
 * breakpoint toggles, active execution highlighting, and search match highlighting.
 */
@Composable
fun CodeTextEditor(
    content: String,
    onValueChange: (String) -> Unit,
    language: String,
    fontSize: Float,
    placeholder: String,
    scrollState: ScrollState,
    keywordColor: Color,
    stringColor: Color,
    commentColor: Color,
    searchHighlight: String = "",
    breakpoints: Set<Int> = emptySet(),
    currentExecutionLine: Int = -1,
    isDebugging: Boolean = false,
    onToggleBreakpoint: (Int) -> Unit = {}
) {
    val lines = remember(content) { content.split("\n") }
    val lineCount = lines.size.coerceAtLeast(1)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFF1E1E1E))
    ) {
        // Line Numbers & Breakpoints Gutter Column
        Column(
            modifier = Modifier
                .width(52.dp)
                .background(Color(0xFF1E1E1E))
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.End
        ) {
            for (i in 1..lineCount) {
                val hasBreakpoint = breakpoints.contains(i)
                val isCurrentLine = isDebugging && currentExecutionLine == i

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleBreakpoint(i) }
                        .padding(end = 6.dp, bottom = 1.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCurrentLine) {
                        Icon(
                            imageVector = Icons.Default.ArrowRight,
                            contentDescription = "Current Execution Line",
                            tint = Color(0xFFFFEB3B),
                            modifier = Modifier.size(12.dp)
                        )
                    } else if (hasBreakpoint) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFF44336), shape = MaterialTheme.shapes.extraSmall)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$i",
                        style = TextStyle(
                            color = if (isCurrentLine) Color(0xFFFFEB3B) else if (hasBreakpoint) Color(0xFFF44336) else Color(0xFF858585),
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSize.sp,
                            fontWeight = if (isCurrentLine || hasBreakpoint) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }

        VerticalDivider(color = Color(0xFF333333))

        // Interactive Editor TextField
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp, horizontal = 10.dp)
        ) {
            if (content.isEmpty()) {
                Text(
                    text = placeholder,
                    style = TextStyle(
                        color = Color(0xFF5A5A5A),
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize.sp
                    )
                )
            }

            BasicTextField(
                value = content,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = Color(0xFFD4D4D4),
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize.sp,
                    lineHeight = fontSize.sp * 1.3f
                ),
                cursorBrush = SolidColor(Color(0xFF007ACC)),
                visualTransformation = remember(language, keywordColor, stringColor, commentColor) {
                    SyntaxHighlightingTransformation(
                        language = language,
                        keywordColor = keywordColor,
                        stringColor = stringColor,
                        commentColor = commentColor
                    )
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("code_editor_textarea")
            )
        }
    }
}

@Composable
fun SideBySideDiffView(
    originalContent: String,
    currentContent: String,
    fontSize: Float,
    lang: String
) {
    val originalLines = originalContent.lines()
    val currentLines = currentContent.lines()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF151515))
    ) {
        // Original version (Left Column)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(1.dp, Color(0xFF2B2B2B))
                .verticalScroll(rememberScrollState())
                .padding(6.dp)
        ) {
            Text(
                text = "النسخة السابقة (Original / -)",
                color = Color.Red.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            originalLines.forEachIndexed { index, line ->
                val isDeleted = !currentLines.contains(line)
                val bgColor = if (isDeleted) Color(0xFF4A1515) else Color.Transparent
                val textColor = if (isDeleted) Color(0xFFFF8888) else Color(0xFFBBBBBB)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .padding(vertical = 1.dp)
                ) {
                    Text(
                        text = "${index + 1} - ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.width(32.dp)
                    )
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize.sp,
                        color = textColor
                    )
                }
            }
        }

        VerticalDivider(color = Color(0xFF333333))

        // Current version (Right Column)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(1.dp, Color(0xFF2B2B2B))
                .verticalScroll(rememberScrollState())
                .padding(6.dp)
        ) {
            Text(
                text = "النسخة الحالية (Modified / +)",
                color = Color.Green.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            currentLines.forEachIndexed { index, line ->
                val isAdded = !originalLines.contains(line)
                val bgColor = if (isAdded) Color(0xFF154A15) else Color.Transparent
                val textColor = if (isAdded) Color(0xFF88FF88) else Color(0xFFBBBBBB)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .padding(vertical = 1.dp)
                ) {
                    Text(
                        text = "${index + 1} + ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.width(32.dp)
                    )
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize.sp,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun TerminalPane(
    logs: List<String>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var commandInput by remember { mutableStateOf("") }
    var mockOutputs = remember { mutableStateListOf<String>() }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Color(0xFF0F0F0F))
            .border(width = 1.dp, color = Color(0xFF222222))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = Color.Green,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Terminal / منفذ الأوامر",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(
                onClick = {
                    onClear()
                    mockOutputs.clear()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear",
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = Color(0xFF222222))
        Spacer(modifier = Modifier.height(4.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            logs.forEach { log ->
                Text(
                    text = log,
                    color = if (log.contains("ERROR") || log.contains("خطأ")) Color.Red else if (log.contains("SUCCESS") || log.contains("Compiled")) Color.Green else Color(0xFFCCCCCC),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            mockOutputs.forEach { mock ->
                Text(
                    text = mock,
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$ ",
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
                BasicTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    cursorBrush = SolidColor(Color.Green),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("terminal_command_input")
                )
            }
        }
    }
}

@Composable
fun CommandPalette(
    isOpen: Boolean,
    workspaceFiles: List<EditorFile> = emptyList(),
    onSelectFile: (EditorFile) -> Unit = {},
    onDismiss: () -> Unit,
    onExecute: (String) -> Unit,
    lang: String
) {
    if (!isOpen) return

    var query by remember { mutableStateOf("") }
    val commands = listOf(
        CommandItem("Save All / حفظ جميع الملفات المفتوحة (Ctrl+Shift+S)", "save_all", Icons.Default.DoneAll),
        CommandItem("Toggle Terminal / إظهار وإخفاء منفذ الأوامر (Ctrl+` / Ctrl+J)", "toggle_terminal", Icons.Default.Terminal),
        CommandItem("Close All Tabs / إغلاق كافة التبويبات المفتوحة", "close_all_tabs", Icons.Default.Close),
        CommandItem("Close Other Tabs / إغلاق التبويبات الأخرى", "close_other_tabs", Icons.Default.TabUnselected),
        CommandItem("Save File / حفظ الملف الحالي (Ctrl+S)", "save", Icons.Default.Save),
        CommandItem("New File / إنشاء ملف جديد (Ctrl+N)", "new_file", Icons.Default.Add),
        CommandItem("Find in File / البحث في الملف (Ctrl+F)", "find", Icons.Default.Search),
        CommandItem("Global Workspace Search / بحث المستودع (Ctrl+Shift+F)", "global_search", Icons.Default.FindInPage),
        CommandItem("Run Code / تشغيل الكود البرمجي (F5)", "run", Icons.Default.PlayArrow),
        CommandItem("Git: Initialize Repository / تهيئة مستودع Git محلي", "git_init", Icons.Default.CloudQueue),
        CommandItem("Git: Commit Changes / تسجيل التعديلات", "git_commit", Icons.Default.Commit),
        CommandItem("Git: View Commit History / استعراض سجل التعديلات", "git_log", Icons.Default.History),
        CommandItem("Database: Open SQLite Browser / استعراض قاعدة بيانات SQLite", "db_sqlite", Icons.Default.Storage),
        CommandItem("Debugger: Start Code Simulation / بدء محاكاة مصحح الأخطاء (F5)", "debug_start", Icons.Default.BugReport),
        CommandItem("Debugger: Step Over / الانتقال للسطر التالي (F10)", "debug_step_over", Icons.Default.Redo),
        CommandItem("Debugger: Stop Simulation / إيقاف محاكاة مصحح الأخطاء", "debug_stop", Icons.Default.Stop),
        CommandItem("Toggle Auto-Save / تبديل الحفظ التلقائي", "toggle_autosave", Icons.Default.Autorenew),
        CommandItem("Toggle Sidebar / إخفاء وإظهار القائمة (Ctrl+B)", "toggle_sidebar", Icons.Default.ViewSidebar),
        CommandItem("Toggle Mini-map / تبديل خريطة الكود", "toggle_minimap", Icons.Default.Map),
        CommandItem("Clear Console / مسح الطرفية", "clear_console", Icons.Default.Delete),
        CommandItem("Toggle Diff View / مقارنة التغييرات", "toggle_diff", Icons.Default.Compare),
        CommandItem("Toggle Theme / تبديل مظهر الألوان", "toggle_theme", Icons.Default.Brightness4),
        CommandItem("Switch Language / تغيير لغة التطبيق", "switch_lang", Icons.Default.Language),
        CommandItem("Open Settings / فتح الإعدادات", "open_settings", Icons.Default.Settings),
        CommandItem("Clear All Data / مسح كافة البيانات", "clear_data", Icons.Default.DeleteForever),
        CommandItem("Sync GitHub / مزامنة مستودع جيت", "sync_github", Icons.Default.SyncAlt)
    )

    val filteredCommands = if (query.isBlank()) {
        commands
    } else {
        commands.filter { it.title.contains(query, ignoreCase = true) }
    }

    val matchedFiles = if (query.isNotBlank()) {
        workspaceFiles.filter { it.name.contains(query, ignoreCase = true) }
    } else {
        emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        },
        title = {
            Column {
                Text(
                    text = "لوحة التحكم بالأوامر / Command Palette (Ctrl+P)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("ابحث عن أمر أو ملف... / Type command or file...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("command_palette_search_input")
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Matched Files section
                if (matchedFiles.isNotEmpty()) {
                    Text(
                        text = "الملفات المطابقة (${matchedFiles.size}):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    matchedFiles.forEach { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectFile(file)
                                    onDismiss()
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${file.name}  [${file.language}]",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))
                }

                // Matched Commands section
                filteredCommands.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onExecute(item.id)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }

                if (filteredCommands.isEmpty() && matchedFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد نتائج مطابقة / No matching commands or files",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    )
}

data class CommandItem(
    val title: String,
    val id: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun ActivityBar(
    activePanel: String,
    isSidebarExpanded: Boolean,
    onPanelSelect: (String) -> Unit,
    lang: String
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(62.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(bottom = 6.dp)
                )

                ActivityBarIcon(
                    icon = Icons.Default.Code,
                    label = "المحرر",
                    isSelected = !isSidebarExpanded,
                    onClick = { onPanelSelect("editor") },
                    tag = "activity_bar_editor"
                )

                ActivityBarIcon(
                    icon = Icons.Default.Folder,
                    label = Locales.get("menu_explorer", lang),
                    isSelected = isSidebarExpanded && activePanel == "files",
                    onClick = { onPanelSelect("files") },
                    tag = "activity_bar_files"
                )

                ActivityBarIcon(
                    icon = Icons.Default.Search,
                    label = Locales.get("menu_search", lang),
                    isSelected = isSidebarExpanded && activePanel == "search",
                    onClick = { onPanelSelect("search") },
                    tag = "activity_bar_search"
                )

                ActivityBarIcon(
                    icon = Icons.Default.Extension,
                    label = Locales.get("menu_extensions", lang),
                    isSelected = isSidebarExpanded && activePanel == "extensions",
                    onClick = { onPanelSelect("extensions") },
                    tag = "activity_bar_extensions"
                )

                ActivityBarIcon(
                    icon = Icons.Default.SyncAlt,
                    label = Locales.get("menu_github", lang),
                    isSelected = isSidebarExpanded && activePanel == "github",
                    onClick = { onPanelSelect("github") },
                    tag = "activity_bar_github"
                )

                ActivityBarIcon(
                    icon = Icons.Default.Storage,
                    label = Locales.get("menu_database", lang),
                    isSelected = isSidebarExpanded && activePanel == "db",
                    onClick = { onPanelSelect("db") },
                    tag = "activity_bar_db"
                )

                ActivityBarIcon(
                    icon = Icons.Default.BugReport,
                    label = Locales.get("menu_debugger", lang),
                    isSelected = isSidebarExpanded && activePanel == "debug",
                    onClick = { onPanelSelect("debug") },
                    tag = "activity_bar_debug"
                )

                ActivityBarIcon(
                    icon = Icons.Default.SystemUpdateAlt,
                    label = Locales.get("menu_updates", lang),
                    isSelected = isSidebarExpanded && activePanel == "updates",
                    onClick = { onPanelSelect("updates") },
                    tag = "activity_bar_updates"
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                ActivityBarIcon(
                    icon = Icons.Default.Settings,
                    label = Locales.get("menu_settings", lang),
                    isSelected = isSidebarExpanded && activePanel == "settings",
                    onClick = { onPanelSelect("settings") },
                    tag = "activity_bar_settings"
                )
            }
        }
    }
}

@Composable
fun ActivityBarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val tintColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable { onClick() }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.6f)
                    .width(3.dp)
                    .background(MaterialTheme.colorScheme.primary)
                    .align(Alignment.CenterStart)
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

private fun parseHexColor(hex: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}
