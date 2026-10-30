package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.EditorPalettes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.editorDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "codepad_editor_preferences"
)

data class EditorSettings(
    val fontSizeSp: Int = 14,
    val wordWrap: Boolean = false,
    val showLineNumbers: Boolean = true,
    val showSymbolBar: Boolean = true,
    val tabSize: Int = 4,
    val editorThemeId: String = EditorPalettes.ObsidianPlus.id
)

class EditorPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.editorDataStore)

    val settingsFlow: Flow<EditorSettings> = dataStore.data.map { prefs ->
        EditorSettings(
            fontSizeSp = (prefs[KEY_FONT_SIZE] ?: 14).coerceIn(10, 24),
            wordWrap = prefs[KEY_WORD_WRAP] ?: false,
            showLineNumbers = prefs[KEY_SHOW_LINE_NUMBERS] ?: true,
            showSymbolBar = prefs[KEY_SHOW_SYMBOL_BAR] ?: true,
            tabSize = if (prefs[KEY_TAB_SIZE] == 2) 2 else 4,
            editorThemeId = prefs[KEY_EDITOR_THEME] ?: EditorPalettes.ObsidianPlus.id
        )
    }

    suspend fun setFontSize(sizeSp: Int) {
        dataStore.edit { it[KEY_FONT_SIZE] = sizeSp.coerceIn(10, 24) }
    }

    suspend fun setWordWrap(enabled: Boolean) {
        dataStore.edit { it[KEY_WORD_WRAP] = enabled }
    }

    suspend fun setShowLineNumbers(enabled: Boolean) {
        dataStore.edit { it[KEY_SHOW_LINE_NUMBERS] = enabled }
    }

    suspend fun setShowSymbolBar(enabled: Boolean) {
        dataStore.edit { it[KEY_SHOW_SYMBOL_BAR] = enabled }
    }

    suspend fun setTabSize(spaces: Int) {
        dataStore.edit { it[KEY_TAB_SIZE] = if (spaces == 2) 2 else 4 }
    }

    suspend fun setEditorTheme(themeId: String) {
        dataStore.edit { it[KEY_EDITOR_THEME] = themeId }
    }

    private companion object {
        val KEY_FONT_SIZE = intPreferencesKey("font_size_sp")
        val KEY_WORD_WRAP = booleanPreferencesKey("word_wrap")
        val KEY_SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
        val KEY_SHOW_SYMBOL_BAR = booleanPreferencesKey("show_symbol_bar")
        val KEY_TAB_SIZE = intPreferencesKey("tab_size")
        val KEY_EDITOR_THEME = stringPreferencesKey("editor_theme_id")
    }
}
