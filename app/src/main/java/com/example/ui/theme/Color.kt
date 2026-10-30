package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Notepad++ Chameleon Emerald & Obsidian Studio Brand Palette
val EmeraldPrimaryDark = Color(0xFF4ADE80)
val EmeraldOnPrimaryDark = Color(0xFF052E16)
val EmeraldContainerDark = Color(0xFF14532D)
val EmeraldOnContainerDark = Color(0xFFBBF7D0)

val CyanSecondaryDark = Color(0xFF38BDF8)
val CyanOnSecondaryDark = Color(0xFF082F49)
val CyanContainerDark = Color(0xFF0C4A6E)
val CyanOnContainerDark = Color(0xFFE0F2FE)

val AmberTertiaryDark = Color(0xFFFBBF24)
val AmberOnTertiaryDark = Color(0xFF451A03)
val AmberContainerDark = Color(0xFF78350F)
val AmberOnContainerDark = Color(0xFFFEF3C7)

val ObsidianBackground = Color(0xFF0D1117)
val ObsidianSurface = Color(0xFF161B22)
val ObsidianSurfaceVariant = Color(0xFF21262D)
val ObsidianOutline = Color(0xFF30363D)
val ObsidianOnSurface = Color(0xFFE6EDF3)
val ObsidianOnSurfaceVariant = Color(0xFF8B949E)

// Daylight Studio Light Palette
val EmeraldPrimaryLight = Color(0xFF15803D)
val EmeraldOnPrimaryLight = Color(0xFFFFFFFF)
val EmeraldContainerLight = Color(0xFFDCFCE7)
val EmeraldOnContainerLight = Color(0xFF052E16)

val CyanSecondaryLight = Color(0xFF0369A1)
val CyanOnSecondaryLight = Color(0xFFFFFFFF)
val CyanContainerLight = Color(0xFFE0F2FE)
val CyanOnContainerLight = Color(0xFF082F49)

val AmberTertiaryLight = Color(0xFFB45309)
val AmberOnTertiaryLight = Color(0xFFFFFFFF)
val AmberContainerLight = Color(0xFFFEF3C7)
val AmberOnContainerLight = Color(0xFF451A03)

val PaperBackground = Color(0xFFF8FAFC)
val PaperSurface = Color(0xFFFFFFFF)
val PaperSurfaceVariant = Color(0xFFF1F5F9)
val PaperOutline = Color(0xFFCBD5E1)
val PaperOnSurface = Color(0xFF0F172A)
val PaperOnSurfaceVariant = Color(0xFF475569)

/**
 * Dedicated syntax-highlighting color schemes inspired by classic Notepad++ themes.
 */
data class EditorColorPalette(
    val id: String,
    val displayName: String,
    val isDark: Boolean,
    val editorBackground: Color,
    val gutterBackground: Color,
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
    val ObsidianPlus = EditorColorPalette(
        id = "obsidian",
        displayName = "Obsidian++ Dark",
        isDark = true,
        editorBackground = Color(0xFF0D1117),
        gutterBackground = Color(0xFF131922),
        gutterText = Color(0xFF6E7681),
        activeLineBackground = Color(0xFF161F2C),
        plainText = Color(0xFFE6EDF3),
        keyword = Color(0xFFFF7B72),
        stringLiteral = Color(0xFFA5D6FF),
        comment = Color(0xFF7EE787),
        numberLiteral = Color(0xFF79C0FF),
        annotationOrTag = Color(0xFFD2A8FF),
        functionOrType = Color(0xFFFFA657),
        operatorOrBracket = Color(0xFF4ADE80),
        cursorColor = Color(0xFF4ADE80),
        selectionBackground = Color(0x5522C55E)
    )

    val MonokaiPro = EditorColorPalette(
        id = "monokai",
        displayName = "Monokai Classic",
        isDark = true,
        editorBackground = Color(0xFF272822),
        gutterBackground = Color(0xFF1E1F1C),
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

    val Dracula = EditorColorPalette(
        id = "dracula",
        displayName = "Dracula Night",
        isDark = true,
        editorBackground = Color(0xFF282A36),
        gutterBackground = Color(0xFF21222C),
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

    val NotepadClassicLight = EditorColorPalette(
        id = "paper_light",
        displayName = "Notepad++ Classic Light",
        isDark = false,
        editorBackground = Color(0xFFFFFFFF),
        gutterBackground = Color(0xFFF1F5F9),
        gutterText = Color(0xFF64748B),
        activeLineBackground = Color(0xFFEFF6FF),
        plainText = Color(0xFF0F172A),
        keyword = Color(0xFF0000FF),
        stringLiteral = Color(0xFFA31515),
        comment = Color(0xFF008000),
        numberLiteral = Color(0xFF098658),
        annotationOrTag = Color(0xFF800000),
        functionOrType = Color(0xFF795E26),
        operatorOrBracket = Color(0xFF15803D),
        cursorColor = Color(0xFF15803D),
        selectionBackground = Color(0x443B82F6)
    )

    val all: List<EditorColorPalette> = listOf(
        ObsidianPlus,
        MonokaiPro,
        Dracula,
        NotepadClassicLight
    )

    fun fromId(id: String): EditorColorPalette {
        return all.firstOrNull { it.id == id } ?: ObsidianPlus
    }
}
