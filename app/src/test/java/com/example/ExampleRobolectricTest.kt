package com.example

import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CodeLanguage
import com.example.domain.SearchMode
import com.example.domain.TextFxOperations
import com.example.domain.TextToolAction
import com.example.domain.TextToolOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CodePad", appName)
    }

    @Test
    fun `detects syntax language from file extension`() {
        assertEquals(CodeLanguage.KOTLIN, CodeLanguage.fromFileName("MainActivity.kt"))
        assertEquals(CodeLanguage.PYTHON, CodeLanguage.fromFileName("script.py"))
        assertEquals(CodeLanguage.CPP, CodeLanguage.fromFileName("editor.cpp"))
        assertEquals(CodeLanguage.JSON, CodeLanguage.fromFileName("package.json"))
    }

    @Test
    fun `regex find and textfx operations transform text accurately`() {
        val text = "zeta\nalpha\nalpha\nbeta"
        val matches = TextFxOperations.findMatches(
            text = text,
            query = "a[a-z]+",
            matchCase = true,
            useRegex = true
        ).getOrThrow()
        assertEquals(2, matches.size)

        val sortedOutcome = TextFxOperations.applyTextTool(
            TextToolAction.SORT_LINES_ASC,
            text,
            TextRange(0)
        )
        assertTrue(sortedOutcome is TextToolOutcome.Transformed)
        assertEquals("alpha\nalpha\nbeta\nzeta", (sortedOutcome as TextToolOutcome.Transformed).text)

        val dedupedOutcome = TextFxOperations.applyTextTool(
            TextToolAction.REMOVE_DUPLICATE_LINES,
            sortedOutcome.text,
            TextRange(0)
        )
        assertEquals("alpha\nbeta\nzeta", (dedupedOutcome as TextToolOutcome.Transformed).text)
    }

    @Test
    fun `notepad plus plus extended search function list and line operations work`() {
        val code = "class Main {\n    fun greet() {\n        println(\"hi\")\n    }\n}"
        val extMatches = TextFxOperations.findMatchesWithMode(
            text = code,
            query = "{\\n    fun",
            matchCase = true,
            mode = SearchMode.EXTENDED
        ).getOrThrow()
        assertEquals(1, extMatches.size)

        val symbols = TextFxOperations.extractFunctions(code, CodeLanguage.KOTLIN)
        assertEquals(2, symbols.size)
        assertEquals("Main", symbols[0].name)
        assertEquals("greet", symbols[1].name)

        val commented = TextFxOperations.applyTextTool(
            TextToolAction.TOGGLE_LINE_COMMENT,
            "val x = 1",
            TextRange(0),
            CodeLanguage.KOTLIN
        ) as TextToolOutcome.Transformed
        assertEquals("// val x = 1", commented.text)

        val summary = TextFxOperations.computeDocumentSummary(code, TextRange(0, 5))
        assertEquals(5, summary.lineCount)
        assertEquals(5, summary.selectedChars)
    }
}
