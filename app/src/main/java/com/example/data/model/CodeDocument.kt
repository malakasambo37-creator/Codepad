package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CodeDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "new 1",
    val content: String = "",
    val language: String = CodeLanguage.PLAINTEXT.id,
    @get:PropertyName("isPinned")
    val isPinned: Boolean = false,
    @get:PropertyName("isOpenTab")
    val isOpenTab: Boolean = true,
    val cursorPosition: Long = 0L,
    val lineEnding: String = "LF",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

fun DocumentSnapshot.toCodeDocumentOrNull(): CodeDocument? {
    if (!exists()) return null
    return CodeDocument(
        id = id,
        userId = getString("userId") ?: "",
        title = getString("title") ?: "new 1",
        content = getString("content") ?: "",
        language = getString("language") ?: CodeLanguage.PLAINTEXT.id,
        isPinned = getBoolean("isPinned") ?: false,
        isOpenTab = getBoolean("isOpenTab") ?: true,
        cursorPosition = getLong("cursorPosition") ?: 0L,
        lineEnding = getString("lineEnding") ?: "LF",
        createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
        updatedAt = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    )
}

enum class CodeLanguage(
    val id: String,
    val displayName: String,
    val nppStatusDescription: String,
    val shortBadge: String,
    val defaultExtension: String,
    val lineCommentPrefix: String,
    val badgeColor: Color
) {
    PLAINTEXT(
        id = "plaintext",
        displayName = "Normal Text",
        nppStatusDescription = "Normal text file",
        shortBadge = "TXT",
        defaultExtension = "txt",
        lineCommentPrefix = "// ",
        badgeColor = Color(0xFF64748B)
    ),
    KOTLIN(
        id = "kotlin",
        displayName = "Kotlin",
        nppStatusDescription = "Kotlin source file",
        shortBadge = "KT",
        defaultExtension = "kt",
        lineCommentPrefix = "// ",
        badgeColor = Color(0xFF9333EA)
    ),
    JAVA(
        id = "java",
        displayName = "Java",
        nppStatusDescription = "Java source file",
        shortBadge = "JV",
        defaultExtension = "java",
        lineCommentPrefix = "// ",
        badgeColor = Color(0xFFEA580C)
    ),
    PYTHON(
        id = "python",
        displayName = "Python",
        nppStatusDescription = "Python file",
        shortBadge = "PY",
        defaultExtension = "py",
        lineCommentPrefix = "# ",
        badgeColor = Color(0xFF0284C7)
    ),
    JAVASCRIPT(
        id = "javascript",
        displayName = "JavaScript / TS",
        nppStatusDescription = "JavaScript file",
        shortBadge = "JS",
        defaultExtension = "js",
        lineCommentPrefix = "// ",
        badgeColor = Color(0xFFCA8A04)
    ),
    HTML(
        id = "html",
        displayName = "HTML / XML",
        nppStatusDescription = "Hyper Text Markup Language file",
        shortBadge = "HTML",
        defaultExtension = "html",
        lineCommentPrefix = "<!-- ",
        badgeColor = Color(0xFFE11D48)
    ),
    CSS(
        id = "css",
        displayName = "CSS",
        nppStatusDescription = "Cascade Style Sheets file",
        shortBadge = "CSS",
        defaultExtension = "css",
        lineCommentPrefix = "/* ",
        badgeColor = Color(0xFF0891B2)
    ),
    CPP(
        id = "cpp",
        displayName = "C / C++",
        nppStatusDescription = "C++ source file",
        shortBadge = "C++",
        defaultExtension = "cpp",
        lineCommentPrefix = "// ",
        badgeColor = Color(0xFF2563EB)
    ),
    JSON(
        id = "json",
        displayName = "JSON",
        nppStatusDescription = "JSON file",
        shortBadge = "JSON",
        defaultExtension = "json",
        lineCommentPrefix = "// ",
        badgeColor = Color(0xFF16A34A)
    ),
    SQL(
        id = "sql",
        displayName = "SQL",
        nppStatusDescription = "Structured Query Language file",
        shortBadge = "SQL",
        defaultExtension = "sql",
        lineCommentPrefix = "-- ",
        badgeColor = Color(0xFFDB2777)
    ),
    MARKDOWN(
        id = "markdown",
        displayName = "Markdown",
        nppStatusDescription = "Markdown file",
        shortBadge = "MD",
        defaultExtension = "md",
        lineCommentPrefix = "> ",
        badgeColor = Color(0xFF0D9488)
    );

    companion object {
        fun fromId(id: String): CodeLanguage {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: PLAINTEXT
        }

        fun fromFileName(fileName: String): CodeLanguage {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "kt", "kts" -> KOTLIN
                "java" -> JAVA
                "py" -> PYTHON
                "js", "jsx", "ts", "tsx", "mjs" -> JAVASCRIPT
                "html", "htm", "xml", "svg" -> HTML
                "css", "scss" -> CSS
                "c", "cpp", "cc", "h", "hpp" -> CPP
                "json" -> JSON
                "sql" -> SQL
                "md", "markdown" -> MARKDOWN
                else -> PLAINTEXT
            }
        }
    }
}

