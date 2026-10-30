package com.example.ui.editor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

    var showExplorerSheet by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showGoToLineDialog by remember { mutableStateOf(false) }
    var showTextFxSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var pendingDeleteDoc by remember { mutableStateOf<CodeDocument?>(null) }

    val allDocs = (docsState as? UiState.Success)?.data ?: emptyList()
    val openTabs = remember(allDocs) { allDocs.filter { it.isOpenTab } }
    val activeDoc = remember(allDocs, activeDocId) {
        allDocs.firstOrNull { it.id == activeDocId }
    }

    // Collect one-shot UI messages
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val msg = when (event) {
                is EditorUiEvent.ShowMessage -> event.message
                is EditorUiEvent.ShowResMessage -> context.getString(event.resId)
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Back handler closes Find & Replace bar first when open
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 840.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        if (!isExpandedScreen) {
                            IconButton(
                                onClick = { showExplorerSheet = true },
                                modifier = Modifier.testTag("open_explorer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.cd_open_explorer)
                                )
                            }
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = activeDoc?.title ?: stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (activeDoc != null) {
                                    "CodePad • ${CodeLanguage.fromId(activeDoc.language).displayName}"
                                } else {
                                    "${allDocs.size} dokumen tersimpan"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showNewFileDialog = true },
                            modifier = Modifier.testTag("topbar_new_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.cd_new_document)
                            )
                        }

                        if (activeDoc != null) {
                            IconButton(
                                onClick = { viewModel.toggleFindBar() },
                                modifier = Modifier.testTag("topbar_find_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.cd_find_replace),
                                    tint = if (findReplaceState.isVisible) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }

                            IconButton(
                                onClick = { showTextFxSheet = true },
                                modifier = Modifier.testTag("topbar_textfx_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = stringResource(R.string.cd_text_tools)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.saveActiveDocumentNow() },
                                modifier = Modifier.testTag("topbar_save_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = stringResource(R.string.cd_save_document)
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.testTag("topbar_overflow_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.cd_more_options)
                                )
                            }

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_open_local_file)) },
                                    leadingIcon = { Icon(Icons.Default.FolderOpen, null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        openLocalLauncher.launch(arrayOf("*/*"))
                                    },
                                    modifier = Modifier.testTag("menu_open_local")
                                )

                                if (activeDoc != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_export_local_file)) },
                                        leadingIcon = { Icon(Icons.Default.SaveAlt, null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            exportLocalLauncher.launch(activeDoc.title)
                                        },
                                        modifier = Modifier.testTag("menu_export_local")
                                    )

                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_rename_document)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.DriveFileRenameOutline, null)
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            showRenameDialog = true
                                        },
                                        modifier = Modifier.testTag("menu_rename_doc")
                                    )

                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_goto_line)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.FormatListNumbered, null)
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            showGoToLineDialog = true
                                        },
                                        modifier = Modifier.testTag("menu_goto_line")
                                    )

                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_share_code)) },
                                        leadingIcon = { Icon(Icons.Default.Share, null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            shareCodeSnippet(context, activeDoc.title, editorValue.text)
                                        },
                                        modifier = Modifier.testTag("menu_share_code")
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_editor_settings)) },
                                    leadingIcon = { Icon(Icons.Default.Settings, null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showSettingsSheet = true
                                    },
                                    modifier = Modifier.testTag("menu_editor_settings")
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_sign_out)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        GoogleAuthHelper.signOut(
                                            credentialManager = credentialManager,
                                            onSignOutComplete = onSignedOut,
                                            scope = scope
                                        )
                                    },
                                    modifier = Modifier.testTag("menu_sign_out")
                                )
                            }
                        }
                    }
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
                            .width(290.dp)
                            .fillMaxHeight()
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outline)
                }

                // Right / Main Multi-Tab Code Workspace
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

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
                                MultiTabBar(
                                    tabs = openTabs,
                                    activeDocId = activeDocId,
                                    dirtyDocIds = dirtyDocIds,
                                    onSelectTab = { viewModel.selectTab(it.id) },
                                    onCloseTab = viewModel::closeTab,
                                    onAddTab = { showNewFileDialog = true }
                                )
                            }

                            if (activeDoc != null) {
                                FindReplaceBar(
                                    state = findReplaceState,
                                    onFindQueryChange = viewModel::updateFindQuery,
                                    onReplaceQueryChange = viewModel::updateReplaceQuery,
                                    onToggleMatchCase = viewModel::toggleMatchCase,
                                    onToggleRegex = viewModel::toggleUseRegex,
                                    onFindPrevious = viewModel::findPrevious,
                                    onFindNext = viewModel::findNext,
                                    onReplaceOne = viewModel::replaceCurrentMatch,
                                    onReplaceAll = viewModel::replaceAllMatches,
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
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                EmptyWorkspaceView(
                                    onCreateBlank = {
                                        viewModel.createNewDocument(
                                            title = "untitled.txt",
                                            language = CodeLanguage.PLAINTEXT,
                                            useStarterTemplate = false
                                        )
                                    },
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

        // Mobile Document Explorer Sheet
        if (showExplorerSheet && !isExpandedScreen) {
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
            initialTitle = "untitled.kt",
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
            onSymbolBarChange = viewModel::setShowSymbolBar,
            onTabSizeChange = viewModel::setTabSize
        )
    }
}

@Composable
private fun MultiTabBar(
    tabs: List<CodeDocument>,
    activeDocId: String?,
    dirtyDocIds: Set<String>,
    onSelectTab: (CodeDocument) -> Unit,
    onCloseTab: (CodeDocument) -> Unit,
    onAddTab: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("multi_tab_bar")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab.id == activeDocId
                    val isDirty = tab.id in dirtyDocIds
                    val lang = CodeLanguage.fromId(tab.language)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            }
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clickable { onSelectTab(tab) }
                            .testTag("editor_tab_${tab.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                .widthIn(max = 200.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(lang.badgeColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isDirty) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "●",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { onCloseTab(tab) }
                                    .testTag("close_tab_${tab.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cd_close_tab),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onAddTab,
                    modifier = Modifier.testTag("tab_bar_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_new_document),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
        }
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
