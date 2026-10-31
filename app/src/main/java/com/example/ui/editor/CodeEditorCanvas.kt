package com.example.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Functions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CodeLanguage
import com.example.data.preferences.EditorSettings
import com.example.domain.CodeSyntaxVisualTransformation
import com.example.domain.FunctionSymbol
import com.example.domain.TextFxOperations
import com.example.ui.theme.EditorColorPalette
import com.example.ui.theme.JetBrainsMonoFamily

private data class LogicalLineAnchor(
    val lineNumber: Int,
    val topPx: Float,
    val bottomPx: Float,
    val rawLine: String,
    val isFoldStart: Boolean,
    val indentLevels: Int
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
    onSummaryClick: () -> Unit,
    onGoToLineClick: () -> Unit,
    onLineEndingClick: () -> Unit,
    onOvertypeClick: () -> Unit,
    onJumpToOffset: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val fontSize = settings.fontSizeSp.sp
    val lineHeight = (settings.fontSizeSp * 1.48f).sp
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

    val activeGutterStyle = remember(settings.fontSizeSp, palette.plainText) {
        gutterTextStyle.copy(
            color = palette.plainText,
            fontWeight = FontWeight.Bold
        )
    }

    val whitespaceBadgeStyle = remember(settings.fontSizeSp, palette.comment) {
        TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontSize = (settings.fontSizeSp - 3).coerceAtLeast(8).sp,
            color = Color(0xFFF97316).copy(alpha = 0.72f),
            fontWeight = FontWeight.SemiBold
        )
    }

    val visualTransformation = remember(
        language,
        palette,
        findReplaceState.matches,
        findReplaceState.activeMatchIndex,
        findReplaceState.markedRanges
    ) {
        CodeSyntaxVisualTransformation(
            language = language,
            palette = palette,
            searchMatches = findReplaceState.matches,
            activeMatchIndex = findReplaceState.activeMatchIndex,
            markedRanges = findReplaceState.markedRanges
        )
    }

    val (activeLineNumber, activeColumn) = remember(value.text, value.selection.start) {
        TextFxOperations.computeLineAndColumn(value.text, value.selection.start)
    }
    val selectedCharsCount = (value.selection.max - value.selection.min).coerceAtLeast(0)
    val rawLines = remember(value.text) { value.text.split('\n') }
    val totalLines = rawLines.size.coerceAtLeast(1)

    val parsedFunctions = remember(value.text, language, settings.showFunctionList) {
        if (settings.showFunctionList) {
            TextFxOperations.extractFunctions(value.text, language)
        } else {
            emptyList()
        }
    }

    val lineAnchors = remember(value.text, textLayoutResult, settings.tabSize) {
        val layout = textLayoutResult
        if (layout == null) {
            emptyList()
        } else {
            val anchors = ArrayList<LogicalLineAnchor>(totalLines)
            var currentOffset = 0
            val len = value.text.length
            for (idx in rawLines.indices) {
                val safeOff = currentOffset.coerceAtMost(len)
                val visualLine = runCatching { layout.getLineForOffset(safeOff) }.getOrDefault(0)
                val top = runCatching { layout.getLineTop(visualLine) }.getOrDefault(0f)
                val bottom = runCatching { layout.getLineBottom(visualLine) }.getOrDefault(top + 22f)
                val lineStr = rawLines[idx]
                val trimmed = lineStr.trimEnd()
                val isFold = trimmed.endsWith("{") || (language == CodeLanguage.PYTHON && trimmed.endsWith(":"))
                val leadingSpaces = lineStr.takeWhile { it == ' ' || it == '\t' }
                    .sumOf { if (it == '\t') settings.tabSize else 1 }
                val indentLvls = leadingSpaces / settings.tabSize.coerceAtLeast(2)

                anchors.add(
                    LogicalLineAnchor(
                        lineNumber = idx + 1,
                        topPx = top,
                        bottomPx = bottom,
                        rawLine = lineStr,
                        isFoldStart = isFold,
                        indentLevels = indentLvls
                    )
                )
                currentOffset += lineStr.length + 1
            }
            anchors
        }
    }

    val digitsCount = totalLines.toString().length.coerceAtLeast(2)
    val foldMarginWidthDp = 16.dp
    val lineNumWidthDp = with(density) {
        ((digitsCount * (settings.fontSizeSp * 0.64f)) + 16f).dp
    }
    val totalGutterWidth = lineNumWidthDp + foldMarginWidthDp
    val topBottomCanvasPadding = 8.dp
    val topPaddingPx = with(density) { topBottomCanvasPadding.toPx() }
    val charWidthPx = remember(settings.fontSizeSp, density) {
        textMeasurer.measure("M", codeTextStyle).size.width.toFloat().coerceAtLeast(7f)
    }

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
        // Middle Workspace Row: [Scintilla Gutter + Code Canvas] | [Document Map] | [Function List]
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(verticalScrollState)
                ) {
                    // Authentic Scintilla Line Number Gutter + Code Folding Margin
                    if (settings.showLineNumbers) {
                        val canvasHeightDp = with(density) {
                            val contentPx = textLayoutResult?.size?.height?.toFloat() ?: 240f
                            (contentPx.toDp() + topBottomCanvasPadding * 2).coerceAtLeast(320.dp)
                        }
                        Canvas(
                            modifier = Modifier
                                .width(totalGutterWidth)
                                .height(canvasHeightDp)
                                .background(palette.gutterBackground)
                                .testTag("editor_line_gutter")
                        ) {
                            val foldMarginPx = foldMarginWidthDp.toPx()
                            val numAreaWidth = size.width - foldMarginPx

                            // Fold margin column background
                            drawRect(
                                color = palette.foldMarginBackground,
                                topLeft = Offset(numAreaWidth, 0f),
                                size = Size(foldMarginPx, size.height)
                            )

                            // Vertical fold spine line & right border
                            val foldCenterX = numAreaWidth + foldMarginPx / 2f
                            drawLine(
                                color = palette.gutterText.copy(alpha = 0.25f),
                                start = Offset(foldCenterX, 0f),
                                end = Offset(foldCenterX, size.height),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = palette.gutterText.copy(alpha = 0.35f),
                                start = Offset(size.width - 1f, 0f),
                                end = Offset(size.width - 1f, size.height),
                                strokeWidth = 1f
                            )

                            if (lineAnchors.isEmpty()) {
                                val measured = textMeasurer.measure("1", activeGutterStyle)
                                drawText(
                                    textLayoutResult = measured,
                                    topLeft = Offset(
                                        x = (numAreaWidth - measured.size.width - 6.dp.toPx()).coerceAtLeast(2f),
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
                                            size = Size(numAreaWidth, anchor.bottomPx - anchor.topPx)
                                        )
                                    }
                                    val style = if (isCurrent) activeGutterStyle else gutterTextStyle
                                    val measured = textMeasurer.measure(anchor.lineNumber.toString(), style)
                                    drawText(
                                        textLayoutResult = measured,
                                        topLeft = Offset(
                                            x = (numAreaWidth - measured.size.width - 6.dp.toPx()).coerceAtLeast(2f),
                                            y = topPaddingPx + anchor.topPx + 1.dp.toPx()
                                        )
                                    )

                                    // Scintilla [-] square fold node on block headers
                                    if (anchor.isFoldStart) {
                                        val boxSize = 9.dp.toPx()
                                        val boxLeft = foldCenterX - boxSize / 2f
                                        val boxTop = topPaddingPx + anchor.topPx + ((anchor.bottomPx - anchor.topPx) - boxSize) / 2f
                                        drawRect(
                                            color = palette.editorBackground,
                                            topLeft = Offset(boxLeft, boxTop),
                                            size = Size(boxSize, boxSize)
                                        )
                                        drawRect(
                                            color = palette.gutterText,
                                            topLeft = Offset(boxLeft, boxTop),
                                            size = Size(boxSize, boxSize),
                                            style = Stroke(width = 1.1f)
                                        )
                                        drawLine(
                                            color = palette.gutterText,
                                            start = Offset(boxLeft + 2f, boxTop + boxSize / 2f),
                                            end = Offset(boxLeft + boxSize - 2f, boxTop + boxSize / 2f),
                                            strokeWidth = 1.2f
                                        )
                                    }
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

                    val dottedEffect = remember {
                        PathEffect.dashPathEffect(floatArrayOf(3f, 4f), 0f)
                    }

                    Box(
                        modifier = editorModifier
                            .heightIn(min = 340.dp)
                            .drawBehind {
                                val horizontalPadPx = 10.dp.toPx()
                                // 1. Notepad++ Current Line Highlight (#FFFBD6 in Classic PC)
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

                                // 2. Scintilla Dotted Vertical Indent Guides
                                if (settings.showIndentGuides && lineAnchors.isNotEmpty()) {
                                    val indentStepPx = charWidthPx * settings.tabSize
                                    lineAnchors.forEach { anchor ->
                                        for (lvl in 1..anchor.indentLevels.coerceAtMost(12)) {
                                            val guideX = horizontalPadPx + (lvl - 1) * indentStepPx + 2f
                                            drawLine(
                                                color = palette.gutterText.copy(alpha = 0.35f),
                                                start = Offset(guideX, topPaddingPx + anchor.topPx),
                                                end = Offset(guideX, topPaddingPx + anchor.bottomPx),
                                                strokeWidth = 1f,
                                                pathEffect = dottedEffect
                                            )
                                        }
                                    }
                                }

                                // 3. Show All Characters (Whitespace dots + CR LF / LF badges)
                                if (settings.showWhitespace && lineAnchors.isNotEmpty()) {
                                    val eolLabel = if (lineEnding == "CRLF") "CRLF" else "LF"
                                    val eolMeasured = textMeasurer.measure(eolLabel, whitespaceBadgeStyle)
                                    lineAnchors.take(250).forEach { anchor ->
                                        anchor.rawLine.forEachIndexed { charIdx, ch ->
                                            if (ch == ' ') {
                                                val cx = horizontalPadPx + charIdx * charWidthPx + charWidthPx * 0.45f
                                                val cy = topPaddingPx + (anchor.topPx + anchor.bottomPx) / 2f
                                                drawCircle(
                                                    color = Color(0xFFF97316).copy(alpha = 0.55f),
                                                    radius = 1.6f,
                                                    center = Offset(cx, cy)
                                                )
                                            }
                                        }
                                        val endX = horizontalPadPx + anchor.rawLine.length * charWidthPx + 4f
                                        drawText(
                                            textLayoutResult = eolMeasured,
                                            topLeft = Offset(endX, topPaddingPx + anchor.topPx + 2f)
                                        )
                                    }
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = topBottomCanvasPadding)
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
                                        Modifier.widthIn(min = 380.dp)
                                    }
                                )
                                .testTag("code_editor_input")
                        )
                    }
                }
            }

            // Notepad++ Document Map Minimap Panel
            if (settings.showDocumentMap) {
                VerticalDivider(color = palette.gutterText.copy(alpha = 0.3f))
                NotepadDocumentMap(
                    lines = rawLines,
                    activeLine = activeLineNumber,
                    palette = palette,
                    onJumpToLine = { lineOneBased ->
                        onJumpToOffset(TextFxOperations.offsetForLine(value.text, lineOneBased))
                    }
                )
            }

            // Notepad++ Function List Side Panel
            if (settings.showFunctionList) {
                VerticalDivider(color = palette.gutterText.copy(alpha = 0.35f))
                NotepadFunctionListPane(
                    symbols = parsedFunctions,
                    palette = palette,
                    onSelectSymbol = { sym -> onJumpToOffset(sym.charOffset) }
                )
            }
        }

        // Optional Mobile Developer Symbol Helper Bar
        if (settings.showSymbolBar) {
            QuickSymbolBar(
                canUndo = canUndo,
                canRedo = canRedo,
                palette = palette,
                onUndo = onUndo,
                onRedo = onRedo,
                onInsertSymbol = onInsertSymbol
            )
        }

        // Authentic Segmented PC Notepad++ Status Bar
        NotepadClassicStatusBar(
            language = language,
            palette = palette,
            charCount = value.text.length,
            lineCount = totalLines,
            line = activeLineNumber,
            column = activeColumn,
            selCount = selectedCharsCount,
            lineEnding = lineEnding,
            overtypeMode = settings.overtypeMode,
            saveStatus = saveStatus,
            onLanguageClick = onLanguageBadgeClick,
            onSummaryClick = onSummaryClick,
            onGoToLineClick = onGoToLineClick,
            onLineEndingClick = onLineEndingClick,
            onOvertypeClick = onOvertypeClick
        )
    }
}

