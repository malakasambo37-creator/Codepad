package com.example.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CodeDocument
import com.example.data.model.CodeLanguage
import com.example.data.preferences.EditorSettings
import com.example.domain.TextToolAction
import com.example.ui.theme.EditorColorPalette
import com.example.ui.theme.FloppyDirtyRed
import com.example.ui.theme.FloppySavedBlue
import com.example.ui.theme.NotepadTabActiveStripe

data class NotepadChromeCallbacks(
    val onNewQuickTab: () -> Unit,
    val onNewTemplateDialog: () -> Unit,
    val onOpenLocalFile: () -> Unit,
    val onOpenExplorer: () -> Unit,
    val onSave: () -> Unit,
    val onSaveAll: () -> Unit,
    val onSaveAsLocal: () -> Unit,
    val onRenameDoc: () -> Unit,
    val onCloseActiveTab: () -> Unit,
    val onCloseAllTabs: () -> Unit,
    val onCloseAllButActive: () -> Unit,
    val onShareText: () -> Unit,
    val onSignOut: () -> Unit,
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
    val onCut: () -> Unit,
    val onCopy: () -> Unit,
    val onPaste: () -> Unit,
    val onSelectAll: () -> Unit,
    val onApplyTextTool: (TextToolAction) -> Unit,
    val onOpenFindTab: (Int) -> Unit,
    val onFindNext: () -> Unit,
    val onFindPrev: () -> Unit,
    val onOpenGoToLine: () -> Unit,
    val onToggleWordWrap: () -> Unit,
    val onToggleWhitespace: () -> Unit,
    val onToggleIndentGuides: () -> Unit,
    val onToggleLineNumbers: () -> Unit,
    val onToggleFunctionList: () -> Unit,
    val onToggleDocumentMap: () -> Unit,
    val onToggleSymbolBar: () -> Unit,
    val onZoomIn: () -> Unit,
    val onZoomOut: () -> Unit,
    val onResetZoom: () -> Unit,
    val onOpenSummary: () -> Unit,
    val onToggleLineEnding: () -> Unit,
    val onSelectLanguage: (CodeLanguage) -> Unit,
    val onOpenStyleConfigurator: () -> Unit,
    val onOpenTextFxSheet: () -> Unit,
    val onStartMacro: () -> Unit,
    val onStopMacro: () -> Unit,
    val onPlayMacroOnce: () -> Unit,
    val onOpenMacroMultiple: () -> Unit,
    val onOpenAbout: () -> Unit
)

