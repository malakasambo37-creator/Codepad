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
    val title: String = "untitled.txt",
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
        title = getString("title") ?: "untitled.txt",
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
    val shortBadge: String,
    val defaultExtension: String,
    val badgeColor: Color
) {
    PLAINTEXT("plaintext", "Plain Text", "TXT", "txt", Color(0xFF94A3B8)),
    KOTLIN("kotlin", "Kotlin", "KT", "kt", Color(0xFFA855F7)),
    JAVA("java", "Java", "JV", "java", Color(0xFFF97316)),
    PYTHON("python", "Python", "PY", "py", Color(0xFF38BDF8)),
    JAVASCRIPT("javascript", "JavaScript / TS", "JS", "js", Color(0xFFFACC15)),
    HTML("html", "HTML / XML", "HTML", "html", Color(0xFFF43F5E)),
    CSS("css", "CSS", "CSS", "css", Color(0xFF06B6D4)),
    CPP("cpp", "C / C++", "C++", "cpp", Color(0xFF3B82F6)),
    JSON("json", "JSON", "JSON", "json", Color(0xFF4ADE80)),
    SQL("sql", "SQL", "SQL", "sql", Color(0xFFEC4899)),
    MARKDOWN("markdown", "Markdown", "MD", "md", Color(0xFF2DD4BF));

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

            // Contoh kode Kotlin di CodePad Studio
            data class Task(val id: Int, val title: String, val done: Boolean = false)

            fun main() {
                val items = listOf(
                    Task(1, "Buka multi-tab editor", done = true),
                    Task(2, "Uji pencarian Regex", done = false)
                )
                items.forEach { task ->
                    val mark = if (task.done) "[x]" else "[ ]"
                    println("${'$'}mark #${'$'}{task.id}: ${'$'}{task.title}")
                }
            }
        """.trimIndent()

        CodeLanguage.PYTHON -> """
            # Skrip Python cepat di CodePad
            from dataclasses import dataclass

            @dataclass
            class DeveloperNote:
                title: str
                priority: int

            def summarize(notes: list[DeveloperNote]) -> None:
                for note in sorted(notes, key=lambda n: n.priority):
                    print(f"[{note.priority}] {note.title}")

            if __name__ == "__main__":
                summarize([
                    DeveloperNote("Refactor API handler", 1),
                    DeveloperNote("Rapikan konfigurasi JSON", 2),
                ])
        """.trimIndent()

        CodeLanguage.JAVASCRIPT -> """
            // Modul JavaScript / ESNext
            const formatBytes = (bytes) => {
              if (bytes === 0) return "0 B";
              const units = ["B", "KB", "MB", "GB"];
              const index = Math.floor(Math.log(bytes) / Math.log(1024));
              return `${'$'}{(bytes / Math.pow(1024, index)).toFixed(2)} ${'$'}{units[index]}`;
            };

            console.log("Ukuran buffer:", formatBytes(24576));
        """.trimIndent()

        CodeLanguage.HTML -> """
            <!DOCTYPE html>
            <html lang="id">
            <head>
              <meta charset="UTF-8" />
              <title>CodePad Preview</title>
            </head>
            <body>
              <main class="container">
                <h1>Halo dari CodePad Android</h1>
                <p>Editor ringan bergaya Notepad++ untuk perangkat mobile.</p>
              </main>
            </body>
            </html>
        """.trimIndent()

        CodeLanguage.CPP -> """
            #include <iostream>
            #include <vector>
            #include <string>

            int main() {
                std::vector<std::string> features = {"Multi-Tab", "Regex Search", "Syntax Highlight"};
                for (const auto& item : features) {
                    std::cout << "+ " << item << std::endl;
                }
                return 0;
            }
        """.trimIndent()

        CodeLanguage.JSON -> """
            {
              "app": "CodePad",
              "inspiredBy": "Notepad++",
              "features": [
                "multi-tab",
                "syntax-highlighting",
                "regex-find-replace",
                "cloud-sync"
              ],
              "encoding": "UTF-8"
            }
        """.trimIndent()

        CodeLanguage.SQL -> """
            SELECT id, title, language, updated_at
            FROM user_documents
            WHERE is_pinned = TRUE
            ORDER BY updated_at DESC
            LIMIT 25;
        """.trimIndent()

        CodeLanguage.MARKDOWN -> """
            # Catatan Proyek CodePad

            - [x] Multi-tab dokumen aktif
            - [x] Syntax highlighting real-time
            - [ ] Gunakan **Find & Replace** dengan mode `.*` Regex
        """.trimIndent()

        CodeLanguage.JAVA -> """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello from CodePad Java!");
                }
            }
        """.trimIndent()

        CodeLanguage.CSS -> """
            .editor-card {
              display: flex;
              padding: 16px;
              border-radius: 12px;
              background-color: #0d1117;
              color: #4ade80;
            }
        """.trimIndent()

        CodeLanguage.PLAINTEXT -> """
            Selamat datang di CodePad!
            Ketik catatan, log, atau kode sumber Anda di sini.
        """.trimIndent()
    }
}
