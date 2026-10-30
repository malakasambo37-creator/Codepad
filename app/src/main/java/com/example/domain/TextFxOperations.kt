package com.example.domain

import android.util.Base64
import androidx.compose.ui.text.TextRange
import org.json.JSONArray
import org.json.JSONObject

enum class TextToolAction {
    UPPERCASE,
    LOWERCASE,
    SORT_LINES_ASC,
    SORT_LINES_DESC,
    REMOVE_DUPLICATE_LINES,
    REMOVE_EMPTY_LINES,
    TRIM_TRAILING_SPACES,
    DUPLICATE_CURRENT_LINE,
    FORMAT_JSON_PRETTY,
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

object TextFxOperations {

    fun findMatches(
        text: String,
        query: String,
        matchCase: Boolean,
        useRegex: Boolean
    ): Result<List<IntRange>> {
        if (query.isEmpty() || text.isEmpty()) return Result.success(emptyList())

        return try {
            val regex = if (useRegex) {
                val options = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
                Regex(query, options)
            } else {
                val escaped = Regex.escape(query)
                val options = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
                Regex(escaped, options)
            }

            val matches = regex.findAll(text)
                .filter { !it.range.isEmpty() }
                .take(2000)
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
        useRegex: Boolean
    ): Pair<String, Int> {
        val start = targetRange.first.coerceIn(0, text.length)
        val endExclusive = (targetRange.last + 1).coerceIn(start, text.length)
        val matchedSubstring = text.substring(start, endExclusive)

        val replacedSegment = if (useRegex) {
            runCatching {
                val opts = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
                Regex(query, opts).replaceFirst(matchedSubstring, replacement)
            }.getOrDefault(replacement)
        } else {
            replacement
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
        useRegex: Boolean
    ): Result<Pair<String, Int>> {
        if (query.isEmpty()) return Result.success(text to 0)
        return try {
            val opts = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
            val regex = if (useRegex) Regex(query, opts) else Regex(Regex.escape(query), opts)
            val count = regex.findAll(text).filter { !it.range.isEmpty() }.count()
            if (count == 0) return Result.success(text to 0)

            val updated = if (useRegex) {
                regex.replace(text, replacement)
            } else {
                regex.replace(text, Regex.escapeReplacement(replacement))
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

    fun applyTextTool(
        action: TextToolAction,
        text: String,
        selection: TextRange
    ): TextToolOutcome {
        val selMin = selection.min.coerceIn(0, text.length)
        val selMax = selection.max.coerceIn(selMin, text.length)
        val hasSelection = selMax > selMin

        return when (action) {
            TextToolAction.UPPERCASE -> {
                transformSelectionOrAll(text, selMin, selMax, hasSelection) { it.uppercase() }
            }

            TextToolAction.LOWERCASE -> {
                transformSelectionOrAll(text, selMin, selMax, hasSelection) { it.lowercase() }
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

            TextToolAction.FORMAT_JSON_PRETTY -> {
                val trimmed = text.trim()
                val formatted = runCatching {
                    when {
                        trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
                        trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
                        else -> error("Not JSON object or array")
                    }
                }.getOrNull() ?: return TextToolOutcome.ValidationError(
                    TextToolOutcome.ErrorKind.INVALID_JSON
                )
                TextToolOutcome.Transformed(formatted, TextRange(0))
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
