package com.example.domain

import android.util.Base64
import androidx.compose.ui.text.TextRange
import com.example.data.model.CodeLanguage
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SearchMode {
    NORMAL,
    EXTENDED,
    REGEX
}

enum class TextToolAction {
    UPPERCASE,
    LOWERCASE,
    PROPER_CASE,
    INVERT_CASE,
    SORT_LINES_ASC,
    SORT_LINES_DESC,
    REMOVE_DUPLICATE_LINES,
    REMOVE_EMPTY_LINES,
    TRIM_TRAILING_SPACES,
    DUPLICATE_CURRENT_LINE,
    DELETE_CURRENT_LINE,
    MOVE_LINE_UP,
    MOVE_LINE_DOWN,
    TOGGLE_LINE_COMMENT,
    INDENT_LINES,
    UNINDENT_LINES,
    INSERT_DATETIME,
    FORMAT_JSON_PRETTY,
    JSON_MINIFY,
    BASE64_ENCODE,
    BASE64_DECODE
}

sealed interface TextToolOutcome {
    data class Transformed(val text: String, val selection: TextRange) : TextToolOutcome
    data class ValidationError(val errorCode: ErrorKind) : TextToolOutcome

    enum class ErrorKind {
        INVALID_JSON,
        INVALID_BASE64
    }
}

data class FunctionSymbol(
    val name: String,
    val kind: String,
    val lineNumber: Int,
    val charOffset: Int
)

data class DocumentSummaryStats(
    val charCount: Int,
    val charWithoutSpaces: Int,
    val byteLengthUtf8: Int,
    val wordCount: Int,
    val lineCount: Int,
    val selectedChars: Int,
    val selectedLines: Int
)

object TextFxOperations {

