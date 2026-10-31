package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Notepad++ Chameleon Green & Desktop Chrome Palette
val EmeraldPrimaryDark = Color(0xFF4ADE80)
val EmeraldOnPrimaryDark = Color(0xFF052E16)
val EmeraldContainerDark = Color(0xFF14532D)
val EmeraldOnContainerDark = Color(0xFFBBF7D0)

val CyanSecondaryDark = Color(0xFF38BDF8)
val CyanOnSecondaryDark = Color(0xFF082F49)
val CyanContainerDark = Color(0xFF0C4A6E)
val CyanOnContainerDark = Color(0xFFE0F2FE)

val AmberTertiaryDark = Color(0xFFF97316)
val AmberOnTertiaryDark = Color(0xFF431407)
val AmberContainerDark = Color(0xFF7C2D12)
val AmberOnContainerDark = Color(0xFFFFEDD5)

val ObsidianBackground = Color(0xFF0D1117)
val ObsidianSurface = Color(0xFF1E222A)
val ObsidianSurfaceVariant = Color(0xFF282C34)
val ObsidianOutline = Color(0xFF3B4048)
val ObsidianOnSurface = Color(0xFFE6EDF3)
val ObsidianOnSurfaceVariant = Color(0xFF9CA3AF)

// Classic Windows / Notepad++ PC Light Chrome Palette
val EmeraldPrimaryLight = Color(0xFF2E7D32)
val EmeraldOnPrimaryLight = Color(0xFFFFFFFF)
val EmeraldContainerLight = Color(0xFFDCFCE7)
val EmeraldOnContainerLight = Color(0xFF052E16)

val CyanSecondaryLight = Color(0xFF1565C0)
val CyanOnSecondaryLight = Color(0xFFFFFFFF)
val CyanContainerLight = Color(0xFFDBEAFE)
val CyanOnContainerLight = Color(0xFF082F49)

val AmberTertiaryLight = Color(0xFFEA580C)
val AmberOnTertiaryLight = Color(0xFFFFFFFF)
val AmberContainerLight = Color(0xFFFFEDD5)
val AmberOnContainerLight = Color(0xFF431407)

val PaperBackground = Color(0xFFF0F0F0)
val PaperSurface = Color(0xFFF3F3F3)
val PaperSurfaceVariant = Color(0xFFE4E6EB)
val PaperOutline = Color(0xFFC4C7CC)
val PaperOnSurface = Color(0xFF1B1F23)
val PaperOnSurfaceVariant = Color(0xFF4B5563)

// Classic Notepad++ Tab Floppy Icon Colors
val FloppySavedBlue = Color(0xFF2563EB)
val FloppyDirtyRed = Color(0xFFDC2626)
val NotepadTabActiveStripe = Color(0xFFF97316)

/**
 * Dedicated Scintilla / Notepad++ Style Configurator color schemes.
 */
data class EditorColorPalette(
    val id: String,
    val displayName: String,
    val isDark: Boolean,
    val chromeMenuBackground: Color,
    val chromeToolbarBackground: Color,
    val chromeTabStripBackground: Color,
    val chromeStatusBackground: Color,
    val editorBackground: Color,
    val gutterBackground: Color,
    val foldMarginBackground: Color,
    val gutterText: Color,
    val activeLineBackground: Color,
    val plainText: Color,
    val keyword: Color,
    val stringLiteral: Color,
    val comment: Color,
    val numberLiteral: Color,
    val annotationOrTag: Color,
    val functionOrType: Color,
    val operatorOrBracket: Color,
    val cursorColor: Color,
    val selectionBackground: Color
)

object EditorPalettes {
    // 1. Authentic Notepad++ Default PC Style (Scintilla White + #FFFBD6 Pale Yellow Active Line)
    val NotepadClassicPc = EditorColorPalette(
        id = "npp_classic",
        displayName = "Default (Notepad++ Classic PC)",
        isDark = false,
        chromeMenuBackground = Color(0xFFF5F6F7),
        chromeToolbarBackground = Color(0xFFECEEF1),
        chromeTabStripBackground = Color(0xFFDCE0E6),
        chromeStatusBackground = Color(0xFFEAECEF),
        editorBackground = Color(0xFFFFFFFF),
        gutterBackground = Color(0xFFE4E4E4),
        foldMarginBackground = Color(0xFFEEEEEE),
        gutterText = Color(0xFF686868),
        activeLineBackground = Color(0xFFFFFBD6),
        plainText = Color(0xFF000000),
        keyword = Color(0xFF0000FF),
        stringLiteral = Color(0xFF808080),
        comment = Color(0xFF008000),
        numberLiteral = Color(0xFFFF8000),
        annotationOrTag = Color(0xFF8000FF),
        functionOrType = Color(0xFF007F7F),
        operatorOrBracket = Color(0xFF000080),
        cursorColor = Color(0xFF000000),
        selectionBackground = Color(0x663399FF)
    )