@Composable
private fun NotepadDocumentMap(
    lines: List<String>,
    activeLine: Int,
    palette: EditorColorPalette,
    onJumpToLine: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .fillMaxHeight()
            .background(palette.gutterBackground.copy(alpha = 0.75f))
            .clickable {
                val mid = (lines.size / 2).coerceAtLeast(1)
                onJumpToLine(mid)
            }
            .padding(4.dp)
            .testTag("document_map_panel")
    ) {
        Text(
            text = "MAP",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = palette.gutterText,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxLinesDrawn = lines.size.coerceIn(1, 180)
            val rowH = (size.height / maxLinesDrawn.coerceAtLeast(30)).coerceIn(2f, 4.5f)
            for (i in 0 until maxLinesDrawn) {
                val ln = lines[i]
                val y = i * rowH
                if ((i + 1) in (activeLine - 4)..(activeLine + 4)) {
                    drawRect(
                        color = Color(0x33F97316),
                        topLeft = Offset(0f, y),
                        size = Size(size.width, rowH)
                    )
                }
                val indent = ln.takeWhile { it == ' ' || it == '\t' }.length.coerceAtMost(16)
                val codeLen = ln.trim().length.coerceAtMost(40)
                if (codeLen > 0) {
                    val xStart = (indent * 1.2f).coerceAtMost(size.width * 0.4f)
                    val barW = (codeLen * 1.3f).coerceAtMost(size.width - xStart)
                    drawLine(
                        color = if (i + 1 == activeLine) palette.keyword else palette.gutterText.copy(alpha = 0.65f),
                        start = Offset(xStart, y + rowH / 2f),
                        end = Offset(xStart + barW, y + rowH / 2f),
                        strokeWidth = 1.6f
                    )
                }
            }
        }
    }
}

