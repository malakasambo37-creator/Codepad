package com.example.ui.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CodeDocument
import com.example.data.model.CodeLanguage
import com.example.domain.TextFxOperations
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.theme.EditorColorPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    viewModel: EditorViewModel,
    palette: EditorColorPalette,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    val snackbarHostState = remember { SnackbarHostState() }

    val docsState by viewModel.documentsState.collectAsStateWithLifecycle()
    val activeDocId by viewModel.activeDocId.collectAsStateWithLifecycle()
    val editorValue by viewModel.editorValue.collectAsStateWithLifecycle()
    val settings by viewModel.editorSettings.collectAsStateWithLifecycle()
    val findReplaceState by viewModel.findReplaceState.collectAsStateWithLifecycle()
    val saveStatus by viewModel.saveStatus.collectAsStateWithLifecycle()
    val dirtyDocIds by viewModel.dirtyDocIds.collectAsStateWithLifecycle()
    val explorerQuery by viewModel.explorerSearchQuery.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val isRecordingMacro by viewModel.isRecordingMacro.collectAsStateWithLifecycle()
    val hasSavedMacro by viewModel.hasSavedMacro.collectAsStateWithLifecycle()

    var showExplorerSheet by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showGoToLineDialog by remember { mutableStateOf(false) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var showMacroMultipleDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showTextFxSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var pendingDeleteDoc by remember { mutableStateOf<CodeDocument?>(null) }

    val allDocs = (docsState as? UiState.Success)?.data ?: emptyList()
    val openTabs = remember(allDocs) { allDocs.filter { it.isOpenTab } }
    val activeDoc = remember(allDocs, activeDocId) {
        allDocs.firstOrNull { it.id == activeDocId }
    }
    val isActiveDirty = activeDoc != null && activeDoc.id in dirtyDocIds

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val msg = when (event) {
                is EditorUiEvent.ShowMessage -> event.message
                is EditorUiEvent.ShowResMessage -> context.getString(event.resId)
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    BackHandler(enabled = findReplaceState.isVisible) {
        viewModel.toggleFindBar(false)
    }

    // SAF Open Local File Launcher
    val openLocalLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val pair = withContext(Dispatchers.IO) { readUriTextAndName(context, uri) }
                if (pair != null) {
                    viewModel.importLocalFile(pair.first, pair.second)
                }
            }
        }
    }

    // SAF Export / Save As Launcher
    val exportLocalLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            val contentToSave = if (activeDoc?.lineEnding == "CRLF") {
                editorValue.text.replace("\n", "\r\n")
            } else {
                editorValue.text
            }
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(contentToSave.toByteArray(Charsets.UTF_8))
                        }
                        true
                    }.getOrDefault(false)
                }
                if (ok) {
                    viewModel.notifyMessageRes(R.string.msg_exported_file)
                }
            }
        }
    }

    // Real System Clipboard Cut / Copy / Paste Handlers
    val performCopy: () -> Unit = {
        val text = editorValue.text
        val min = editorValue.selection.min.coerceIn(0, text.length)
        val max = editorValue.selection.max.coerceIn(min, text.length)
        val snippet = if (max > min) {
            text.substring(min, max)
        } else {
            val lineStart = text.lastIndexOf('\n', (min - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
            val lineEnd = text.indexOf('\n', min).let { if (it == -1) text.length else it }
            text.substring(lineStart, lineEnd)
        }
        if (snippet.isNotEmpty()) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("CodePad", snippet))
        }
    }

    val performCut: () -> Unit = {
        val text = editorValue.text
        val min = editorValue.selection.min.coerceIn(0, text.length)
        val max = editorValue.selection.max.coerceIn(min, text.length)
        if (max > min) {
            val snippet = text.substring(min, max)
            clipboardManager.setPrimaryClip(ClipData.newPlainText("CodePad", snippet))
            val updated = text.substring(0, min) + text.substring(max)
            viewModel.onEditorValueChange(TextFieldValue(updated, TextRange(min)))
        } else {
            performCopy()
            viewModel.applyTextTool(com.example.domain.TextToolAction.DELETE_CURRENT_LINE)
        }
    }

    val performPaste: () -> Unit = {
        val clipText = clipboardManager.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
        if (!clipText.isNullOrEmpty()) {
            viewModel.insertRawText(clipText, autoPair = false)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 840.dp

        val chromeCallbacks = remember(activeDoc, settings, editorValue) {
            NotepadChromeCallbacks(
                onNewQuickTab = viewModel::createQuickNewTab,
                onNewTemplateDialog = { showNewFileDialog = true },
                onOpenLocalFile = { openLocalLauncher.launch(arrayOf("*/*")) },
                onOpenExplorer = { showExplorerSheet = true },
                onSave = viewModel::saveActiveDocumentNow,
                onSaveAll = viewModel::saveAllTabs,
                onSaveAsLocal = { activeDoc?.let { exportLocalLauncher.launch(it.title) } },
                onRenameDoc = { if (activeDoc != null) showRenameDialog = true },
                onCloseActiveTab = { activeDoc?.let(viewModel::closeTab) },
                onCloseAllTabs = viewModel::closeAllTabs,
                onCloseAllButActive = { activeDoc?.let { viewModel.closeAllBut(it.id) } },
                onShareText = {
                    activeDoc?.let { shareCodeSnippet(context, it.title, editorValue.text) }
                },
                onSignOut = {
                    GoogleAuthHelper.signOut(
                        credentialManager = credentialManager,
                        onSignOutComplete = onSignedOut,
                        scope = scope
                    )
                },
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onCut = performCut,
                onCopy = performCopy,
                onPaste = performPaste,
                onSelectAll = viewModel::selectAllText,
                onApplyTextTool = viewModel::applyTextTool,
                onOpenFindTab = viewModel::openFindPanel,
                onFindNext = viewModel::findNext,
                onFindPrev = viewModel::findPrevious,
                onOpenGoToLine = { showGoToLineDialog = true },
                onToggleWordWrap = { viewModel.setWordWrap(!settings.wordWrap) },
                onToggleWhitespace = { viewModel.setShowWhitespace(!settings.showWhitespace) },
                onToggleIndentGuides = { viewModel.setShowIndentGuides(!settings.showIndentGuides) },
                onToggleLineNumbers = { viewModel.setShowLineNumbers(!settings.showLineNumbers) },
                onToggleFunctionList = { viewModel.setShowFunctionList(!settings.showFunctionList) },
                onToggleDocumentMap = { viewModel.setShowDocumentMap(!settings.showDocumentMap) },
                onToggleSymbolBar = { viewModel.setShowSymbolBar(!settings.showSymbolBar) },
                onZoomIn = viewModel::zoomIn,
                onZoomOut = viewModel::zoomOut,
                onResetZoom = viewModel::resetZoom,
                onOpenSummary = { if (activeDoc != null) showSummaryDialog = true },
                onToggleLineEnding = viewModel::toggleLineEnding,
                onSelectLanguage = viewModel::setActiveLanguage,
                onOpenStyleConfigurator = { showSettingsSheet = true },
                onOpenTextFxSheet = { showTextFxSheet = true },
                onStartMacro = viewModel::startMacroRecording,
                onStopMacro = viewModel::stopMacroRecording,
                onPlayMacroOnce = { viewModel.playMacro(1) },
                onOpenMacroMultiple = { showMacroMultipleDialog = true },
                onOpenAbout = { showAboutDialog = true }
            )
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = palette.editorBackground,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                NotepadDesktopChromeHeader(
                    activeDoc = activeDoc,
                    isActiveDirty = isActiveDirty,
                    hasAnyDirty = dirtyDocIds.isNotEmpty(),
                    allDocsCount = allDocs.size,
                    isExpandedScreen = isExpandedScreen,
                    palette = palette,
                    settings = settings,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    isRecordingMacro = isRecordingMacro,
                    hasSavedMacro = hasSavedMacro,
                    callbacks = chromeCallbacks
                )
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                // Canonical Supporting Sidebar on Expanded / Tablet screens
                if (isExpandedScreen) {
                    ExplorerPanelContent(
                        documents = allDocs,
                        activeDocId = activeDocId,
                        dirtyDocIds = dirtyDocIds,
                        searchQuery = explorerQuery,
                        onSearchQueryChange = viewModel::setExplorerSearchQuery,
                        onSelectDocument = { doc -> viewModel.selectTab(doc.id) },
                        onTogglePin = viewModel::togglePin,
                        onRequestDelete = { doc -> pendingDeleteDoc = doc },
                        onNewDocumentClick = { showNewFileDialog = true },
                        modifier = Modifier
                            .width(275.dp)
                            .fillMaxHeight()
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outline)
                }

                // Main Notepad++ Scintilla Editor Pane
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (val state = docsState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is UiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        is UiState.Success -> {
                            if (openTabs.isNotEmpty()) {
                                NotepadScintillaTabBar(
                                    tabs = openTabs,
                                    activeDocId = activeDocId,
                                    dirtyDocIds = dirtyDocIds,
                                    palette = palette,
                                    onSelectTab = { viewModel.selectTab(it.id) },
                                    onCloseTab = viewModel::closeTab,
                                    onCloseAllBut = { viewModel.closeAllBut(it.id) },
                                    onCloseAll = viewModel::closeAllTabs,
                                    onRenameActive = { showRenameDialog = true },
                                    onAddQuickTab = viewModel::createQuickNewTab
                                )
                            }

                            if (activeDoc != null) {
                                FindReplaceBar(
                                    state = findReplaceState,
                                    palette = palette,
                                    onSelectTab = viewModel::setFindDialogTab,
                                    onFindQueryChange = viewModel::updateFindQuery,
                                    onReplaceQueryChange = viewModel::updateReplaceQuery,
                                    onToggleMatchCase = viewModel::toggleMatchCase,
                                    onSelectSearchMode = viewModel::setSearchMode,
                                    onFindPrevious = viewModel::findPrevious,
                                    onFindNext = viewModel::findNext,
                                    onCountMatches = viewModel::countMatchesToast,
                                    onReplaceOne = viewModel::replaceCurrentMatch,
                                    onReplaceAll = viewModel::replaceAllMatches,
                                    onMarkAll = viewModel::markAllOccurrences,
                                    onClearMarks = viewModel::clearMarks,
                                    onClose = { viewModel.toggleFindBar(false) }
                                )

                                CodeEditorCanvas(
                                    value = editorValue,
                                    onValueChange = viewModel::onEditorValueChange,
                                    language = CodeLanguage.fromId(activeDoc.language),
                                    palette = palette,
                                    settings = settings,
                                    findReplaceState = findReplaceState,
                                    saveStatus = saveStatus,
                                    lineEnding = activeDoc.lineEnding,
                                    canUndo = canUndo,
                                    canRedo = canRedo,
                                    onUndo = viewModel::undo,
                                    onRedo = viewModel::redo,
                                    onInsertSymbol = viewModel::insertSymbol,
                                    onLanguageBadgeClick = { showRenameDialog = true },
                                    onSummaryClick = { showSummaryDialog = true },
                                    onGoToLineClick = { showGoToLineDialog = true },
                                    onLineEndingClick = viewModel::toggleLineEnding,
                                    onOvertypeClick = viewModel::toggleOvertypeMode,
                                    onJumpToOffset = viewModel::jumpToCharOffset,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                EmptyWorkspaceView(
                                    onCreateBlank = viewModel::createQuickNewTab,
                                    onOpenLocalFile = {
                                        openLocalLauncher.launch(arrayOf("*/*"))
                                    },
                                    onCreateTemplate = { fileName, lang ->
                                        viewModel.createNewDocument(
                                            title = fileName,
                                            language = lang,
                                            useStarterTemplate = true
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Workspace Document Explorer Sheet
        if (showExplorerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showExplorerSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp)
                        .navigationBarsPadding()
                ) {
                    ExplorerPanelContent(
                        documents = allDocs,
                        activeDocId = activeDocId,
                        dirtyDocIds = dirtyDocIds,
                        searchQuery = explorerQuery,
                        onSearchQueryChange = viewModel::setExplorerSearchQuery,
                        onSelectDocument = { doc ->
                            viewModel.selectTab(doc.id)
                            showExplorerSheet = false
                        },
                        onTogglePin = viewModel::togglePin,
                        onRequestDelete = { doc -> pendingDeleteDoc = doc },
                        onNewDocumentClick = {
                            showExplorerSheet = false
                            showNewFileDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showNewFileDialog) {
        NewOrRenameDocumentDialog(
            isRenameMode = false,
            initialTitle = viewModel.nextNewTabTitle(allDocs),
            initialLanguage = CodeLanguage.KOTLIN,
            onDismiss = { showNewFileDialog = false },
            onConfirm = { title, lang, useStarter ->
                showNewFileDialog = false
                viewModel.createNewDocument(title, lang, useStarter)
            }
        )
    }

    if (showRenameDialog && activeDoc != null) {
        NewOrRenameDocumentDialog(
            isRenameMode = true,
            initialTitle = activeDoc.title,
            initialLanguage = CodeLanguage.fromId(activeDoc.language),
            onDismiss = { showRenameDialog = false },
            onConfirm = { title, lang, _ ->
                showRenameDialog = false
                viewModel.renameAndSetLanguage(title, lang)
            }
        )
    }

    if (showGoToLineDialog) {
        val maxLine = TextFxOperations.countLines(editorValue.text)
        GoToLineDialog(
            maxLines = maxLine,
            onDismiss = { showGoToLineDialog = false },
            onGoToLine = { line ->
                showGoToLineDialog = false
                viewModel.goToLine(line)
            }
        )
    }

    if (showSummaryDialog && activeDoc != null) {
        val stats = remember(editorValue.text, editorValue.selection) {
            TextFxOperations.computeDocumentSummary(editorValue.text, editorValue.selection)
        }
        DocumentSummaryDialog(
            doc = activeDoc,
            stats = stats,
            onDismiss = { showSummaryDialog = false }
        )
    }

    if (showMacroMultipleDialog) {
        RunMacroMultipleDialog(
            onDismiss = { showMacroMultipleDialog = false },
            onRunMacro = { times ->
                showMacroMultipleDialog = false
                viewModel.playMacro(times)
            }
        )
    }

    if (showAboutDialog) {
        AboutNotepadDialog(onDismiss = { showAboutDialog = false })
    }

    pendingDeleteDoc?.let { target ->
        DeleteDocumentDialog(
            doc = target,
            onDismiss = { pendingDeleteDoc = null },
            onConfirmDelete = {
                viewModel.deleteDocument(target)
                pendingDeleteDoc = null
            }
        )
    }

    if (showTextFxSheet) {
        TextFxBottomSheet(
            onDismiss = { showTextFxSheet = false },
            onApplyTool = viewModel::applyTextTool,
            onToggleLineEnding = viewModel::toggleLineEnding
        )
    }

    if (showSettingsSheet) {
        EditorSettingsBottomSheet(
            settings = settings,
            onDismiss = { showSettingsSheet = false },
            onSelectTheme = viewModel::setEditorTheme,
            onFontSizeChange = viewModel::setFontSize,
            onWordWrapChange = viewModel::setWordWrap,
            onLineNumbersChange = viewModel::setShowLineNumbers,
            onWhitespaceChange = viewModel::setShowWhitespace,
            onIndentGuidesChange = viewModel::setShowIndentGuides,
            onDocumentMapChange = viewModel::setShowDocumentMap,
            onFunctionListChange = viewModel::setShowFunctionList,
            onSymbolBarChange = viewModel::setShowSymbolBar,
            onTabSizeChange = viewModel::setTabSize
        )
    }
}

private fun readUriTextAndName(context: Context, uri: Uri): Pair<String, String>? {
    return runCatching {
        var displayName = "imported.txt"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIdx != -1 && cursor.moveToFirst()) {
                displayName = cursor.getString(nameIdx) ?: displayName
            }
        }
        val content = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        } ?: return null
        displayName to content
    }.getOrNull()
}

private fun shareCodeSnippet(context: Context, title: String, content: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(sendIntent, title))
}