    // 2. Notepad++ Dark Mode (Windows 11 Notepad++ Dark Mode)
    val ObsidianPlus = EditorColorPalette(
        id = "obsidian",
        displayName = "Notepad++ Dark Mode",
        isDark = true,
        chromeMenuBackground = Color(0xFF21252B),
        chromeToolbarBackground = Color(0xFF282C34),
        chromeTabStripBackground = Color(0xFF181A1F),
        chromeStatusBackground = Color(0xFF21252B),
        editorBackground = Color(0xFF1E1E1E),
        gutterBackground = Color(0xFF252526),
        foldMarginBackground = Color(0xFF2B2B2C),
        gutterText = Color(0xFF858585),
        activeLineBackground = Color(0xFF2A2D2E),
        plainText = Color(0xFFD4D4D4),
        keyword = Color(0xFF569CD6),
        stringLiteral = Color(0xFFCE9178),
        comment = Color(0xFF6A9955),
        numberLiteral = Color(0xFFB5CEA8),
        annotationOrTag = Color(0xFFC586C0),
        functionOrType = Color(0xFF4EC9B0),
        operatorOrBracket = Color(0xFFDCDCAA),
        cursorColor = Color(0xFF4ADE80),
        selectionBackground = Color(0x55264F78)
    )

    // 3. Monokai (Classic Notepad++ Style Configurator Theme)
    val MonokaiPro = EditorColorPalette(
        id = "monokai",
        displayName = "Monokai",
        isDark = true,
        chromeMenuBackground = Color(0xFF1E1F1C),
        chromeToolbarBackground = Color(0xFF272822),
        chromeTabStripBackground = Color(0xFF171814),
        chromeStatusBackground = Color(0xFF1E1F1C),
        editorBackground = Color(0xFF272822),
        gutterBackground = Color(0xFF1E1F1C),
        foldMarginBackground = Color(0xFF23241F),
        gutterText = Color(0xFF90908A),
        activeLineBackground = Color(0xFF3E3D32),
        plainText = Color(0xFFF8F8F2),
        keyword = Color(0xFFF92672),
        stringLiteral = Color(0xFFE6DB74),
        comment = Color(0xFF75715E),
        numberLiteral = Color(0xFFAE81FF),
        annotationOrTag = Color(0xFFA6E22E),
        functionOrType = Color(0xFF66D9EF),
        operatorOrBracket = Color(0xFFFD971F),
        cursorColor = Color(0xFFA6E22E),
        selectionBackground = Color(0x55A6E22E)
    )

    // 4. Dracula
    val Dracula = EditorColorPalette(
        id = "dracula",
        displayName = "Dracula",
        isDark = true,
        chromeMenuBackground = Color(0xFF1E1F29),
        chromeToolbarBackground = Color(0xFF282A36),
        chromeTabStripBackground = Color(0xFF191A21),
        chromeStatusBackground = Color(0xFF1E1F29),
        editorBackground = Color(0xFF282A36),
        gutterBackground = Color(0xFF21222C),
        foldMarginBackground = Color(0xFF252732),
        gutterText = Color(0xFF6272A4),
        activeLineBackground = Color(0xFF343746),
        plainText = Color(0xFFF8F8F2),
        keyword = Color(0xFFFF79C6),
        stringLiteral = Color(0xFFF1FA8C),
        comment = Color(0xFF6272A4),
        numberLiteral = Color(0xFFBD93F9),
        annotationOrTag = Color(0xFF50FA7B),
        functionOrType = Color(0xFF8BE9FD),
        operatorOrBracket = Color(0xFFFFB86C),
        cursorColor = Color(0xFF50FA7B),
        selectionBackground = Color(0x55BD93F9)
    )

    val all: List<EditorColorPalette> = listOf(
        NotepadClassicPc,
        ObsidianPlus,
        MonokaiPro,
        Dracula
    )

    fun fromId(id: String): EditorColorPalette {
        if (id == "paper_light") return NotepadClassicPc
        return all.firstOrNull { it.id == id } ?: NotepadClassicPc
    }
}
