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
    val showWhitespace: Boolean = false,
    val showIndentGuides: Boolean = true,
    val showDocumentMap: Boolean = false,
    val showFunctionList: Boolean = false,
    val overtypeMode: Boolean = false,
    val tabSize: Int = 4,
    val editorThemeId: String = EditorPalettes.NotepadClassicPc.id
)

class EditorPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.editorDataStore)

    val settingsFlow: Flow<EditorSettings> = dataStore.data.map { prefs ->
        EditorSettings(
            fontSizeSp = (prefs[KEY_FONT_SIZE] ?: 14).coerceIn(9, 26),
            wordWrap = prefs[KEY_WORD_WRAP] ?: false,
            showLineNumbers = prefs[KEY_SHOW_LINE_NUMBERS] ?: true,
            showSymbolBar = prefs[KEY_SHOW_SYMBOL_BAR] ?: true,
            showWhitespace = prefs[KEY_SHOW_WHITESPACE] ?: false,
            showIndentGuides = prefs[KEY_SHOW_INDENT_GUIDES] ?: true,
            showDocumentMap = prefs[KEY_SHOW_DOC_MAP] ?: false,
            showFunctionList = prefs[KEY_SHOW_FUNC_LIST] ?: false,
            overtypeMode = prefs[KEY_OVERTYPE_MODE] ?: false,
            tabSize = if (prefs[KEY_TAB_SIZE] == 2) 2 else 4,
            editorThemeId = prefs[KEY_EDITOR_THEME] ?: EditorPalettes.NotepadClassicPc.id
        )
    }

    suspend fun setFontSize(sizeSp: Int) {
        dataStore.edit { it[KEY_FONT_SIZE] = sizeSp.coerceIn(9, 26) }
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

    suspend fun setShowWhitespace(enabled: Boolean) {
        dataStore.edit { it[KEY_SHOW_WHITESPACE] = enabled }
    }

    suspend fun setShowIndentGuides(enabled: Boolean) {
        dataStore.edit { it[KEY_SHOW_INDENT_GUIDES] = enabled }
    }

    suspend fun setShowDocumentMap(enabled: Boolean) {
        dataStore.edit { it[KEY_SHOW_DOC_MAP] = enabled }
    }

    suspend fun setShowFunctionList(enabled: Boolean) {
        dataStore.edit { it[KEY_SHOW_FUNC_LIST] = enabled }
    }

    suspend fun setOvertypeMode(enabled: Boolean) {
        dataStore.edit { it[KEY_OVERTYPE_MODE] = enabled }
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
        val KEY_SHOW_WHITESPACE = booleanPreferencesKey("show_whitespace")
        val KEY_SHOW_INDENT_GUIDES = booleanPreferencesKey("show_indent_guides")
        val KEY_SHOW_DOC_MAP = booleanPreferencesKey("show_document_map")
        val KEY_SHOW_FUNC_LIST = booleanPreferencesKey("show_function_list")
        val KEY_OVERTYPE_MODE = booleanPreferencesKey("overtype_mode")
        val KEY_TAB_SIZE = intPreferencesKey("tab_size")
        val KEY_EDITOR_THEME = stringPreferencesKey("editor_theme_id")
    }
}
