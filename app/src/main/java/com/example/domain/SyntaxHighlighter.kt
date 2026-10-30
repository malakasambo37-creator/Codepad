package com.example.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.data.model.CodeLanguage
import com.example.ui.theme.EditorColorPalette

class CodeSyntaxVisualTransformation(
    private val language: CodeLanguage,
    private val palette: EditorColorPalette,
    private val searchMatches: List<IntRange> = emptyList(),
    private val activeMatchIndex: Int = -1
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val highlighted = SyntaxHighlighter.highlight(
            code = raw,
            language = language,
            palette = palette,
            searchMatches = searchMatches,
            activeMatchIndex = activeMatchIndex
        )
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

object SyntaxHighlighter {

    private val numberRegex = Regex("""\b(?:0x[0-9a-fA-F]+|\d+(?:\.\d+)?(?:[eE][+-]?\d+)?[fFdDlL]?)\b""")
    private val bracketRegex = Regex("""[\{\}\(\)\[\]<>]|=>|->|==|!=|<=|>=|\+\+|--""")
    private val annotationRegex = Regex("""@[A-Za-z_][A-Za-z0-9_]*""")
    private val cDirectiveRegex = Regex("""(?m)^\s*#\s*[a-zA-Z_]+""")
    private val htmlTagRegex = Regex("""</?[A-Za-z][A-Za-z0-9\-:]*|/?>""")
    private val htmlAttrRegex = Regex("""\b[A-Za-z_:][A-Za-z0-9_.\-:]*\s*(?==)""")
    private val jsonKeyRegex = Regex(""""(?:\\.|[^"\\])*"(?=\s*:)""")
    private val markdownHeadingRegex = Regex("""(?m)^#{1,6}\s+.*$""")
    private val markdownBoldRegex = Regex("""\*\*[^*\n]+\*\*|__[^_\n]+__""")
    private val markdownInlineCodeRegex = Regex("""`[^`\n]+`""")

    private val doubleQuoteStringRegex = Regex(""""(?:\\.|[^"\\\n])*"""")
    private val singleQuoteStringRegex = Regex("""'(?:\\.|[^'\\\n])*'""")
    private val backtickStringRegex = Regex("""`(?:\\.|[^`\\])*`""")
    private val tripleQuoteRegex = Regex("(\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?''')")

    private val slashLineCommentRegex = Regex("""//[^\n]*""")
    private val blockCommentRegex = Regex("""/\*[\s\S]*?\*/""")
    private val hashCommentRegex = Regex("""#[^\n]*""")
    private val sqlLineCommentRegex = Regex("""--[^\n]*""")
    private val htmlCommentRegex = Regex("""<!--[\s\S]*?-->""")

    private val keywordPatterns: Map<CodeLanguage, Regex> = mapOf(
        CodeLanguage.KOTLIN to wordSetRegex(
            "package", "import", "class", "interface", "object", "fun", "val", "var",
            "if", "else", "when", "for", "while", "do", "return", "break", "continue",
            "try", "catch", "finally", "throw", "in", "is", "as", "null", "true", "false",
            "this", "super", "private", "protected", "public", "internal", "open", "override",
            "data", "sealed", "enum", "companion", "suspend", "inline", "lateinit", "by"
        ),
        CodeLanguage.JAVA to wordSetRegex(
            "package", "import", "class", "interface", "enum", "extends", "implements",
            "public", "private", "protected", "static", "final", "abstract", "synchronized",
            "void", "int", "long", "double", "float", "boolean", "char", "byte", "short",
            "if", "else", "switch", "case", "default", "for", "while", "do", "return",
            "break", "continue", "new", "try", "catch", "finally", "throw", "throws",
            "this", "super", "null", "true", "false", "record", "var"
        ),
        CodeLanguage.PYTHON to wordSetRegex(
            "def", "class", "import", "from", "as", "if", "elif", "else", "for", "while",
            "break", "continue", "return", "yield", "try", "except", "finally", "raise",
            "with", "pass", "lambda", "and", "or", "not", "in", "is", "None", "True",
            "False", "async", "await", "global", "nonlocal", "self", "match", "case"
        ),
        CodeLanguage.JAVASCRIPT to wordSetRegex(
            "const", "let", "var", "function", "class", "extends", "import", "export",
            "from", "default", "if", "else", "switch", "case", "for", "while", "do",
            "return", "break", "continue", "try", "catch", "finally", "throw", "new",
            "this", "super", "typeof", "instanceof", "in", "of", "async", "await",
            "true", "false", "null", "undefined", "interface", "type", "readonly"
        ),
        CodeLanguage.CPP to wordSetRegex(
            "int", "long", "short", "double", "float", "char", "void", "bool", "auto",
            "const", "constexpr", "static", "extern", "inline", "virtual", "override",
            "class", "struct", "enum", "namespace", "using", "template", "typename",
            "public", "private", "protected", "if", "else", "switch", "case", "default",
            "for", "while", "do", "return", "break", "continue", "new", "delete",
            "try", "catch", "throw", "nullptr", "true", "false", "sizeof", "std"
        ),
        CodeLanguage.SQL to Regex(
            """\b(?:SELECT|FROM|WHERE|INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|TABLE|ALTER|DROP|INDEX|JOIN|INNER|LEFT|RIGHT|FULL|ON|AS|AND|OR|NOT|NULL|IS|IN|EXISTS|GROUP|BY|ORDER|ASC|DESC|HAVING|LIMIT|OFFSET|DISTINCT|COUNT|SUM|AVG|MIN|MAX|PRIMARY|KEY|FOREIGN|TRUE|FALSE)\b""",
            RegexOption.IGNORE_CASE
        ),
        CodeLanguage.CSS to wordSetRegex(
            "display", "flex", "grid", "position", "color", "background", "background-color",
            "margin", "padding", "border", "border-radius", "width", "height", "font-size",
            "font-weight", "font-family", "align-items", "justify-content", "gap", "z-index",
            "important", "media", "keyframes", "var", "solid", "none", "auto"
        ),
        CodeLanguage.JSON to wordSetRegex("true", "false", "null")
    )

    private val commonTypesRegex = wordSetRegex(
        "String", "Int", "Long", "Double", "Float", "Boolean", "Unit", "List", "Map", "Set",
        "Array", "Promise", "Console", "Math", "Object", "System", "Exception", "Result",
        "Flow", "StateFlow", "Timestamp", "vector", "string", "iostream"
    )

    fun highlight(
        code: String,
        language: CodeLanguage,
        palette: EditorColorPalette,
        searchMatches: List<IntRange> = emptyList(),
        activeMatchIndex: Int = -1
    ): AnnotatedString {
        // Skip heavy regex passes on extremely huge files (> 80KB) to keep 60fps typing smooth
        val enableSyntax = language != CodeLanguage.PLAINTEXT && code.length <= 80_000

        return buildAnnotatedString {
            append(code)

            if (enableSyntax) {
                when (language) {
                    CodeLanguage.MARKDOWN -> applyMarkdownSpans(code, palette)
                    CodeLanguage.HTML -> applyHtmlSpans(code, palette)
                    CodeLanguage.JSON -> applyJsonSpans(code, palette)
                    else -> applyGeneralCodeSpans(code, language, palette)
                }
            }

            // Highlight search matches on top so they are always visible
            searchMatches.forEachIndexed { idx, range ->
                val start = range.first.coerceIn(0, code.length)
                val endExclusive = (range.last + 1).coerceIn(start, code.length)
                if (start < endExclusive) {
                    val isCurrent = idx == activeMatchIndex
                    addStyle(
                        style = SpanStyle(
                            background = if (isCurrent) {
                                Color(0xCC22C55E)
                            } else {
                                Color(0x66FBBF24)
                            },
                            color = if (isCurrent) Color(0xFF052E16) else Color.Unspecified,
                            fontWeight = if (isCurrent) FontWeight.Bold else null
                        ),
                        start = start,
                        end = endExclusive
                    )
                }
            }
        }
    }

    private fun AnnotatedString.Builder.applyGeneralCodeSpans(
        code: String,
        language: CodeLanguage,
        palette: EditorColorPalette
    ) {
        applyRegex(bracketRegex, code, SpanStyle(color = palette.operatorOrBracket))
        applyRegex(numberRegex, code, SpanStyle(color = palette.numberLiteral))
        applyRegex(commonTypesRegex, code, SpanStyle(color = palette.functionOrType))

        keywordPatterns[language]?.let { kwRegex ->
            applyRegex(
                kwRegex,
                code,
                SpanStyle(color = palette.keyword, fontWeight = FontWeight.SemiBold)
            )
        }

        if (language == CodeLanguage.CPP) {
            applyRegex(cDirectiveRegex, code, SpanStyle(color = palette.annotationOrTag))
        } else {
            applyRegex(annotationRegex, code, SpanStyle(color = palette.annotationOrTag))
        }

        // Strings override keywords inside quotes
        applyRegex(doubleQuoteStringRegex, code, SpanStyle(color = palette.stringLiteral))
        applyRegex(singleQuoteStringRegex, code, SpanStyle(color = palette.stringLiteral))
        if (language == CodeLanguage.JAVASCRIPT) {
            applyRegex(backtickStringRegex, code, SpanStyle(color = palette.stringLiteral))
        }
        if (language == CodeLanguage.PYTHON || language == CodeLanguage.KOTLIN) {
            applyRegex(tripleQuoteRegex, code, SpanStyle(color = palette.stringLiteral))
        }

        // Comments override everything inside comment ranges
        val commentStyle = SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic)
        when (language) {
            CodeLanguage.PYTHON -> applyRegex(hashCommentRegex, code, commentStyle)
            CodeLanguage.SQL -> {
                applyRegex(sqlLineCommentRegex, code, commentStyle)
                applyRegex(blockCommentRegex, code, commentStyle)
            }
            CodeLanguage.CSS -> applyRegex(blockCommentRegex, code, commentStyle)
            else -> {
                applyRegex(slashLineCommentRegex, code, commentStyle)
                applyRegex(blockCommentRegex, code, commentStyle)
            }
        }
    }

    private fun AnnotatedString.Builder.applyHtmlSpans(code: String, palette: EditorColorPalette) {
        applyRegex(
            htmlTagRegex,
            code,
            SpanStyle(color = palette.keyword, fontWeight = FontWeight.SemiBold)
        )
        runCatching {
            val attrSafe = Regex("""\b[A-Za-z_:][A-Za-z0-9_.\-:]*\s*(?==)""")
            applyRegex(attrSafe, code, SpanStyle(color = palette.annotationOrTag))
        }
        applyRegex(doubleQuoteStringRegex, code, SpanStyle(color = palette.stringLiteral))
        applyRegex(singleQuoteStringRegex, code, SpanStyle(color = palette.stringLiteral))
        applyRegex(
            htmlCommentRegex,
            code,
            SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic)
        )
    }

    private fun AnnotatedString.Builder.applyJsonSpans(code: String, palette: EditorColorPalette) {
        applyRegex(bracketRegex, code, SpanStyle(color = palette.operatorOrBracket))
        applyRegex(numberRegex, code, SpanStyle(color = palette.numberLiteral))
        keywordPatterns[CodeLanguage.JSON]?.let {
            applyRegex(it, code, SpanStyle(color = palette.keyword, fontWeight = FontWeight.SemiBold))
        }
        applyRegex(doubleQuoteStringRegex, code, SpanStyle(color = palette.stringLiteral))
        applyRegex(
            jsonKeyRegex,
            code,
            SpanStyle(color = palette.functionOrType, fontWeight = FontWeight.Medium)
        )
    }

    private fun AnnotatedString.Builder.applyMarkdownSpans(code: String, palette: EditorColorPalette) {
        applyRegex(
            markdownHeadingRegex,
            code,
            SpanStyle(color = palette.keyword, fontWeight = FontWeight.Bold)
        )
        applyRegex(
            markdownBoldRegex,
            code,
            SpanStyle(color = palette.functionOrType, fontWeight = FontWeight.Bold)
        )
        applyRegex(
            markdownInlineCodeRegex,
            code,
            SpanStyle(color = palette.stringLiteral)
        )
    }

    private fun AnnotatedString.Builder.applyRegex(
        regex: Regex,
        text: String,
        style: SpanStyle
    ) {
        regex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            if (start in 0 until end && end <= text.length) {
                addStyle(style, start, end)
            }
        }
    }

    private fun wordSetRegex(vararg words: String): Regex {
        val joined = words.joinToString("|") { Regex.escape(it) }
        return Regex("\\b(?:$joined)\\b")
    }
}