object StarterTemplates {
    fun starterFor(language: CodeLanguage): String = when (language) {
        CodeLanguage.KOTLIN -> """
            package app.codepad

            // Notepad++ Style Editor di Android
            data class PluginItem(val id: Int, val name: String, val active: Boolean = true)

            fun main() {
                val plugins = listOf(
                    PluginItem(1, "NppExport", active = true),
                    PluginItem(2, "MimeTools", active = true),
                    PluginItem(3, "Converter", active = false)
                )
                plugins.forEach { item ->
                    println("[#${'$'}{item.id}] ${'$'}{item.name} -> active=${'$'}{item.active}")
                }
            }
        """.trimIndent()

        CodeLanguage.PYTHON -> """
            # Python Script - CodePad (Notepad++ Style)
            from dataclasses import dataclass

            @dataclass
            class BufferStats:
                name: str
                lines: int

            def print_summary(buffers: list[BufferStats]) -> None:
                for buf in buffers:
                    print(f"{buf.name}: {buf.lines} lines")

            if __name__ == "__main__":
                print_summary([
                    BufferStats("new 1", 24),
                    BufferStats("script.py", 68),
                ])
        """.trimIndent()

        CodeLanguage.JAVASCRIPT -> """
            // JavaScript / ESNext Module
            function formatFileSize(bytes) {
              if (bytes === 0) return "0 B";
              const units = ["B", "KB", "MB", "GB"];
              const idx = Math.floor(Math.log(bytes) / Math.log(1024));
              return `${'$'}{(bytes / Math.pow(1024, idx)).toFixed(2)} ${'$'}{units[idx]}`;
            }

            console.log("Length:", formatFileSize(16384));
        """.trimIndent()

        CodeLanguage.HTML -> """
            <!DOCTYPE html>
            <html lang="id">
            <head>
              <meta charset="UTF-8" />
              <title>Notepad++ Style Preview</title>
            </head>
            <body>
              <main class="container">
                <h1>CodePad Desktop Mode</h1>
                <p>Tampilan Scintilla klasik lengkap dengan Menu Bar &amp; Toolbar.</p>
              </main>
            </body>
            </html>
        """.trimIndent()

        CodeLanguage.CPP -> """
            #include <iostream>
            #include <vector>
            #include <string>

            int main() {
                std::vector<std::string> menus = {"File", "Edit", "Search", "View", "Encoding", "Language"};
                for (const auto& m : menus) {
                    std::cout << m << " | ";
                }
                std::cout << std::endl;
                return 0;
            }
        """.trimIndent()

        CodeLanguage.JSON -> """
            {
              "editor": "CodePad",
              "layout": "Notepad++ PC Classic",
              "scintilla": {
                "lineNumbers": true,
                "foldMargin": true,
                "indentGuides": true,
                "currentLineHighlight": "#FFFBD6"
              }
            }
        """.trimIndent()

        CodeLanguage.SQL -> """
            SELECT id, title, language, line_ending
            FROM documents
            WHERE is_open_tab = TRUE
            ORDER BY updated_at DESC
            LIMIT 50;
        """.trimIndent()

        CodeLanguage.MARKDOWN -> """
            # Notepad++ Workspace Notes

            ## Shortcut Cepat
            - **File -> New**: membuat tab `new 1`, `new 2`, dst.
            - **Search -> Find / Replace**: mendukung `Normal`, `Extended (\n, \t)`, dan `Regex`.
            - **Macro**: rekam urutan edit lalu putar berulang kali!
        """.trimIndent()

        CodeLanguage.JAVA -> """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Notepad++ Classic Style on Android");
                }
            }
        """.trimIndent()

        CodeLanguage.CSS -> """
            .npp-tab-active {
              border-top: 3px solid #f97316;
              background-color: #ffffff;
              color: #000000;
            }
        """.trimIndent()

        CodeLanguage.PLAINTEXT -> ""
    }
}