@Composable
private fun NotepadFunctionListPane(
    symbols: List<FunctionSymbol>,
    palette: EditorColorPalette,
    onSelectSymbol: (FunctionSymbol) -> Unit
) {
    Column(
        modifier = Modifier
            .width(172.dp)
            .fillMaxHeight()
            .background(palette.chromeMenuBackground)
            .testTag("function_list_panel")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.chromeToolbarBackground)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Functions,
                contentDescription = null,
                tint = palette.keyword,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "Function List (${symbols.size})",
                style = MaterialTheme.typography.labelSmall,
                color = palette.plainText,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        HorizontalDivider(color = palette.gutterText.copy(alpha = 0.3f))
        if (symbols.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = "No functions/classes detected in buffer.",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.gutterText
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(symbols, key = { "${it.lineNumber}_${it.name}" }) { sym ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSymbol(sym) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = sym.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.plainText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = ":${sym.lineNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.gutterText
                        )
                    }
                }
            }
        }
    }
}

private val quickSymbols = listOf(
    "TAB", "{", "}", "(", ")", "[", "]", "<", ">", "=", ";", ":", "\"", "'", "/", "+", "-", "_", "!", "&", "|"
)

@Composable
private fun QuickSymbolBar(
    canUndo: Boolean,
    canRedo: Boolean,
    palette: EditorColorPalette,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onInsertSymbol: (String) -> Unit
) {
    val borderColor = palette.gutterText.copy(alpha = 0.32f)
    Surface(
        color = palette.chromeToolbarBackground,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_symbol_bar")
    ) {
        Column {
            HorizontalDivider(color = borderColor)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.size(36.dp).testTag("symbol_bar_undo")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.cd_undo),
                        tint = if (canUndo) palette.plainText else palette.gutterText.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.size(36.dp).testTag("symbol_bar_redo")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = stringResource(R.string.cd_redo),
                        tint = if (canRedo) palette.plainText else palette.gutterText.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(0.6f).padding(horizontal = 4.dp),
                    color = borderColor
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickSymbols.forEach { sym ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(30.dp)
                                .background(palette.editorBackground, RoundedCornerShape(4.dp))
                                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                                .clickable { onInsertSymbol(sym) }
                                .padding(horizontal = 10.dp)
                                .testTag("symbol_key_$sym")
                        ) {
                            Text(
                                text = if (sym == "TAB") "⇥ Tab" else sym,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.plainText
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Classic Segmented Notepad++ Bottom Status Bar:
 * [ Kotlin source file ] | [ length : 245   lines : 12 ] | [ Ln : 4   Col : 10   Sel : 0 ] | [ Unix (LF) ] | [ UTF-8 ] | [ INS ]
 */
@Composable
private fun NotepadClassicStatusBar(
    language: CodeLanguage,
    palette: EditorColorPalette,
    charCount: Int,
    lineCount: Int,
    line: Int,
    column: Int,
    selCount: Int,
    lineEnding: String,
    overtypeMode: Boolean,
    saveStatus: SaveStatus,
    onLanguageClick: () -> Unit,
    onSummaryClick: () -> Unit,
    onGoToLineClick: () -> Unit,
    onLineEndingClick: () -> Unit,
    onOvertypeClick: () -> Unit
) {
    val textColor = if (palette.isDark) Color(0xFFD1D5DB) else Color(0xFF1F2937)
    val sepColor = if (palette.isDark) Color(0xFF3B4048) else Color(0xFFC4C8CE)
    val eolDisplay = if (lineEnding == "CRLF") "Windows (CR LF)" else "Unix (LF)"
    val syncDot = when (saveStatus) {
        SaveStatus.SAVED -> "● Cloud Saved"
        SaveStatus.SAVING -> "↻ Saving…"
        SaveStatus.UNSAVED -> "● Modified"
    }

    Surface(
        color = palette.chromeStatusBackground,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("editor_status_bar")
    ) {
        Column {
            HorizontalDivider(thickness = 1.dp, color = sepColor)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusCell(
                    text = language.nppStatusDescription,
                    textColor = textColor,
                    tag = "status_language_selector",
                    onClick = onLanguageClick
                )
                StatusSep(sepColor)
                StatusCell(
                    text = "length : $charCount    lines : $lineCount",
                    textColor = textColor,
                    tag = "status_length_lines",
                    onClick = onSummaryClick
                )
                StatusSep(sepColor)
                StatusCell(
                    text = "Ln : $line    Col : $column    Sel : $selCount",
                    textColor = textColor,
                    tag = "status_ln_col",
                    onClick = onGoToLineClick
                )
                StatusSep(sepColor)
                StatusCell(
                    text = eolDisplay,
                    textColor = textColor,
                    tag = "status_eol_cell",
                    onClick = onLineEndingClick
                )
                StatusSep(sepColor)
                StatusCell(
                    text = "UTF-8",
                    textColor = textColor,
                    tag = "status_encoding_cell",
                    onClick = {}
                )
                StatusSep(sepColor)
                StatusCell(
                    text = if (overtypeMode) "OVR" else "INS",
                    textColor = textColor,
                    tag = "status_ins_ovr_cell",
                    onClick = onOvertypeClick
                )
                StatusSep(sepColor)
                Text(
                    text = syncDot,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = textColor.copy(alpha = 0.78f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusCell(
    text: String,
    textColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
            .testTag(tag)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
private fun StatusSep(color: Color) {
    VerticalDivider(
        modifier = Modifier.fillMaxHeight(0.72f),
        thickness = 1.dp,
        color = color
    )
}