@Composable
fun NotepadDesktopChromeHeader(
    activeDoc: CodeDocument?,
    isActiveDirty: Boolean,
    hasAnyDirty: Boolean,
    allDocsCount: Int,
    isExpandedScreen: Boolean,
    palette: EditorColorPalette,
    settings: EditorSettings,
    canUndo: Boolean,
    canRedo: Boolean,
    isRecordingMacro: Boolean,
    hasSavedMacro: Boolean,
    callbacks: NotepadChromeCallbacks
) {
    val chromeTextColor = if (palette.isDark) Color(0xFFE5E7EB) else Color(0xFF1F2937)
    val chromeSubtleColor = if (palette.isDark) Color(0xFF9CA3AF) else Color(0xFF4B5563)
    val borderColor = if (palette.isDark) Color(0xFF3B4048) else Color(0xFFC8CCD2)

    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Classic Notepad++ Window Title Bar
        Surface(
            color = palette.chromeMenuBackground,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("npp_window_title_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isExpandedScreen) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clickable(onClick = callbacks.onOpenExplorer)
                            .testTag("open_explorer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.cd_open_explorer),
                            tint = chromeTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Chameleon green Notepad++ emblem
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                val dirtyPrefix = if (isActiveDirty) "*" else ""
                val windowTitle = if (activeDoc != null) {
                    "$dirtyPrefix${activeDoc.title} - CodePad (Notepad++)"
                } else {
                    "CodePad (Notepad++)"
                }

                Text(
                    text = windowTitle,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = chromeTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isRecordingMacro) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = FloppyDirtyRed.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, FloppyDirtyRed),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(FloppyDirtyRed, CircleShape)
                            )
                            Text(
                                text = "REC",
                                style = MaterialTheme.typography.labelSmall,
                                color = FloppyDirtyRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = borderColor.copy(alpha = 0.35f),
                    modifier = Modifier
                        .clickable(onClick = callbacks.onOpenExplorer)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "$allDocsCount files",
                        style = MaterialTheme.typography.labelSmall,
                        color = chromeSubtleColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = borderColor.copy(alpha = 0.7f))

        // 2. Classic Notepad++ Horizontal Menu Bar: File | Edit | Search | View | Encoding | Language | Settings | TextFX | Macro | ?
        NotepadClassicMenuBar(
            activeDoc = activeDoc,
            palette = palette,
            settings = settings,
            chromeTextColor = chromeTextColor,
            chromeSubtleColor = chromeSubtleColor,
            isRecordingMacro = isRecordingMacro,
            hasSavedMacro = hasSavedMacro,
            callbacks = callbacks
        )

        HorizontalDivider(thickness = 1.dp, color = borderColor)

        // 3. Classic Notepad++ Scrollable Icon Toolbar
        NotepadClassicIconToolbar(
            hasActiveDoc = activeDoc != null,
            isActiveDirty = isActiveDirty,
            hasAnyDirty = hasAnyDirty,
            palette = palette,
            settings = settings,
            chromeTextColor = chromeTextColor,
            borderColor = borderColor,
            canUndo = canUndo,
            canRedo = canRedo,
            isRecordingMacro = isRecordingMacro,
            hasSavedMacro = hasSavedMacro,
            callbacks = callbacks
        )

        HorizontalDivider(thickness = 1.dp, color = borderColor)
    }
}

