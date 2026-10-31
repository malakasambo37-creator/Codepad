package com.example.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatIndentDecrease
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CodeDocument
import com.example.data.model.CodeLanguage
import com.example.data.preferences.EditorSettings
import com.example.domain.DocumentSummaryStats
import com.example.domain.TextToolAction
import com.example.ui.theme.EditorPalettes
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewOrRenameDocumentDialog(
    isRenameMode: Boolean,
    initialTitle: String,
    initialLanguage: CodeLanguage,
    onDismiss: () -> Unit,
    onConfirm: (title: String, language: CodeLanguage, useStarter: Boolean) -> Unit
) {
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var selectedLanguage by remember(initialLanguage) { mutableStateOf(initialLanguage) }
    var useStarter by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (isRenameMode) R.string.dialog_rename_title else R.string.dialog_new_file_title
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { updated ->
                        title = updated
                        if (updated.contains('.')) {
                            val inferred = CodeLanguage.fromFileName(updated)
                            if (inferred != CodeLanguage.PLAINTEXT || updated.endsWith(".txt")) {
                                selectedLanguage = inferred
                            }
                        }
                    },
                    singleLine = true,
                    label = { Text(stringResource(R.string.label_file_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_filename_input")
                )

                Text(
                    text = stringResource(R.string.label_language),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CodeLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = selectedLanguage == lang,
                            onClick = {
                                selectedLanguage = lang
                                if (!isRenameMode && (title.isBlank() || title.startsWith("untitled.") || title.startsWith("new "))) {
                                    title = "untitled.${lang.defaultExtension}"
                                }
                            },
                            label = {
                                Text(lang.displayName, style = MaterialTheme.typography.bodySmall)
                            },
                            modifier = Modifier.testTag("lang_chip_${lang.id}")
                        )
                    }
                }

                if (!isRenameMode) {
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.label_template),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !useStarter,
                            onClick = { useStarter = false },
                            label = { Text(stringResource(R.string.template_blank)) },
                            modifier = Modifier.testTag("template_blank_chip")
                        )
                        FilterChip(
                            selected = useStarter,
                            onClick = { useStarter = true },
                            label = { Text(stringResource(R.string.template_starter)) },
                            modifier = Modifier.testTag("template_starter_chip")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.trim().ifEmpty {
                        "untitled.${selectedLanguage.defaultExtension}"
                    }
                    onConfirm(finalTitle, selectedLanguage, useStarter)
                },
                modifier = Modifier.testTag("dialog_confirm_file_button")
            ) {
                Text(stringResource(if (isRenameMode) R.string.btn_save else R.string.btn_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun GoToLineDialog(
    maxLines: Int,
    onDismiss: () -> Unit,
    onGoToLine: (Int) -> Unit
) {
    var lineInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_goto_line_title)) },
        text = {
            OutlinedTextField(
                value = lineInput,
                onValueChange = { lineInput = it.filter { ch -> ch.isDigit() } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text(stringResource(R.string.label_line_number, maxLines)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goto_line_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = lineInput.toIntOrNull()?.coerceIn(1, maxLines.coerceAtLeast(1)) ?: 1
                    onGoToLine(parsed)
                },
                modifier = Modifier.testTag("goto_line_confirm_button")
            ) {
                Text(stringResource(R.string.btn_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun DeleteDocumentDialog(
    doc: CodeDocument,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_delete_title)) },
        text = { Text(stringResource(R.string.dialog_delete_message, doc.title)) },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("confirm_delete_doc_button")
            ) {
                Text(stringResource(R.string.btn_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun DocumentSummaryDialog(
    doc: CodeDocument,
    stats: DocumentSummaryStats,
    onDismiss: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val createdStr = doc.createdAt?.toDate()?.let(fmt::format) ?: "—"
    val modifiedStr = doc.updatedAt?.toDate()?.let(fmt::format) ?: "—"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_summary_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SummaryRow("Document name:", doc.title)
                SummaryRow("Type:", CodeLanguage.fromId(doc.language).nppStatusDescription)
                SummaryRow("Created:", createdStr)
                SummaryRow("Modified:", modifiedStr)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SummaryRow("Characters (without blanks):", stats.charWithoutSpaces.toString())
                SummaryRow("Total characters:", stats.charCount.toString())
                SummaryRow("Words:", stats.wordCount.toString())
                SummaryRow("Lines:", stats.lineCount.toString())
                SummaryRow("Current document length (UTF-8):", "${stats.byteLengthUtf8} bytes")
                SummaryRow("Selected text:", "${stats.selectedChars} chars (${stats.selectedLines} lines)")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.testTag("summary_ok_button")) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun RunMacroMultipleDialog(
    onDismiss: () -> Unit,
    onRunMacro: (Int) -> Unit
) {
    var countText by remember { mutableStateOf("5") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_macro_multi_title)) },
        text = {
            OutlinedTextField(
                value = countText,
                onValueChange = { countText = it.filter(Char::isDigit) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text(stringResource(R.string.label_macro_times)) },
                modifier = Modifier.fillMaxWidth().testTag("macro_times_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val times = countText.toIntOrNull()?.coerceIn(1, 100) ?: 1
                    onRunMacro(times)
                },
                modifier = Modifier.testTag("macro_run_confirm_button")
            ) {
                Text(stringResource(R.string.btn_run_macro))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun AboutNotepadDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_about_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CodePad — Native Android Scintilla-Style Code & Text Editor",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Dirancang dengan antarmuka klasik Notepad++ PC: Menu Bar lengkap, Toolbar ikon cepat, Tab Disket Biru/Merah, Gutter + Code Folding Margin, Find/Replace/Mark (Normal, Extended, Regex), Macro Recorder, Document Map, Function List, dan sinkronisasi Cloud otomatis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFxBottomSheet(
    onDismiss: () -> Unit,
    onApplyTool: (TextToolAction) -> Unit,
    onToggleLineEnding: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.textfx_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.textfx_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            TextFxItem(Icons.Default.TextFields, R.string.tool_uppercase, "tool_uppercase") {
                onApplyTool(TextToolAction.UPPERCASE); onDismiss()
            }
            TextFxItem(Icons.Default.TextFields, R.string.tool_lowercase, "tool_lowercase") {
                onApplyTool(TextToolAction.LOWERCASE); onDismiss()
            }
            TextFxItem(Icons.Default.TextFields, R.string.tool_proper_case, "tool_proper_case") {
                onApplyTool(TextToolAction.PROPER_CASE); onDismiss()
            }
            TextFxItem(Icons.Default.TextFields, R.string.tool_invert_case, "tool_invert_case") {
                onApplyTool(TextToolAction.INVERT_CASE); onDismiss()
            }
            TextFxItem(Icons.Default.ContentCopy, R.string.tool_duplicate_line, "tool_dup_line") {
                onApplyTool(TextToolAction.DUPLICATE_CURRENT_LINE); onDismiss()
            }
            TextFxItem(Icons.Default.DeleteSweep, R.string.tool_delete_line, "tool_del_line") {
                onApplyTool(TextToolAction.DELETE_CURRENT_LINE); onDismiss()
            }
            TextFxItem(Icons.Default.ArrowUpward, R.string.tool_move_line_up, "tool_move_up") {
                onApplyTool(TextToolAction.MOVE_LINE_UP); onDismiss()
            }
            TextFxItem(Icons.Default.ArrowDownward, R.string.tool_move_line_down, "tool_move_down") {
                onApplyTool(TextToolAction.MOVE_LINE_DOWN); onDismiss()
            }
            TextFxItem(Icons.AutoMirrored.Filled.Comment, R.string.tool_toggle_comment, "tool_comment") {
                onApplyTool(TextToolAction.TOGGLE_LINE_COMMENT); onDismiss()
            }
            TextFxItem(Icons.Default.FormatIndentIncrease, R.string.tool_indent_lines, "tool_indent") {
                onApplyTool(TextToolAction.INDENT_LINES); onDismiss()
            }
            TextFxItem(Icons.Default.FormatIndentDecrease, R.string.tool_unindent_lines, "tool_unindent") {
                onApplyTool(TextToolAction.UNINDENT_LINES); onDismiss()
            }
            TextFxItem(Icons.AutoMirrored.Filled.Sort, R.string.tool_sort_asc, "tool_sort_asc") {
                onApplyTool(TextToolAction.SORT_LINES_ASC); onDismiss()
            }
            TextFxItem(Icons.AutoMirrored.Filled.Sort, R.string.tool_sort_desc, "tool_sort_desc") {
                onApplyTool(TextToolAction.SORT_LINES_DESC); onDismiss()
            }
            TextFxItem(Icons.Default.FilterList, R.string.tool_dedupe_lines, "tool_dedupe") {
                onApplyTool(TextToolAction.REMOVE_DUPLICATE_LINES); onDismiss()
            }
            TextFxItem(Icons.Default.CleaningServices, R.string.tool_remove_empty, "tool_remove_empty") {
                onApplyTool(TextToolAction.REMOVE_EMPTY_LINES); onDismiss()
            }
            TextFxItem(Icons.Default.SpaceBar, R.string.tool_trim_trailing, "tool_trim_trailing") {
                onApplyTool(TextToolAction.TRIM_TRAILING_SPACES); onDismiss()
            }
            TextFxItem(Icons.Default.AccessTime, R.string.tool_insert_datetime, "tool_datetime") {
                onApplyTool(TextToolAction.INSERT_DATETIME); onDismiss()
            }
            TextFxItem(Icons.Default.DataObject, R.string.tool_json_pretty, "tool_json_pretty") {
                onApplyTool(TextToolAction.FORMAT_JSON_PRETTY); onDismiss()
            }
            TextFxItem(Icons.Default.DataObject, R.string.tool_json_minify, "tool_json_minify") {
                onApplyTool(TextToolAction.JSON_MINIFY); onDismiss()
            }
            TextFxItem(Icons.Default.EnhancedEncryption, R.string.tool_base64_encode, "tool_b64_enc") {
                onApplyTool(TextToolAction.BASE64_ENCODE); onDismiss()
            }
            TextFxItem(Icons.Default.LockOpen, R.string.tool_base64_decode, "tool_b64_dec") {
                onApplyTool(TextToolAction.BASE64_DECODE); onDismiss()
            }
            TextFxItem(Icons.AutoMirrored.Filled.KeyboardReturn, R.string.tool_toggle_line_ending, "tool_eol") {
                onToggleLineEnding(); onDismiss()
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TextFxItem(
    icon: ImageVector,
    labelRes: Int,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSettingsBottomSheet(
    settings: EditorSettings,
    onDismiss: () -> Unit,
    onSelectTheme: (String) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onWordWrapChange: (Boolean) -> Unit,
    onLineNumbersChange: (Boolean) -> Unit,
    onWhitespaceChange: (Boolean) -> Unit,
    onIndentGuidesChange: (Boolean) -> Unit,
    onDocumentMapChange: (Boolean) -> Unit,
    onFunctionListChange: (Boolean) -> Unit,
    onSymbolBarChange: (Boolean) -> Unit,
    onTabSizeChange: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.settings_theme_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EditorPalettes.all.forEach { palette ->
                    val selected = settings.editorThemeId == palette.id
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.editorBackground,
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable { onSelectTheme(palette.id) }
                            .testTag("theme_option_${palette.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = palette.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.plainText
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    palette.keyword,
                                    palette.stringLiteral,
                                    palette.comment,
                                    palette.functionOrType
                                ).forEach { swatch ->
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(swatch, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            Column {
                Text(
                    text = stringResource(R.string.settings_font_size, settings.fontSizeSp),
                    style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = settings.fontSizeSp.toFloat(),
                    onValueChange = { onFontSizeChange(it.roundToInt()) },
                    valueRange = 9f..26f,
                    steps = 16,
                    modifier = Modifier.testTag("settings_font_slider")
                )
            }

            SettingsSwitchRow(
                label = stringResource(R.string.settings_word_wrap),
                checked = settings.wordWrap,
                onCheckedChange = onWordWrapChange,
                tag = "switch_word_wrap"
            )
            SettingsSwitchRow(
                label = stringResource(R.string.settings_line_numbers),
                checked = settings.showLineNumbers,
                onCheckedChange = onLineNumbersChange,
                tag = "switch_line_numbers"
            )
            SettingsSwitchRow(
                label = stringResource(R.string.settings_whitespace),
                checked = settings.showWhitespace,
                onCheckedChange = onWhitespaceChange,
                tag = "switch_whitespace"
            )
            SettingsSwitchRow(
                label = stringResource(R.string.settings_indent_guides),
                checked = settings.showIndentGuides,
                onCheckedChange = onIndentGuidesChange,
                tag = "switch_indent_guides"
            )
            SettingsSwitchRow(
                label = stringResource(R.string.settings_doc_map),
                checked = settings.showDocumentMap,
                onCheckedChange = onDocumentMapChange,
                tag = "switch_doc_map"
            )
            SettingsSwitchRow(
                label = stringResource(R.string.settings_func_list),
                checked = settings.showFunctionList,
                onCheckedChange = onFunctionListChange,
                tag = "switch_func_list"
            )
            SettingsSwitchRow(
                label = stringResource(R.string.settings_symbol_bar),
                checked = settings.showSymbolBar,
                onCheckedChange = onSymbolBarChange,
                tag = "switch_symbol_bar"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings_tab_size),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.tabSize == 2,
                        onClick = { onTabSizeChange(2) },
                        label = { Text("2 Spaces") },
                        modifier = Modifier.testTag("tab_size_2_chip")
                    )
                    FilterChip(
                        selected = settings.tabSize == 4,
                        onClick = { onTabSizeChange(4) },
                        label = { Text("4 Spaces") },
                        modifier = Modifier.testTag("tab_size_4_chip")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
