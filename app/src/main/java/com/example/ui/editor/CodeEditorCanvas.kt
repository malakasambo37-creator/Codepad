package com.example.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CodeLanguage
import com.example.data.preferences.EditorSettings
import com.example.domain.CodeSyntaxVisualTransformation
import com.example.domain.TextFxOperations
import com.example.ui.theme.EditorColorPalette
import com.example.ui.theme.JetBrainsMonoFamily

private data class LogicalLineAnchor(
    val lineNumber: Int,
    val topPx: Float,
    val bottomPx: Float
)

@Composable
fun CodeEditorCanvas(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    language: CodeLanguage,
    palette: EditorColorPalette,
    settings: EditorSettings,
    findReplaceState: FindReplaceState,
    saveStatus: SaveStatus,
    lineEnding: String,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onInsertSymbol: (String) -> Unit,
    onLanguageBadgeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val fontSize = settings.fontSizeSp.sp
    val lineHeight = (settings.fontSizeSp * 1.5f).sp
    val codeTextStyle = remember(settings.fontSizeSp, palette.plainText) {
        TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontSize = fontSize,
            lineHeight = lineHeight,
            color = palette.plainText
        )
    }

    val gutterTextStyle = remember(settings.fontSizeSp, palette.gutterText) {
        TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontSize = (settings.fontSizeSp - 1).coerceAtLeast(9).sp,
            lineHeight = lineHeight,
            color = palette.gutterText
        )
    }

    val activeGuttersStyle = remember(settings.fontSizeSp, palette.cursorColor) {
        gutterTextStyle.copy(
            color = palette.cursorColor,
            fontWeight = FontWeight.Bold
        )
    }

    val visualTransformation = remember(
        language,
        palette,
        findReplaceState.matches,
        findReplaceState.activeMatchIndex
    ) {
        CodeSyntaxVisualTransformation(
            language = language,
            palette = palette,
            searchMatches = findReplaceState.matches,
            activeMatchIndex = findReplaceState.activeMatchIndex
        )
    }

    val (activeLineNumber, activeColumn) = remember(value.text, value.selection.start) {
        TextFxOperations.computeLineAndColumn(value.text, value.selection.start)
    }
    val totalLines = remember(value.text) {
        TextFxOperations.countLines(value.text)
    }

    val lineAnchors = remember(value.text, textLayoutResult) {
        val layout = textLayoutResult
        if (layout == null) {
            emptyList()
        } else {
            val anchors = ArrayList<LogicalLineAnchor>(totalLines)
            var currentOffset = 0
            var lineNum = 1
            val len = value.text.length
            while (currentOffset <= len) {
                val safeOff = currentOffset.coerceAtMost(len)
                val visualLine = runCatching { layout.getLineForOffset(safeOff) }.getOrDefault(0)
                val top = runCatching { layout.getLineTop(visualLine) }.getOrDefault(0f)
                val bottom = runCatching { layout.getLineBottom(visualLine) }.getOrDefault(top + 24f)
                anchors.add(LogicalLineAnchor(lineNum, top, bottom))
                if (currentOffset == len) break
                val nextNewline = value.text.indexOf('\n', currentOffset)
                if (nextNewline == -1) break
                currentOffset = nextNewline + 1
                lineNum++
            }
            anchors
        }
    }

    val digitsCount = totalLines.toString().length.coerceAtLeast(2)
    val gutterWidth = with(density) {
        ((digitsCount * (settings.fontSizeSp * 0.65f)) + 20f).dp
    }
    val topBottomCanvasPadding = 12.dp
    val topPaddingPx = with(density) { topBottomCanvasPadding.toPx() }

    val selectionColors = remember(palette) {
        TextSelectionColors(
            handleColor = palette.cursorColor,
            backgroundColor = palette.selectionBackground
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.editorBackground)
    ) {
        // Main scrollable code area + synchronized line number gutter
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(verticalScrollState)
            ) {
                if (settings.showLineNumbers) {
                    val canvasHeightDp = with(density) {
                        val contentPx = textLayoutResult?.size?.height?.toFloat() ?: 200f
                        (contentPx.toDp() + topBottomCanvasPadding * 2).coerceAtLeast(240.dp)
                    }
                    Canvas(
                        modifier = Modifier
                            .width(gutterWidth)
                            .height(canvasHeightDp)
                            .background(palette.gutterBackground)
                            .testTag("editor_line_gutter")
                    ) {
                        // Subtle right separator line
                        drawLine(
                            color = palette.gutterText.copy(alpha = 0.22f),
                            start = Offset(size.width - 1f, 0f),
                            end = Offset(size.width - 1f, size.height),
                            strokeWidth = 1f
                        )

                        if (lineAnchors.isEmpty()) {
                            val measured = textMeasurer.measure("1", activeGuttersStyle)
                            drawText(
                                textLayoutResult = measured,
                                topLeft = Offset(
                                    x = (size.width - measured.size.width - 10.dp.toPx()).coerceAtLeast(4f),
                                    y = topPaddingPx
                                )
                            )
                        } else {
                            lineAnchors.forEach { anchor ->
                                val isCurrent = anchor.lineNumber == activeLineNumber
                                if (isCurrent) {
                                    drawRect(
                                        color = palette.activeLineBackground,
                                        topLeft = Offset(0f, topPaddingPx + anchor.topPx),
                                        size = Size(size.width - 1f, anchor.bottomPx - anchor.topPx)
                                    )
                                }
                                val style = if (isCurrent) activeGuttersStyle else gutterTextStyle
                                val measured = textMeasurer.measure(anchor.lineNumber.toString(), style)
                                drawText(
                                    textLayoutResult = measured,
                                    topLeft = Offset(
                                        x = (size.width - measured.size.width - 8.dp.toPx()).coerceAtLeast(4f),
                                        y = topPaddingPx + anchor.topPx + 1.dp.toPx()
                                    )
                                )
                            }
                        }
                    }
                }

                val editorModifier = if (settings.wordWrap) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScrollState)
                }

                Box(
                    modifier = editorModifier
                        .heightIn(min = 320.dp)
                        .drawBehind {
                            val activeAnchor = lineAnchors.firstOrNull { it.lineNumber == activeLineNumber }
                            if (activeAnchor != null) {
                                drawRect(
                                    color = palette.activeLineBackground,
                                    topLeft = Offset(0f, topPaddingPx + activeAnchor.topPx),
                                    size = Size(
                                        size.width,
                                        (activeAnchor.bottomPx - activeAnchor.topPx).coerceAtLeast(18f)
                                    )
                                )
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = topBottomCanvasPadding)
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = codeTextStyle,
                        cursorBrush = SolidColor(palette.cursorColor),
                        visualTransformation = visualTransformation,
                        onTextLayout = { result -> textLayoutResult = result },
                        modifier = Modifier
                            .then(
                                if (settings.wordWrap) {
                                    Modifier.fillMaxWidth()
                                } else {
                                    Modifier.widthIn(min = 360.dp)
                                }
                            )
                            .testTag("code_editor_input"),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (value.text.isEmpty()) {
                                    Text(
                                        text = "// Ketik kode atau catatan di sini…",
                                        style = codeTextStyle.copy(
                                            color = palette.gutterText.copy(alpha = 0.7f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }

        // Optional Quick Developer Symbol Bar
        if (settings.showSymbolBar) {
            QuickSymbolBar(
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = onUndo,
                onRedo = onRedo,
                onInsertSymbol = onInsertSymbol
            )
        }

        // Notepad++ Status Bar
        NotepadStatusBar(
            language = language,
            line = activeLineNumber,
            column = activeColumn,
            charCount = value.text.length,
            lineCount = totalLines,
            lineEnding = lineEnding,
            saveStatus = saveStatus,
            onLanguageClick = onLanguageBadgeClick
        )
    }
}

private val quickSymbols = listOf(
    "TAB", "{", "}", "(", ")", "[", "]", "<", ">", "=", ";", ":", "\"", "'", "/", "+", "-", "_", "!", "&", "|"
)

@Composable
private fun QuickSymbolBar(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onInsertSymbol: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_symbol_bar")
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.testTag("symbol_bar_undo")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.cd_undo),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.testTag("symbol_bar_redo")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = stringResource(R.string.cd_redo),
                        modifier = Modifier.size(20.dp)
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight(0.6f)
                        .padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.outline
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickSymbols.forEach { sym ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable { onInsertSymbol(sym) }
                                .testTag("symbol_key_$sym")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (sym == "TAB") "⇥ Tab" else sym,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotepadStatusBar(
    language: CodeLanguage,
    line: Int,
    column: Int,
    charCount: Int,
    lineCount: Int,
    lineEnding: String,
    saveStatus: SaveStatus,
    onLanguageClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("editor_status_bar")
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Clickable Language Selector Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = language.badgeColor.copy(alpha = 0.18f),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onLanguageClick)
                        .testTag("status_language_selector")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(language.badgeColor, RoundedCornerShape(50))
                        )
                        Text(
                            text = language.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "Ln $line, Col $column",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "$charCount chars | $lineCount lines",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "UTF-8  •  $lineEnding",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val (icon, labelRes, tint) = when (saveStatus) {
                        SaveStatus.SAVED -> Triple(
                            Icons.Default.CloudDone,
                            R.string.status_saved,
                            MaterialTheme.colorScheme.primary
                        )
                        SaveStatus.SAVING -> Triple(
                            Icons.Default.CloudSync,
                            R.string.status_saving,
                            MaterialTheme.colorScheme.secondary
                        )
                        SaveStatus.UNSAVED -> Triple(
                            Icons.Default.EditNote,
                            R.string.status_unsaved,
                            MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = tint
                    )
                }
            }
        }
    }
}