    fun decodeExtendedEscapes(raw: String): String {
        val sb = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            if (raw[i] == '\\' && i + 1 < raw.length) {
                when (raw[i + 1]) {
                    'n' -> { sb.append('\n'); i += 2; continue }
                    'r' -> { sb.append('\r'); i += 2; continue }
                    't' -> { sb.append('\t'); i += 2; continue }
                    '0' -> { sb.append('\u0000'); i += 2; continue }
                    '\\' -> { sb.append('\\'); i += 2; continue }
                }
            }
            sb.append(raw[i])
            i++
        }
        return sb.toString()
    }

    fun findMatches(
        text: String,
        query: String,
        matchCase: Boolean,
        useRegex: Boolean
    ): Result<List<IntRange>> {
        val mode = if (useRegex) SearchMode.REGEX else SearchMode.NORMAL
        return findMatchesWithMode(text, query, matchCase, mode)
    }

    fun findMatchesWithMode(
        text: String,
        query: String,
        matchCase: Boolean,
        mode: SearchMode
    ): Result<List<IntRange>> {
        if (query.isEmpty() || text.isEmpty()) return Result.success(emptyList())

        return try {
            val options = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
            val regex = when (mode) {
                SearchMode.REGEX -> Regex(query, options)
                SearchMode.EXTENDED -> {
                    val decoded = decodeExtendedEscapes(query)
                    if (decoded.isEmpty()) return Result.success(emptyList())
                    Regex(Regex.escape(decoded), options)
                }
                SearchMode.NORMAL -> Regex(Regex.escape(query), options)
            }

            val matches = regex.findAll(text)
                .filter { !it.range.isEmpty() }
                .take(2500)
                .map { it.range }
                .toList()
            Result.success(matches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun replaceOne(
        text: String,
        targetRange: IntRange,
        replacement: String,
        query: String,
        matchCase: Boolean,
        useRegex: Boolean,
        mode: SearchMode = if (useRegex) SearchMode.REGEX else SearchMode.NORMAL
    ): Pair<String, Int> {
        val start = targetRange.first.coerceIn(0, text.length)
        val endExclusive = (targetRange.last + 1).coerceIn(start, text.length)
        val matchedSubstring = text.substring(start, endExclusive)

        val replacedSegment = when (mode) {
            SearchMode.REGEX -> runCatching {
                val opts = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
                Regex(query, opts).replaceFirst(matchedSubstring, replacement)
            }.getOrDefault(replacement)
            SearchMode.EXTENDED -> decodeExtendedEscapes(replacement)
            SearchMode.NORMAL -> replacement
        }

        val updated = buildString(text.length - (endExclusive - start) + replacedSegment.length) {
            append(text, 0, start)
            append(replacedSegment)
            append(text, endExclusive, text.length)
        }
        val newCursor = (start + replacedSegment.length).coerceIn(0, updated.length)
        return updated to newCursor
    }

    fun replaceAll(
        text: String,
        query: String,
        replacement: String,
        matchCase: Boolean,
        useRegex: Boolean,
        mode: SearchMode = if (useRegex) SearchMode.REGEX else SearchMode.NORMAL
    ): Result<Pair<String, Int>> {
        if (query.isEmpty()) return Result.success(text to 0)
        return try {
            val opts = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
            val regex = when (mode) {
                SearchMode.REGEX -> Regex(query, opts)
                SearchMode.EXTENDED -> Regex(Regex.escape(decodeExtendedEscapes(query)), opts)
                SearchMode.NORMAL -> Regex(Regex.escape(query), opts)
            }
            val count = regex.findAll(text).filter { !it.range.isEmpty() }.count()
            if (count == 0) return Result.success(text to 0)

            val updated = when (mode) {
                SearchMode.REGEX -> regex.replace(text, replacement)
                SearchMode.EXTENDED -> {
                    regex.replace(text, Regex.escapeReplacement(decodeExtendedEscapes(replacement)))
                }
                SearchMode.NORMAL -> regex.replace(text, Regex.escapeReplacement(replacement))
            }
            Result.success(updated to count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun computeLineAndColumn(text: String, cursorOffset: Int): Pair<Int, Int> {
        val safeOffset = cursorOffset.coerceIn(0, text.length)
        var line = 1
        var lastNewlineIndex = -1
        for (i in 0 until safeOffset) {
            if (text[i] == '\n') {
                line++
                lastNewlineIndex = i
            }
        }
        val col = safeOffset - lastNewlineIndex
        return line to col
    }

    fun countLines(text: String): Int {
        if (text.isEmpty()) return 1
        var count = 1
        for (ch in text) {
            if (ch == '\n') count++
        }
        return count
    }

    fun offsetForLine(text: String, targetLineOneBased: Int): Int {
        if (targetLineOneBased <= 1 || text.isEmpty()) return 0
        var currentLine = 1
        for (i in text.indices) {
            if (text[i] == '\n') {
                currentLine++
                if (currentLine == targetLineOneBased) {
                    return (i + 1).coerceAtMost(text.length)
                }
            }
        }
        return text.length
    }

    fun computeDocumentSummary(text: String, selection: TextRange): DocumentSummaryStats {
        val selMin = selection.min.coerceIn(0, text.length)
        val selMax = selection.max.coerceIn(selMin, text.length)
        val selectedText = if (selMax > selMin) text.substring(selMin, selMax) else ""
        val wordRegex = Regex("""\S+""")
        return DocumentSummaryStats(
            charCount = text.length,
            charWithoutSpaces = text.count { !it.isWhitespace() },
            byteLengthUtf8 = text.toByteArray(Charsets.UTF_8).size,
            wordCount = wordRegex.findAll(text).count(),
            lineCount = countLines(text),
            selectedChars = selectedText.length,
            selectedLines = if (selectedText.isEmpty()) 0 else countLines(selectedText)
        )
    }

    fun extractFunctions(text: String, language: CodeLanguage): List<FunctionSymbol> {
        if (text.isBlank()) return emptyList()
        val lines = text.split('\n')
        val results = mutableListOf<FunctionSymbol>()
        var runningOffset = 0

        val symbolRegex = when (language) {
            CodeLanguage.KOTLIN -> Regex("""^\s*(?:(?:private|public|internal|protected|suspend|inline|override|open|data|sealed)\s+)*(fun|class|interface|object)\s+([A-Za-z_][A-Za-z0-9_]*)""")
            CodeLanguage.JAVA -> Regex("""^\s*(?:(?:public|private|protected|static|final|abstract|synchronized)\s+)*(?:(class|interface|enum|record)\s+([A-Za-z_][A-Za-z0-9_]*)|[A-Za-z0-9_<>\[\]]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*\()""")
            CodeLanguage.PYTHON -> Regex("""^\s*(?:async\s+)?(def|class)\s+([A-Za-z_][A-Za-z0-9_]*)""")
            CodeLanguage.JAVASCRIPT -> Regex("""^\s*(?:export\s+)?(?:default\s+)?(?:async\s+)?(?:(function|class|interface|type)\s+([A-Za-z_$][A-Za-z0-9_$]*)|(?:const|let|var)\s+([A-Za-z_$][A-Za-z0-9_$]*)\s*=\s*(?:async\s*)?\()""")
            CodeLanguage.CPP -> Regex("""^\s*(?:(class|struct|namespace)\s+([A-Za-z_][A-Za-z0-9_]*)|[A-Za-z_][A-Za-z0-9_:<>*&\s]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*\([^;]*$)""")
            CodeLanguage.MARKDOWN -> Regex("""^(#{1,4})\s+(.+)$""")
            CodeLanguage.HTML -> Regex("""^\s*<(h[1-6]|section|main|header|footer|nav|script|style)\b[^>]*>""", RegexOption.IGNORE_CASE)
            CodeLanguage.SQL -> Regex("""^\s*(CREATE\s+(?:TABLE|VIEW|FUNCTION|PROCEDURE|INDEX))\s+([A-Za-z0-9_."]+)""", RegexOption.IGNORE_CASE)
            else -> Regex("""^\s*(fun|function|def|class)\s+([A-Za-z_][A-Za-z0-9_]*)""")
        }

        lines.forEachIndexed { idx, line ->
            val match = symbolRegex.find(line)
            if (match != null) {
                val groups = match.groupValues.drop(1).filter { it.isNotBlank() }
                if (groups.isNotEmpty()) {
                    val kind = if (groups.size >= 2) groups[0].trim() else "sym"
                    val name = groups.last().trim()
                    if (name !in setOf("if", "for", "while", "switch", "catch", "when")) {
                        results.add(
                            FunctionSymbol(
                                name = name,
                                kind = kind,
                                lineNumber = idx + 1,
                                charOffset = runningOffset
                            )
                        )
                    }
                }
            }
            runningOffset += line.length + 1
        }
        return results
    }

    fun applyTextTool(
        action: TextToolAction,
        text: String,
        selection: TextRange,
        language: CodeLanguage = CodeLanguage.PLAINTEXT,
        tabSize: Int = 4
    ): TextToolOutcome {
        val selMin = selection.min.coerceIn(0, text.length)
        val selMax = selection.max.coerceIn(selMin, text.length)
        val hasSelection = selMax > selMin

        return when (action) {
            TextToolAction.UPPERCASE ->
                transformSelectionOrAll(text, selMin, selMax, hasSelection) { it.uppercase() }

            TextToolAction.LOWERCASE ->
                transformSelectionOrAll(text, selMin, selMax, hasSelection) { it.lowercase() }

            TextToolAction.PROPER_CASE ->
                transformSelectionOrAll(text, selMin, selMax, hasSelection) { src ->
                    src.split(' ').joinToString(" ") { w ->
                        w.lowercase().replaceFirstChar { it.titlecase() }
                    }
                }

            TextToolAction.INVERT_CASE ->
                transformSelectionOrAll(text, selMin, selMax, hasSelection) { src ->
                    buildString(src.length) {
                        for (c in src) {
                            append(if (c.isUpperCase()) c.lowercaseChar() else c.uppercaseChar())
                        }
                    }
                }

            TextToolAction.SORT_LINES_ASC -> {
                val sorted = text.split('\n').sorted().joinToString("\n")
                TextToolOutcome.Transformed(sorted, TextRange(sorted.length.coerceAtMost(selMin)))
            }

            TextToolAction.SORT_LINES_DESC -> {
                val sorted = text.split('\n').sortedDescending().joinToString("\n")
                TextToolOutcome.Transformed(sorted, TextRange(sorted.length.coerceAtMost(selMin)))
            }

            TextToolAction.REMOVE_DUPLICATE_LINES -> {
                val deduped = text.split('\n').distinct().joinToString("\n")
                TextToolOutcome.Transformed(deduped, TextRange(deduped.length.coerceAtMost(selMin)))
            }

            TextToolAction.REMOVE_EMPTY_LINES -> {
                val cleaned = text.split('\n').filter { it.isNotBlank() }.joinToString("\n")
                TextToolOutcome.Transformed(cleaned, TextRange(cleaned.length.coerceAtMost(selMin)))
            }

            TextToolAction.TRIM_TRAILING_SPACES -> {
                val trimmed = text.split('\n').joinToString("\n") { it.trimEnd(' ', '\t', '\r') }
                TextToolOutcome.Transformed(trimmed, TextRange(trimmed.length.coerceAtMost(selMin)))
            }

            TextToolAction.DUPLICATE_CURRENT_LINE -> {
                val lineStart = text.lastIndexOf('\n', (selMin - 1).coerceAtLeast(0))
                    .let { if (it == -1) 0 else it + 1 }
                val lineEnd = text.indexOf('\n', selMin)
                    .let { if (it == -1) text.length else it }
                val currentLine = text.substring(lineStart, lineEnd)
                val updated = buildString(text.length + currentLine.length + 1) {
                    append(text, 0, lineEnd)
                    append('\n')
                    append(currentLine)
                    append(text, lineEnd, text.length)
                }
                val newCursor = (lineEnd + 1 + currentLine.length).coerceAtMost(updated.length)
                TextToolOutcome.Transformed(updated, TextRange(newCursor))
            }

            TextToolAction.DELETE_CURRENT_LINE -> {
                val lines = text.split('\n').toMutableList()
                val (lineIdxOneBased, _) = computeLineAndColumn(text, selMin)
                val idx = (lineIdxOneBased - 1).coerceIn(0, lines.lastIndex)
                lines.removeAt(idx)
                val updated = if (lines.isEmpty()) "" else lines.joinToString("\n")
                val newOff = offsetForLine(updated, lineIdxOneBased.coerceAtMost(lines.size.coerceAtLeast(1)))
                TextToolOutcome.Transformed(updated, TextRange(newOff.coerceIn(0, updated.length)))
            }

            TextToolAction.MOVE_LINE_UP -> {
                val lines = text.split('\n').toMutableList()
                val (lineOneBased, colOneBased) = computeLineAndColumn(text, selMin)
                val idx = lineOneBased - 1
                if (idx <= 0 || idx >= lines.size) {
                    TextToolOutcome.Transformed(text, selection)
                } else {
                    val tmp = lines[idx - 1]
                    lines[idx - 1] = lines[idx]
                    lines[idx] = tmp
                    val updated = lines.joinToString("\n")
                    val targetStart = offsetForLine(updated, lineOneBased - 1)
                    val newCursor = (targetStart + colOneBased - 1).coerceIn(0, updated.length)
                    TextToolOutcome.Transformed(updated, TextRange(newCursor))
                }
            }

            TextToolAction.MOVE_LINE_DOWN -> {
                val lines = text.split('\n').toMutableList()
                val (lineOneBased, colOneBased) = computeLineAndColumn(text, selMin)
                val idx = lineOneBased - 1
                if (idx < 0 || idx >= lines.lastIndex) {
                    TextToolOutcome.Transformed(text, selection)
                } else {
                    val tmp = lines[idx + 1]
                    lines[idx + 1] = lines[idx]
                    lines[idx] = tmp
                    val updated = lines.joinToString("\n")
                    val targetStart = offsetForLine(updated, lineOneBased + 1)
                    val newCursor = (targetStart + colOneBased - 1).coerceIn(0, updated.length)
                    TextToolOutcome.Transformed(updated, TextRange(newCursor))
                }
            }

            TextToolAction.TOGGLE_LINE_COMMENT -> {
                val prefix = language.lineCommentPrefix
                val trimmedPrefix = prefix.trim()
                val lines = text.split('\n').toMutableList()
                val (startLine, _) = computeLineAndColumn(text, selMin)
                val (endLine, _) = computeLineAndColumn(text, selMax)
                val range = (startLine - 1)..(endLine - 1).coerceAtMost(lines.lastIndex)
                val allCommented = range.all { lines[it].trimStart().startsWith(trimmedPrefix) }
                for (i in range) {
                    val line = lines[i]
                    lines[i] = if (allCommented) {
                        val pos = line.indexOf(trimmedPrefix)
                        if (pos != -1) {
                            val cutLen = if (line.drop(pos).startsWith(prefix)) prefix.length else trimmedPrefix.length
                            line.removeRange(pos, pos + cutLen)
                        } else line
                    } else {
                        prefix + line
                    }
                }
                val updated = lines.joinToString("\n")
                TextToolOutcome.Transformed(updated, TextRange(selMin.coerceAtMost(updated.length)))
            }

            TextToolAction.INDENT_LINES -> {
                val pad = " ".repeat(tabSize)
                val lines = text.split('\n').toMutableList()
                val (startLine, _) = computeLineAndColumn(text, selMin)
                val (endLine, _) = computeLineAndColumn(text, selMax)
                for (i in (startLine - 1)..(endLine - 1).coerceAtMost(lines.lastIndex)) {
                    lines[i] = pad + lines[i]
                }
                val updated = lines.joinToString("\n")
                TextToolOutcome.Transformed(updated, TextRange((selMin + pad.length).coerceAtMost(updated.length)))
            }

            TextToolAction.UNINDENT_LINES -> {
                val lines = text.split('\n').toMutableList()
                val (startLine, _) = computeLineAndColumn(text, selMin)
                val (endLine, _) = computeLineAndColumn(text, selMax)
                for (i in (startLine - 1)..(endLine - 1).coerceAtMost(lines.lastIndex)) {
                    val ln = lines[i]
                    val spacesToRemove = ln.take(tabSize).takeWhile { it == ' ' }.length
                    lines[i] = when {
                        ln.startsWith("\t") -> ln.drop(1)
                        spacesToRemove > 0 -> ln.drop(spacesToRemove)
                        else -> ln
                    }
                }
                val updated = lines.joinToString("\n")
                TextToolOutcome.Transformed(updated, TextRange(selMin.coerceAtMost(updated.length)))
            }

            TextToolAction.INSERT_DATETIME -> {
                val stamp = SimpleDateFormat("HH:mm yyyy-MM-dd", Locale.getDefault()).format(Date())
                val updated = text.substring(0, selMin) + stamp + text.substring(selMax)
                TextToolOutcome.Transformed(updated, TextRange(selMin + stamp.length))
            }

            TextToolAction.FORMAT_JSON_PRETTY -> {
                val trimmed = text.trim()
                val formatted = runCatching {
                    when {
                        trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
                        trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
                        else -> error("Not JSON")
                    }
                }.getOrNull() ?: return TextToolOutcome.ValidationError(
                    TextToolOutcome.ErrorKind.INVALID_JSON
                )
                TextToolOutcome.Transformed(formatted, TextRange(0))
            }

            TextToolAction.JSON_MINIFY -> {
                val trimmed = text.trim()
                val minified = runCatching {
                    when {
                        trimmed.startsWith("{") -> JSONObject(trimmed).toString()
                        trimmed.startsWith("[") -> JSONArray(trimmed).toString()
                        else -> error("Not JSON")
                    }
                }.getOrNull() ?: return TextToolOutcome.ValidationError(
                    TextToolOutcome.ErrorKind.INVALID_JSON
                )
                TextToolOutcome.Transformed(minified, TextRange(0))
            }

            TextToolAction.BASE64_ENCODE -> {
                transformSelectionOrAll(text, selMin, selMax, hasSelection) { segment ->
                    Base64.encodeToString(segment.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                }
            }

            TextToolAction.BASE64_DECODE -> {
                val target = if (hasSelection) text.substring(selMin, selMax) else text.trim()
                val decoded = runCatching {
                    String(Base64.decode(target, Base64.DEFAULT), Charsets.UTF_8)
                }.getOrNull() ?: return TextToolOutcome.ValidationError(
                    TextToolOutcome.ErrorKind.INVALID_BASE64
                )
                if (hasSelection) {
                    val merged = text.substring(0, selMin) + decoded + text.substring(selMax)
                    TextToolOutcome.Transformed(merged, TextRange(selMin, selMin + decoded.length))
                } else {
                    TextToolOutcome.Transformed(decoded, TextRange(decoded.length))
                }
            }
        }
    }

    private inline fun transformSelectionOrAll(
        text: String,
        selMin: Int,
        selMax: Int,
        hasSelection: Boolean,
        transform: (String) -> String
    ): TextToolOutcome {
        return if (hasSelection) {
            val converted = transform(text.substring(selMin, selMax))
            val updated = text.substring(0, selMin) + converted + text.substring(selMax)
            TextToolOutcome.Transformed(updated, TextRange(selMin, selMin + converted.length))
        } else {
            val converted = transform(text)
            TextToolOutcome.Transformed(converted, TextRange(converted.length.coerceAtMost(selMin)))
        }
    }
}