@Composable
private fun NotepadClassicMenuBar(
    activeDoc: CodeDocument?,
    palette: EditorColorPalette,
    settings: EditorSettings,
    chromeTextColor: Color,
    chromeSubtleColor: Color,
    isRecordingMacro: Boolean,
    hasSavedMacro: Boolean,
    callbacks: NotepadChromeCallbacks
) {
    var openMenu by remember { mutableStateOf<String?>(null) }
    val activeLang = activeDoc?.let { CodeLanguage.fromId(it.language) } ?: CodeLanguage.PLAINTEXT

    Surface(
        color = palette.chromeMenuBackground,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("npp_menu_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // FILE MENU
            MenuBarTrigger(
                title = stringResource(R.string.npp_menu_file),
                isOpen = openMenu == "file",
                textColor = chromeTextColor,
                tag = "menu_bar_file",
                onClick = { openMenu = "file" }
            ) {
                DropdownMenu(expanded = openMenu == "file", onDismissRequest = { openMenu = null }) {
                    DesktopMenuItem("New", "Ctrl+N", "menu_file_new") { openMenu = null; callbacks.onNewQuickTab() }
                    DesktopMenuItem("New from Template…", "", "menu_file_template") { openMenu = null; callbacks.onNewTemplateDialog() }
                    DesktopMenuItem("Open Local File…", "Ctrl+O", "menu_open_local") { openMenu = null; callbacks.onOpenLocalFile() }
                    DesktopMenuItem("Workspace Explorer…", "", "menu_file_explorer") { openMenu = null; callbacks.onOpenExplorer() }
                    HorizontalDivider()
                    DesktopMenuItem("Save", "Ctrl+S", "menu_file_save", enabled = activeDoc != null) { openMenu = null; callbacks.onSave() }
                    DesktopMenuItem("Save As (Device)…", "Ctrl+Alt+S", "menu_export_local", enabled = activeDoc != null) { openMenu = null; callbacks.onSaveAsLocal() }
                    DesktopMenuItem("Save All", "Ctrl+Shift+S", "menu_file_save_all") { openMenu = null; callbacks.onSaveAll() }
                    DesktopMenuItem("Rename…", "", "menu_rename_doc", enabled = activeDoc != null) { openMenu = null; callbacks.onRenameDoc() }
                    HorizontalDivider()
                    DesktopMenuItem("Close", "Ctrl+W", "menu_file_close", enabled = activeDoc != null) { openMenu = null; callbacks.onCloseActiveTab() }
                    DesktopMenuItem("Close All", "", "menu_file_close_all") { openMenu = null; callbacks.onCloseAllTabs() }
                    DesktopMenuItem("Close All BUT This", "", "menu_file_close_others", enabled = activeDoc != null) { openMenu = null; callbacks.onCloseAllButActive() }
                    HorizontalDivider()
                    DesktopMenuItem("Share / Export Text…", "", "menu_share_code", enabled = activeDoc != null) { openMenu = null; callbacks.onShareText() }
                    DesktopMenuItem("Sign Out", "", "menu_sign_out") { openMenu = null; callbacks.onSignOut() }
                }
            }

            // EDIT MENU
            MenuBarTrigger(
                title = stringResource(R.string.npp_menu_edit),
                isOpen = openMenu == "edit",
                textColor = chromeTextColor,
                tag = "menu_bar_edit",
                onClick = { openMenu = "edit" }
            ) {
                DropdownMenu(expanded = openMenu == "edit", onDismissRequest = { openMenu = null }) {
                    DesktopMenuItem("Undo", "Ctrl+Z", "menu_edit_undo") { openMenu = null; callbacks.onUndo() }
                    DesktopMenuItem("Redo", "Ctrl+Y", "menu_edit_redo") { openMenu = null; callbacks.onRedo() }
                    HorizontalDivider()
                    DesktopMenuItem("Cut", "Ctrl+X", "menu_edit_cut") { openMenu = null; callbacks.onCut() }
                    DesktopMenuItem("Copy", "Ctrl+C", "menu_edit_copy") { openMenu = null; callbacks.onCopy() }
                    DesktopMenuItem("Paste", "Ctrl+V", "menu_edit_paste") { openMenu = null; callbacks.onPaste() }
                    DesktopMenuItem("Select All", "Ctrl+A", "menu_edit_select_all") { openMenu = null; callbacks.onSelectAll() }
                    HorizontalDivider()
                    DesktopMenuItem("Duplicate Current Line", "Ctrl+D", "menu_edit_dup") { openMenu = null; callbacks.onApplyTextTool(TextToolAction.DUPLICATE_CURRENT_LINE) }
                    DesktopMenuItem("Delete Current Line", "Ctrl+L", "menu_edit_del_line") { openMenu = null; callbacks.onApplyTextTool(TextToolAction.DELETE_CURRENT_LINE) }
                    DesktopMenuItem("Move Up Current Line", "Ctrl+Shift+Up", "menu_edit_up") { openMenu = null; callbacks.onApplyTextTool(TextToolAction.MOVE_LINE_UP) }
                    DesktopMenuItem("Move Down Current Line", "Ctrl+Shift+Down", "menu_edit_down") { openMenu = null; callbacks.onApplyTextTool(TextToolAction.MOVE_LINE_DOWN) }
                    DesktopMenuItem("Toggle Single Line Comment", "Ctrl+Q", "menu_edit_comment") { openMenu = null; callbacks.onApplyTextTool(TextToolAction.TOGGLE_LINE_COMMENT) }
                    HorizontalDivider()
                    DesktopMenuItem("UPPERCASE", "Ctrl+Shift+U", "menu_edit_upper") { openMenu = null; callbacks.onApplyTextTool(TextToolAction.UPPERCASE) }
                    DesktopMenuItem("lowercase", "Ctrl+U", "menu_edit_lower") { openMenu = null; callbacks.onApplyTextTool(TextToolAction.LOWERCASE) }
                    DesktopMenuItem("Insert Date Time", "F5", "menu_edit_datetime") { openMenu = null; callbacks.onApplyTextTool(TextToolAction.INSERT_DATETIME) }
                }
            }

            // SEARCH MENU
            MenuBarTrigger(
                title = stringResource(R.string.npp_menu_search),
                isOpen = openMenu == "search",
                textColor = chromeTextColor,
                tag = "menu_bar_search",
                onClick = { openMenu = "search" }
            ) {
                DropdownMenu(expanded = openMenu == "search", onDismissRequest = { openMenu = null }) {
                    DesktopMenuItem("Find…", "Ctrl+F", "menu_search_find") { openMenu = null; callbacks.onOpenFindTab(0) }
                    DesktopMenuItem("Replace…", "Ctrl+H", "menu_search_replace") { openMenu = null; callbacks.onOpenFindTab(1) }
                    DesktopMenuItem("Mark…", "Ctrl+M", "menu_search_mark") { openMenu = null; callbacks.onOpenFindTab(2) }
                    HorizontalDivider()
                    DesktopMenuItem("Find Next", "F3", "menu_search_next") { openMenu = null; callbacks.onFindNext() }
                    DesktopMenuItem("Find Previous", "Shift+F3", "menu_search_prev") { openMenu = null; callbacks.onFindPrev() }
                    HorizontalDivider()
                    DesktopMenuItem("Go to…", "Ctrl+G", "menu_goto_line") { openMenu = null; callbacks.onOpenGoToLine() }
                }
            }

            // VIEW MENU
            MenuBarTrigger(
                title = stringResource(R.string.npp_menu_view),
                isOpen = openMenu == "view",
                textColor = chromeTextColor,
                tag = "menu_bar_view",
                onClick = { openMenu = "view" }
            ) {
                DropdownMenu(expanded = openMenu == "view", onDismissRequest = { openMenu = null }) {
                    DesktopMenuItem("Word wrap", "", "menu_view_wrap", checked = settings.wordWrap) { openMenu = null; callbacks.onToggleWordWrap() }
                    DesktopMenuItem("Show White Space and EOL", "", "menu_view_ws", checked = settings.showWhitespace) { openMenu = null; callbacks.onToggleWhitespace() }
                    DesktopMenuItem("Show Indent Guide", "", "menu_view_indent", checked = settings.showIndentGuides) { openMenu = null; callbacks.onToggleIndentGuides() }
                    DesktopMenuItem("Display Line Number", "", "menu_view_ln", checked = settings.showLineNumbers) { openMenu = null; callbacks.onToggleLineNumbers() }
                    DesktopMenuItem("Mobile Symbol Helper Bar", "", "menu_view_sym", checked = settings.showSymbolBar) { openMenu = null; callbacks.onToggleSymbolBar() }
                    HorizontalDivider()
                    DesktopMenuItem("Document Map", "", "menu_view_map", checked = settings.showDocumentMap) { openMenu = null; callbacks.onToggleDocumentMap() }
                    DesktopMenuItem("Function List", "", "menu_view_fn", checked = settings.showFunctionList) { openMenu = null; callbacks.onToggleFunctionList() }
                    HorizontalDivider()
                    DesktopMenuItem("Zoom In", "Ctrl+Num+", "menu_view_zin") { openMenu = null; callbacks.onZoomIn() }
                    DesktopMenuItem("Zoom Out", "Ctrl+Num-", "menu_view_zout") { openMenu = null; callbacks.onZoomOut() }
                    DesktopMenuItem("Restore Default Zoom", "Ctrl+/", "menu_view_zreset") { openMenu = null; callbacks.onResetZoom() }
                    HorizontalDivider()
                    DesktopMenuItem("Summary…", "", "menu_view_summary", enabled = activeDoc != null) { openMenu = null; callbacks.onOpenSummary() }
                }
            }

            // ENCODING MENU
            MenuBarTrigger(
                title = stringResource(R.string.npp_menu_encoding),
                isOpen = openMenu == "encoding",
                textColor = chromeTextColor,
                tag = "menu_bar_encoding",
                onClick = { openMenu = "encoding" }
            ) {
                DropdownMenu(expanded = openMenu == "encoding", onDismissRequest = { openMenu = null }) {
                    DesktopMenuItem("UTF-8", "", "menu_enc_utf8", checked = true) { openMenu = null }
                    HorizontalDivider()
                    DesktopMenuItem(
                        "Windows (CR LF)",
                        "",
                        "menu_eol_crlf",
                        checked = activeDoc?.lineEnding == "CRLF"
                    ) {
                        openMenu = null
                        if (activeDoc?.lineEnding != "CRLF") callbacks.onToggleLineEnding()
                    }
                    DesktopMenuItem(
                        "Unix (LF)",
                        "",
                        "menu_eol_lf",
                        checked = activeDoc?.lineEnding != "CRLF"
                    ) {
                        openMenu = null
                        if (activeDoc?.lineEnding == "CRLF") callbacks.onToggleLineEnding()
                    }
                }
            }

            // LANGUAGE MENU
            MenuBarTrigger(
                title = stringResource(R.string.npp_menu_language),
                isOpen = openMenu == "language",
                textColor = chromeTextColor,
                tag = "menu_bar_language",
                onClick = { openMenu = "language" }
            ) {
                DropdownMenu(expanded = openMenu == "language", onDismissRequest = { openMenu = null }) {
                    CodeLanguage.entries.forEach { lang ->
                        DesktopMenuItem(
                            label = lang.displayName,
                            shortcut = ".${lang.defaultExtension}",
                            tag = "menu_lang_${lang.id}",
                            checked = activeLang == lang
                        ) {
                            openMenu = null
                            callbacks.onSelectLanguage(lang)
                        }
                    }
                }
            }

            // SETTINGS MENU
            MenuBarTrigger(
                title = stringResource(R.string.npp_menu_settings),
                isOpen = openMenu == "settings",
                textColor = chromeTextColor,
                tag = "menu_bar_settings",
                onClick = { openMenu = "settings" }
            ) {
                DropdownMenu(expanded = openMenu == "settings", onDismissRequest = { openMenu = null }) {
                    DesktopMenuItem("Style Configurator…", "", "menu_editor_settings") {
                        openMenu = null
                        callbacks.onOpenStyleConfigurator()
                    }
                }
            }

            // TEXTFX / TOOLS MENU
            MenuBarTrigger(
                title = stringResource(R.string.npp_menu_tools),
                isOpen = openMenu == "tools",
                textColor = chromeTextColor,
                tag = "menu_bar_tools",
                onClick = { openMenu = "tools" }
            ) {
                DropdownMenu(expanded = openMenu == "tools", onDismissRequest = { openMenu = null }) {
                    DesktopMenuItem("All TextFX & Line Operations…", "", "menu_tools_sheet") { openMenu = null; callbacks.onOpenTextFxSheet() }
                    HorizontalDivider()
                    DesktopMenuItem("Sort Lines Ascending", "", "menu_tools_sort_asc") { openMenu = null; callbacks.onApplyTextTool(TextToolAction.SORT_LINES_ASC) }
                    DesktopMenuItem("Sort Lines Descending", "", "menu_tools_sort_desc") { openMenu = null; callbacks.onApplyTextTool(TextToolAction.SORT_LINES_DESC) }
                    DesktopMenuItem("Remove Duplicate Lines", "", "menu_tools_dedupe") { openMenu = null; callbacks.onApplyTextTool(TextToolAction.REMOVE_DUPLICATE_LINES) }
                    DesktopMenuItem("Trim Trailing Space", "", "menu_tools_trim") { openMenu = null; callbacks.onApplyTextTool(TextToolAction.TRIM_TRAILING_SPACES) }
                    HorizontalDivider()
                    DesktopMenuItem("JSON Pretty Format", "", "menu_tools_json") { openMenu = null; callbacks.onApplyTextTool(TextToolAction.FORMAT_JSON_PRETTY) }
                    DesktopMenuItem("Base64 Encode", "", "menu_tools_b64e") { openMenu = null; callbacks.onApplyTextTool(TextToolAction.BASE64_ENCODE) }
                    DesktopMenuItem("Base64 Decode", "", "menu_tools_b64d") { openMenu = null; callbacks.onApplyTextTool(TextToolAction.BASE64_DECODE) }
                }
            }

            // MACRO MENU
            MenuBarTrigger(
                title = stringResource(R.string.npp_menu_macro),
                isOpen = openMenu == "macro",
                textColor = chromeTextColor,
                tag = "menu_bar_macro",
                onClick = { openMenu = "macro" }
            ) {
                DropdownMenu(expanded = openMenu == "macro", onDismissRequest = { openMenu = null }) {
                    DesktopMenuItem("Start Recording", "", "menu_macro_start", enabled = !isRecordingMacro) { openMenu = null; callbacks.onStartMacro() }
                    DesktopMenuItem("Stop Recording", "", "menu_macro_stop", enabled = isRecordingMacro) { openMenu = null; callbacks.onStopMacro() }
                    HorizontalDivider()
                    DesktopMenuItem("Playback", "Ctrl+Shift+P", "menu_macro_play", enabled = hasSavedMacro && !isRecordingMacro) { openMenu = null; callbacks.onPlayMacroOnce() }
                    DesktopMenuItem("Run a Macro Multiple Times…", "", "menu_macro_multi", enabled = hasSavedMacro && !isRecordingMacro) { openMenu = null; callbacks.onOpenMacroMultiple() }
                }
            }

            // ? (HELP / ABOUT) MENU
            MenuBarTrigger(
                title = stringResource(R.string.npp_menu_help),
                isOpen = openMenu == "help",
                textColor = chromeSubtleColor,
                tag = "menu_bar_help",
                onClick = { openMenu = "help" }
            ) {
                DropdownMenu(expanded = openMenu == "help", onDismissRequest = { openMenu = null }) {
                    DesktopMenuItem("About CodePad (Notepad++ Edition)…", "F1", "menu_help_about") {
                        openMenu = null
                        callbacks.onOpenAbout()
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuBarTrigger(
    title: String,
    isOpen: Boolean,
    textColor: Color,
    tag: String,
    onClick: () -> Unit,
    dropdown: @Composable () -> Unit
) {
    Box {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .background(
                    if (isOpen) Color(0x283B82F6) else Color.Transparent,
                    RoundedCornerShape(3.dp)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag(tag)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = textColor
            )
        }
        dropdown()
    }
}

@Composable
private fun DesktopMenuItem(
    label: String,
    shortcut: String,
    tag: String,
    enabled: Boolean = true,
    checked: Boolean = false,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        enabled = enabled,
        onClick = onClick,
        leadingIcon = {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(16.dp))
            }
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.bodySmall)
                if (shortcut.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(18.dp))
                    Text(
                        text = shortcut,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        modifier = Modifier
            .height(38.dp)
            .testTag(tag)
    )
}

@Composable
private fun NotepadClassicIconToolbar(
    hasActiveDoc: Boolean,
    isActiveDirty: Boolean,
    hasAnyDirty: Boolean,
    palette: EditorColorPalette,
    settings: EditorSettings,
    chromeTextColor: Color,
    borderColor: Color,
    canUndo: Boolean,
    canRedo: Boolean,
    isRecordingMacro: Boolean,
    hasSavedMacro: Boolean,
    callbacks: NotepadChromeCallbacks
) {
    Surface(
        color = palette.chromeToolbarBackground,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("npp_icon_toolbar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ToolbarIconBtn(Icons.Default.NoteAdd, R.string.cd_new_document, Color(0xFF16A34A), tag = "topbar_new_file_button", onClick = callbacks.onNewQuickTab)
            ToolbarIconBtn(Icons.Default.FolderOpen, R.string.cd_open_file, Color(0xFFD97706), tag = "toolbar_open", onClick = callbacks.onOpenLocalFile)
            ToolbarIconBtn(
                icon = Icons.Default.Save,
                cdRes = R.string.cd_save_document,
                tint = if (isActiveDirty) FloppyDirtyRed else FloppySavedBlue,
                enabled = hasActiveDoc,
                tag = "topbar_save_button",
                onClick = callbacks.onSave
            )
            ToolbarIconBtn(
                icon = Icons.Default.SaveAs,
                cdRes = R.string.cd_save_all,
                tint = if (hasAnyDirty) FloppyDirtyRed else FloppySavedBlue,
                tag = "toolbar_save_all",
                onClick = callbacks.onSaveAll
            )
            ToolbarIconBtn(Icons.Default.Close, R.string.cd_close_tab, FloppyDirtyRed, enabled = hasActiveDoc, tag = "toolbar_close_tab", onClick = callbacks.onCloseActiveTab)

            ToolbarSeparator(borderColor)

            ToolbarIconBtn(Icons.Default.ContentCut, R.string.cd_cut, chromeTextColor, enabled = hasActiveDoc, tag = "toolbar_cut", onClick = callbacks.onCut)
            ToolbarIconBtn(Icons.Default.ContentCopy, R.string.cd_copy, chromeTextColor, enabled = hasActiveDoc, tag = "toolbar_copy", onClick = callbacks.onCopy)
            ToolbarIconBtn(Icons.Default.ContentPaste, R.string.cd_paste, chromeTextColor, enabled = hasActiveDoc, tag = "toolbar_paste", onClick = callbacks.onPaste)

            ToolbarSeparator(borderColor)

            ToolbarIconBtn(Icons.AutoMirrored.Filled.Undo, R.string.cd_undo, Color(0xFF2563EB), enabled = canUndo, tag = "toolbar_undo", onClick = callbacks.onUndo)
            ToolbarIconBtn(Icons.AutoMirrored.Filled.Redo, R.string.cd_redo, Color(0xFF2563EB), enabled = canRedo, tag = "toolbar_redo", onClick = callbacks.onRedo)

            ToolbarSeparator(borderColor)

            ToolbarIconBtn(Icons.Default.Search, R.string.cd_find_replace, Color(0xFF0284C7), enabled = hasActiveDoc, tag = "topbar_find_button", onClick = { callbacks.onOpenFindTab(0) })
            ToolbarIconBtn(Icons.Default.FindReplace, R.string.cd_find_replace, Color(0xFF7C3AED), enabled = hasActiveDoc, tag = "toolbar_replace", onClick = { callbacks.onOpenFindTab(1) })

            ToolbarSeparator(borderColor)

            ToolbarIconBtn(Icons.Default.ZoomIn, R.string.cd_zoom_in, chromeTextColor, tag = "toolbar_zoom_in", onClick = callbacks.onZoomIn)
            ToolbarIconBtn(Icons.Default.ZoomOut, R.string.cd_zoom_out, chromeTextColor, tag = "toolbar_zoom_out", onClick = callbacks.onZoomOut)

            ToolbarSeparator(borderColor)

            ToolbarIconBtn(Icons.AutoMirrored.Filled.WrapText, R.string.cd_word_wrap, chromeTextColor, active = settings.wordWrap, tag = "toolbar_word_wrap", onClick = callbacks.onToggleWordWrap)
            ToolbarIconBtn(Icons.Default.SubdirectoryArrowLeft, R.string.cd_show_symbols, chromeTextColor, active = settings.showWhitespace, tag = "toolbar_show_chars", onClick = callbacks.onToggleWhitespace)
            ToolbarIconBtn(Icons.Default.FormatIndentIncrease, R.string.cd_indent_guide, chromeTextColor, active = settings.showIndentGuides, tag = "toolbar_indent_guide", onClick = callbacks.onToggleIndentGuides)
            ToolbarIconBtn(Icons.Default.Functions, R.string.cd_function_list, Color(0xFF0D9488), active = settings.showFunctionList, tag = "toolbar_func_list", onClick = callbacks.onToggleFunctionList)
            ToolbarIconBtn(Icons.Default.Map, R.string.cd_document_map, Color(0xFF4F46E5), active = settings.showDocumentMap, tag = "toolbar_doc_map", onClick = callbacks.onToggleDocumentMap)

            ToolbarSeparator(borderColor)

            ToolbarIconBtn(Icons.Default.FiberManualRecord, R.string.cd_macro_rec, FloppyDirtyRed, enabled = !isRecordingMacro, active = isRecordingMacro, tag = "toolbar_macro_rec", onClick = callbacks.onStartMacro)
            ToolbarIconBtn(Icons.Default.Stop, R.string.cd_macro_stop, chromeTextColor, enabled = isRecordingMacro, tag = "toolbar_macro_stop", onClick = callbacks.onStopMacro)
            ToolbarIconBtn(Icons.Default.PlayArrow, R.string.cd_macro_play, Color(0xFF16A34A), enabled = hasSavedMacro && !isRecordingMacro, tag = "toolbar_macro_play", onClick = callbacks.onPlayMacroOnce)
        }
    }
}

@Composable
private fun ToolbarSeparator(color: Color) {
    VerticalDivider(
        modifier = Modifier
            .fillMaxHeight(0.58f)
            .padding(horizontal = 3.dp),
        color = color
    )
}

@Composable
private fun ToolbarIconBtn(
    icon: ImageVector,
    cdRes: Int,
    tint: Color,
    enabled: Boolean = true,
    active: Boolean = false,
    tag: String,
    onClick: () -> Unit
) {
    val activeBg = Color(0x2E2563EB)
    val activeBorder = Color(0xFF3B82F6)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .then(
                if (active) {
                    Modifier
                        .background(activeBg, RoundedCornerShape(4.dp))
                        .border(1.dp, activeBorder, RoundedCornerShape(4.dp))
                } else {
                    Modifier
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(cdRes),
            tint = if (enabled) tint else tint.copy(alpha = 0.32f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Authentic Notepad++ Scintilla Tab Strip:
 * - Flush rectangular desktop tabs
 * - 3.dp Orange stripe at top of active tab
 * - Blue Floppy Disk when saved, Red Floppy Disk when modified/unsaved
 */
@Composable
fun NotepadScintillaTabBar(
    tabs: List<CodeDocument>,
    activeDocId: String?,
    dirtyDocIds: Set<String>,
    palette: EditorColorPalette,
    onSelectTab: (CodeDocument) -> Unit,
    onCloseTab: (CodeDocument) -> Unit,
    onCloseAllBut: (CodeDocument) -> Unit,
    onCloseAll: () -> Unit,
    onRenameActive: () -> Unit,
    onAddQuickTab: () -> Unit
) {
    val borderColor = if (palette.isDark) Color(0xFF3B4048) else Color(0xFFB8BDC4)
    val inactiveTabBg = if (palette.isDark) Color(0xFF21252B) else Color(0xFFE6E9EF)
    val tabTextColor = if (palette.isDark) Color(0xFFE5E7EB) else Color(0xFF111827)

    Surface(
        color = palette.chromeTabStripBackground,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("multi_tab_bar")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 4.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab.id == activeDocId
                    val isDirty = tab.id in dirtyDocIds
                    var showTabMenu by remember { mutableStateOf(false) }

                    Box {
                        Column(
                            modifier = Modifier
                                .widthIn(min = 104.dp, max = 210.dp)
                                .background(
                                    color = if (isSelected) palette.editorBackground else inactiveTabBg,
                                    shape = RectangleShape
                                )
                                .border(1.dp, borderColor, RectangleShape)
                                .clickable { onSelectTab(tab) }
                                .testTag("editor_tab_${tab.id}")
                        ) {
                            // Classic Notepad++ 3dp Orange Top Highlight Bar on active tab
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(
                                        if (isSelected) NotepadTabActiveStripe else Color.Transparent
                                    )
                            )

                            Row(
                                modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Iconic Blue / Red Floppy Disk status icon
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    tint = if (isDirty) FloppyDirtyRed else FloppySavedBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) palette.plainText else tabTextColor.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clickable { showTabMenu = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = stringResource(R.string.cd_more_options),
                                        tint = palette.gutterText,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { onCloseTab(tab) }
                                        .testTag("close_tab_${tab.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.cd_close_tab),
                                        tint = palette.gutterText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        DropdownMenu(expanded = showTabMenu, onDismissRequest = { showTabMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_close_tab)) },
                                onClick = { showTabMenu = false; onCloseTab(tab) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_close_others)) },
                                onClick = { showTabMenu = false; onCloseAllBut(tab) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_close_all)) },
                                onClick = { showTabMenu = false; onCloseAll() }
                            )
                            if (isSelected) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_rename_document)) },
                                    onClick = { showTabMenu = false; onRenameActive() }
                                )
                            }
                        }
                    }
                }

                // Double-click / '+' quick new tab button right next to tabs
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(bottom = 2.dp)
                        .clickable(onClick = onAddQuickTab)
                        .testTag("tab_bar_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_new_document),
                        tint = tabTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            HorizontalDivider(thickness = 1.dp, color = borderColor)
        }
    }
}
