package com.example.ui.editor

import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CodeDocument
import com.example.data.model.CodeLanguage
import com.example.data.model.StarterTemplates
import com.example.data.preferences.EditorPreferencesRepository
import com.example.data.preferences.EditorSettings
import com.example.data.repository.DocumentRepository
import com.example.domain.TextFxOperations
import com.example.domain.TextToolAction
import com.example.domain.TextToolOutcome
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class SaveStatus {
    SAVED,
    UNSAVED,
    SAVING
}

data class FindReplaceState(
    val isVisible: Boolean = false,
    val findQuery: String = "",
    val replaceQuery: String = "",
    val matchCase: Boolean = false,
    val useRegex: Boolean = false,
    val matches: List<IntRange> = emptyList(),
    val activeMatchIndex: Int = -1,
    val hasRegexError: Boolean = false
)

sealed interface EditorUiEvent {
    data class ShowMessage(val message: String) : EditorUiEvent
    data class ShowResMessage(val resId: Int) : EditorUiEvent
}

private class TabBufferState(
    var textFieldValue: TextFieldValue,
    var isDirty: Boolean = false,
    val undoStack: ArrayDeque<TextFieldValue> = ArrayDeque(),
    val redoStack: ArrayDeque<TextFieldValue> = ArrayDeque()
)

class EditorViewModel(
    private val repository: DocumentRepository,
    private val preferencesRepository: EditorPreferencesRepository,
    private val currentUserId: String
) : ViewModel() {

    private val tabBuffers = mutableMapOf<String, TabBufferState>()
    private var autoSaveJob: Job? = null

    private val _activeDocId = MutableStateFlow<String?>(null)
    val activeDocId: StateFlow<String?> = _activeDocId.asStateFlow()

    private val _editorValue = MutableStateFlow(TextFieldValue(""))
    val editorValue: StateFlow<TextFieldValue> = _editorValue.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _saveStatus = MutableStateFlow(SaveStatus.SAVED)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    private val _dirtyDocIds = MutableStateFlow<Set<String>>(emptySet())
    val dirtyDocIds: StateFlow<Set<String>> = _dirtyDocIds.asStateFlow()

    private val _explorerSearchQuery = MutableStateFlow("")
    val explorerSearchQuery: StateFlow<String> = _explorerSearchQuery.asStateFlow()

    private val _findReplaceState = MutableStateFlow(FindReplaceState())
    val findReplaceState: StateFlow<FindReplaceState> = _findReplaceState.asStateFlow()

    private val _events = MutableSharedFlow<EditorUiEvent>()
    val events = _events.asSharedFlow()

    val editorSettings: StateFlow<EditorSettings> = preferencesRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = EditorSettings()
        )

    // Two-tier Kotlin Flow error handling architecture per firebase-firestore-android skill
    val documentsState: StateFlow<UiState<List<CodeDocument>>> = repository
        .observeDocuments(currentUserId)
        .map<List<CodeDocument>, UiState<List<CodeDocument>>> { docs ->
            reconcileWithRemoteDocs(docs)
            UiState.Success(docs)
        }
        .catch { error ->
            Log.w(TAG, "Error observing documents", error)
            emit(UiState.Error(error.message ?: "Gagal memuat dokumen cloud"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private fun currentDocsList(): List<CodeDocument> {
        return (documentsState.value as? UiState.Success)?.data ?: emptyList()
    }

    fun activeDocument(docs: List<CodeDocument> = currentDocsList()): CodeDocument? {
        val id = _activeDocId.value ?: return null
        return docs.firstOrNull { it.id == id }
    }

    private fun reconcileWithRemoteDocs(docs: List<CodeDocument>) {
        val openTabs = docs.filter { it.isOpenTab }
        val currentId = _activeDocId.value

        // Populate buffer for newly arrived docs without overwriting dirty local edits
        docs.forEach { doc ->
            val existingBuffer = tabBuffers[doc.id]
            if (existingBuffer == null) {
                val safeCursor = doc.cursorPosition.toInt().coerceIn(0, doc.content.length)
                tabBuffers[doc.id] = TabBufferState(
                    textFieldValue = TextFieldValue(
                        text = doc.content,
                        selection = TextRange(safeCursor)
                    ),
                    isDirty = false
                )
            } else if (!existingBuffer.isDirty && doc.id != currentId) {
                val safeCursor = doc.cursorPosition.toInt().coerceIn(0, doc.content.length)
                existingBuffer.textFieldValue = TextFieldValue(
                    text = doc.content,
                    selection = TextRange(safeCursor)
                )
            }
        }

        // Remove deleted document buffers
        val validIds = docs.map { it.id }.toSet()
        tabBuffers.keys.retainAll(validIds)
        _dirtyDocIds.update { it.intersect(validIds) }

        if (currentId == null || currentId !in validIds || docs.firstOrNull { it.id == currentId }?.isOpenTab == false) {
            val nextDoc = openTabs.firstOrNull()
            selectTabInternal(nextDoc?.id, docs)
        }
    }

    fun selectTab(docId: String) {
        val docs = currentDocsList()
        val target = docs.firstOrNull { it.id == docId } ?: return
        // Flush previous tab if dirty
        flushCurrentTabIfDirty()

        if (!target.isOpenTab) {
            viewModelScope.launch {
                repository.updateDocument(target.copy(isOpenTab = true))
            }
        }
        selectTabInternal(docId, docs)
    }

    private fun selectTabInternal(docId: String?, docs: List<CodeDocument>) {
        _activeDocId.value = docId
        if (docId == null) {
            _editorValue.value = TextFieldValue("")
            _canUndo.value = false
            _canRedo.value = false
            _saveStatus.value = SaveStatus.SAVED
            recomputeFindMatches("")
            return
        }
        val doc = docs.firstOrNull { it.id == docId }
        val buffer = tabBuffers.getOrPut(docId) {
            val content = doc?.content ?: ""
            val cursor = (doc?.cursorPosition?.toInt() ?: 0).coerceIn(0, content.length)
            TabBufferState(TextFieldValue(content, TextRange(cursor)))
        }
        _editorValue.value = buffer.textFieldValue
        _canUndo.value = buffer.undoStack.isNotEmpty()
        _canRedo.value = buffer.redoStack.isNotEmpty()
        _saveStatus.value = if (buffer.isDirty) SaveStatus.UNSAVED else SaveStatus.SAVED
        recomputeFindMatches(buffer.textFieldValue.text)
    }

    fun onEditorValueChange(newValue: TextFieldValue) {
        val docId = _activeDocId.value ?: return
        val buffer = tabBuffers[docId] ?: return
        val previousValue = buffer.textFieldValue
        val textChanged = previousValue.text != newValue.text

        if (textChanged) {
            if (buffer.undoStack.size >= MAX_HISTORY) {
                buffer.undoStack.removeFirst()
            }
            buffer.undoStack.addLast(previousValue)
            buffer.redoStack.clear()
            buffer.isDirty = true
            _dirtyDocIds.update { it + docId }
            _saveStatus.value = SaveStatus.UNSAVED
            scheduleAutoSave(docId)
        }

        buffer.textFieldValue = newValue
        _editorValue.value = newValue
        _canUndo.value = buffer.undoStack.isNotEmpty()
        _canRedo.value = buffer.redoStack.isNotEmpty()

        if (textChanged && _findReplaceState.value.isVisible) {
            recomputeFindMatches(newValue.text)
        }
    }

    fun undo() {
        val docId = _activeDocId.value ?: return
        val buffer = tabBuffers[docId] ?: return
        if (buffer.undoStack.isEmpty()) return

        val current = buffer.textFieldValue
        val restored = buffer.undoStack.removeLast()
        buffer.redoStack.addLast(current)
        buffer.textFieldValue = restored
        buffer.isDirty = true
        _dirtyDocIds.update { it + docId }
        _editorValue.value = restored
        _canUndo.value = buffer.undoStack.isNotEmpty()
        _canRedo.value = buffer.redoStack.isNotEmpty()
        _saveStatus.value = SaveStatus.UNSAVED
        recomputeFindMatches(restored.text)
        scheduleAutoSave(docId)
    }

    fun redo() {
        val docId = _activeDocId.value ?: return
        val buffer = tabBuffers[docId] ?: return
        if (buffer.redoStack.isEmpty()) return

        val current = buffer.textFieldValue
        val restored = buffer.redoStack.removeLast()
        buffer.undoStack.addLast(current)
        buffer.textFieldValue = restored
        buffer.isDirty = true
        _dirtyDocIds.update { it + docId }
        _editorValue.value = restored
        _canUndo.value = buffer.undoStack.isNotEmpty()
        _canRedo.value = buffer.redoStack.isNotEmpty()
        _saveStatus.value = SaveStatus.UNSAVED
        recomputeFindMatches(restored.text)
        scheduleAutoSave(docId)
    }

    fun insertSymbol(symbol: String) {
        val current = _editorValue.value
        val text = current.text
        val start = current.selection.min.coerceIn(0, text.length)
        val end = current.selection.max.coerceIn(start, text.length)

        val actualInsert = if (symbol == "TAB") {
            " ".repeat(editorSettings.value.tabSize)
        } else {
            symbol
        }

        // Auto-place cursor inside paired brackets/quotes when nothing is selected
        val pairedSecondChar: Char? = if (start == end && actualInsert.length == 1) {
            when (actualInsert[0]) {
                '{' -> '}'
                '(' -> ')'
                '[' -> ']'
                '"' -> '"'
                '\'' -> '\''
                else -> null
            }
        } else {
            null
        }

        val inserted = if (pairedSecondChar != null) {
            "$actualInsert$pairedSecondChar"
        } else {
            actualInsert
        }

        val updatedText = buildString(text.length - (end - start) + inserted.length) {
            append(text, 0, start)
            append(inserted)
            append(text, end, text.length)
        }
        val newCursor = if (pairedSecondChar != null) {
            start + 1
        } else {
            start + inserted.length
        }

        onEditorValueChange(
            TextFieldValue(
                text = updatedText,
                selection = TextRange(newCursor.coerceIn(0, updatedText.length))
            )
        )
    }

    private fun scheduleAutoSave(docId: String) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DELAY_MS)
            saveDocumentById(docId, showFeedback = false)
        }
    }

    fun saveActiveDocumentNow() {
        val docId = _activeDocId.value ?: return
        autoSaveJob?.cancel()
        viewModelScope.launch {
            saveDocumentById(docId, showFeedback = true)
        }
    }

    private fun flushCurrentTabIfDirty() {
        val docId = _activeDocId.value ?: return
        val buffer = tabBuffers[docId] ?: return
        if (!buffer.isDirty) return
        autoSaveJob?.cancel()
        viewModelScope.launch {
            saveDocumentById(docId, showFeedback = false)
        }
    }

    private suspend fun saveDocumentById(docId: String, showFeedback: Boolean) {
        val doc = currentDocsList().firstOrNull { it.id == docId } ?: return
        val buffer = tabBuffers[docId] ?: return

        if (docId == _activeDocId.value) {
            _saveStatus.value = SaveStatus.SAVING
        }

        val updatedDoc = doc.copy(
            content = buffer.textFieldValue.text,
            cursorPosition = buffer.textFieldValue.selection.start.toLong()
        )
        val result = repository.updateDocument(updatedDoc)
        if (result.isSuccess) {
            buffer.isDirty = false
            _dirtyDocIds.update { it - docId }
            if (docId == _activeDocId.value) {
                _saveStatus.value = SaveStatus.SAVED
            }
            if (showFeedback) {
                _events.emit(EditorUiEvent.ShowResMessage(com.example.R.string.msg_saved_cloud))
            }
        } else {
            if (docId == _activeDocId.value) {
                _saveStatus.value = SaveStatus.UNSAVED
            }
            val msg = result.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan perubahan"
            _events.emit(EditorUiEvent.ShowMessage(msg))
        }
    }

    fun createNewDocument(
        title: String,
        language: CodeLanguage,
        useStarterTemplate: Boolean,
        customContent: String? = null
    ) {
        flushCurrentTabIfDirty()
        val initialContent = customContent
            ?: if (useStarterTemplate) StarterTemplates.starterFor(language) else ""

        viewModelScope.launch {
            val result = repository.createDocument(
                title = title,
                content = initialContent,
                language = language,
                isOpenTab = true
            )
            result.onSuccess { createdId ->
                tabBuffers[createdId] = TabBufferState(
                    textFieldValue = TextFieldValue(initialContent, TextRange(0)),
                    isDirty = false
                )
                _activeDocId.value = createdId
                _editorValue.value = TextFieldValue(initialContent, TextRange(0))
                _canUndo.value = false
                _canRedo.value = false
                _saveStatus.value = SaveStatus.SAVED
            }.onFailure { err ->
                _events.emit(
                    EditorUiEvent.ShowMessage(err.localizedMessage ?: "Gagal membuat dokumen")
                )
            }
        }
    }

    fun importLocalFile(fileName: String, content: String) {
        val detectedLang = CodeLanguage.fromFileName(fileName)
        val lineEnding = if (content.contains("\r\n")) "CRLF" else "LF"
        val normalizedContent = content.replace("\r\n", "\n")

        flushCurrentTabIfDirty()
        viewModelScope.launch {
            val res = repository.createDocument(
                title = fileName,
                content = normalizedContent,
                language = detectedLang,
                isOpenTab = true,
                lineEnding = lineEnding
            )
            res.onSuccess { createdId ->
                tabBuffers[createdId] = TabBufferState(
                    textFieldValue = TextFieldValue(normalizedContent, TextRange(0)),
                    isDirty = false
                )
                _activeDocId.value = createdId
                _editorValue.value = TextFieldValue(normalizedContent, TextRange(0))
                _events.emit(EditorUiEvent.ShowResMessage(com.example.R.string.msg_imported_file))
            }.onFailure { err ->
                _events.emit(EditorUiEvent.ShowMessage(err.localizedMessage ?: "Gagal mengimpor file"))
            }
        }
    }

    fun renameAndSetLanguage(newTitle: String, newLanguage: CodeLanguage) {
        val doc = activeDocument() ?: return
        val buffer = tabBuffers[doc.id]
        val latestContent = buffer?.textFieldValue?.text ?: doc.content
        val latestCursor = buffer?.textFieldValue?.selection?.start?.toLong() ?: doc.cursorPosition

        viewModelScope.launch {
            val res = repository.updateDocument(
                doc.copy(
                    title = newTitle.trim().ifEmpty { doc.title },
                    language = newLanguage.id,
                    content = latestContent,
                    cursorPosition = latestCursor
                )
            )
            if (res.isSuccess) {
                buffer?.isDirty = false
                _dirtyDocIds.update { it - doc.id }
                _saveStatus.value = SaveStatus.SAVED
            }
        }
    }

    fun setActiveLanguage(language: CodeLanguage) {
        val doc = activeDocument() ?: return
        renameAndSetLanguage(doc.title, language)
    }

    fun togglePin(doc: CodeDocument) {
        val buffer = tabBuffers[doc.id]
        val latestContent = buffer?.textFieldValue?.text ?: doc.content
        viewModelScope.launch {
            repository.updateDocument(
                doc.copy(
                    isPinned = !doc.isPinned,
                    content = latestContent
                )
            )
        }
    }

    fun closeTab(doc: CodeDocument) {
        val buffer = tabBuffers[doc.id]
        val latestContent = buffer?.textFieldValue?.text ?: doc.content
        val latestCursor = buffer?.textFieldValue?.selection?.start?.toLong() ?: doc.cursorPosition

        val remainingTabs = currentDocsList().filter { it.isOpenTab && it.id != doc.id }
        if (_activeDocId.value == doc.id) {
            selectTabInternal(remainingTabs.firstOrNull()?.id, currentDocsList())
        }

        viewModelScope.launch {
            repository.updateDocument(
                doc.copy(
                    isOpenTab = false,
                    content = latestContent,
                    cursorPosition = latestCursor
                )
            )
            buffer?.isDirty = false
            _dirtyDocIds.update { it - doc.id }
        }
    }

    fun deleteDocument(doc: CodeDocument) {
        autoSaveJob?.cancel()
        val remainingTabs = currentDocsList().filter { it.isOpenTab && it.id != doc.id }
        if (_activeDocId.value == doc.id) {
            selectTabInternal(remainingTabs.firstOrNull()?.id, currentDocsList())
        }
        tabBuffers.remove(doc.id)
        _dirtyDocIds.update { it - doc.id }

        viewModelScope.launch {
            repository.deleteDocument(doc.id)
        }
    }

    fun goToLine(lineNumberOneBased: Int) {
        val current = _editorValue.value
        val offset = TextFxOperations.offsetForLine(current.text, lineNumberOneBased)
        val updated = current.copy(selection = TextRange(offset))
        _editorValue.value = updated
        _activeDocId.value?.let { id -> tabBuffers[id]?.textFieldValue = updated }
    }

    fun applyTextTool(action: TextToolAction) {
        val current = _editorValue.value
        when (val outcome = TextFxOperations.applyTextTool(action, current.text, current.selection)) {
            is TextToolOutcome.Transformed -> {
                onEditorValueChange(
                    TextFieldValue(text = outcome.text, selection = outcome.selection)
                )
            }
            is TextToolOutcome.ValidationError -> {
                viewModelScope.launch {
                    val resId = when (outcome.errorCode) {
                        TextToolOutcome.ErrorKind.INVALID_JSON -> com.example.R.string.msg_invalid_json
                        TextToolOutcome.ErrorKind.INVALID_BASE64 -> com.example.R.string.msg_invalid_base64
                    }
                    _events.emit(EditorUiEvent.ShowResMessage(resId))
                }
            }
        }
    }

    fun toggleLineEnding() {
        val doc = activeDocument() ?: return
        val next = if (doc.lineEnding == "CRLF") "LF" else "CRLF"
        val buffer = tabBuffers[doc.id]
        val latestContent = buffer?.textFieldValue?.text ?: doc.content
        viewModelScope.launch {
            repository.updateDocument(doc.copy(lineEnding = next, content = latestContent))
        }
    }

    // --- Explorer Search ---
    fun setExplorerSearchQuery(query: String) {
        _explorerSearchQuery.value = query
    }

    // --- Find & Replace ---
    fun toggleFindBar(visible: Boolean = !_findReplaceState.value.isVisible) {
        _findReplaceState.update { it.copy(isVisible = visible) }
        if (visible) {
            recomputeFindMatches(_editorValue.value.text)
        } else {
            _findReplaceState.update { it.copy(matches = emptyList(), activeMatchIndex = -1) }
        }
    }

    fun updateFindQuery(query: String) {
        _findReplaceState.update { it.copy(findQuery = query) }
        recomputeFindMatches(_editorValue.value.text, jumpToFirstMatch = true)
    }

    fun updateReplaceQuery(replacement: String) {
        _findReplaceState.update { it.copy(replaceQuery = replacement) }
    }

    fun toggleMatchCase() {
        _findReplaceState.update { it.copy(matchCase = !it.matchCase) }
        recomputeFindMatches(_editorValue.value.text, jumpToFirstMatch = true)
    }

    fun toggleUseRegex() {
        _findReplaceState.update { it.copy(useRegex = !it.useRegex) }
        recomputeFindMatches(_editorValue.value.text, jumpToFirstMatch = true)
    }

    private fun recomputeFindMatches(text: String, jumpToFirstMatch: Boolean = false) {
        val state = _findReplaceState.value
        if (!state.isVisible || state.findQuery.isEmpty()) {
            _findReplaceState.update {
                it.copy(matches = emptyList(), activeMatchIndex = -1, hasRegexError = false)
            }
            return
        }

        val res = TextFxOperations.findMatches(
            text = text,
            query = state.findQuery,
            matchCase = state.matchCase,
            useRegex = state.useRegex
        )
        res.onSuccess { found ->
            val nextIdx = when {
                found.isEmpty() -> -1
                jumpToFirstMatch || state.activeMatchIndex !in found.indices -> 0
                else -> state.activeMatchIndex
            }
            _findReplaceState.update {
                it.copy(matches = found, activeMatchIndex = nextIdx, hasRegexError = false)
            }
            if (jumpToFirstMatch && nextIdx in found.indices) {
                selectMatchRange(found[nextIdx])
            }
        }.onFailure {
            _findReplaceState.update {
                it.copy(matches = emptyList(), activeMatchIndex = -1, hasRegexError = true)
            }
        }
    }

    fun findNext() {
        val state = _findReplaceState.value
        if (state.matches.isEmpty()) return
        val next = (state.activeMatchIndex + 1) % state.matches.size
        _findReplaceState.update { it.copy(activeMatchIndex = next) }
        selectMatchRange(state.matches[next])
    }

    fun findPrevious() {
        val state = _findReplaceState.value
        if (state.matches.isEmpty()) return
        val prev = if (state.activeMatchIndex <= 0) state.matches.lastIndex else state.activeMatchIndex - 1
        _findReplaceState.update { it.copy(activeMatchIndex = prev) }
        selectMatchRange(state.matches[prev])
    }

    private fun selectMatchRange(range: IntRange) {
        val current = _editorValue.value
        val start = range.first.coerceIn(0, current.text.length)
        val end = (range.last + 1).coerceIn(start, current.text.length)
        val updated = current.copy(selection = TextRange(start, end))
        _editorValue.value = updated
        _activeDocId.value?.let { id -> tabBuffers[id]?.textFieldValue = updated }
    }

    fun replaceCurrentMatch() {
        val state = _findReplaceState.value
        if (state.matches.isEmpty()) return
        val idx = state.activeMatchIndex.coerceIn(0, state.matches.lastIndex)
        val target = state.matches[idx]
        val (newText, newCursor) = TextFxOperations.replaceOne(
            text = _editorValue.value.text,
            targetRange = target,
            replacement = state.replaceQuery,
            query = state.findQuery,
            matchCase = state.matchCase,
            useRegex = state.useRegex
        )
        onEditorValueChange(TextFieldValue(text = newText, selection = TextRange(newCursor)))
    }

    fun replaceAllMatches() {
        val state = _findReplaceState.value
        if (state.findQuery.isEmpty()) return
        val res = TextFxOperations.replaceAll(
            text = _editorValue.value.text,
            query = state.findQuery,
            replacement = state.replaceQuery,
            matchCase = state.matchCase,
            useRegex = state.useRegex
        )
        res.onSuccess { (newText, count) ->
            if (count > 0) {
                onEditorValueChange(
                    TextFieldValue(
                        text = newText,
                        selection = TextRange(_editorValue.value.selection.start.coerceAtMost(newText.length))
                    )
                )
                viewModelScope.launch {
                    _events.emit(EditorUiEvent.ShowMessage("$count kecocokan berhasil diganti"))
                }
            }
        }
    }

    // --- Preferences Mutators ---
    fun setFontSize(sizeSp: Int) {
        viewModelScope.launch { preferencesRepository.setFontSize(sizeSp) }
    }

    fun setWordWrap(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setWordWrap(enabled) }
    }

    fun setShowLineNumbers(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowLineNumbers(enabled) }
    }

    fun setShowSymbolBar(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowSymbolBar(enabled) }
    }

    fun setTabSize(spaces: Int) {
        viewModelScope.launch { preferencesRepository.setTabSize(spaces) }
    }

    fun setEditorTheme(themeId: String) {
        viewModelScope.launch { preferencesRepository.setEditorTheme(themeId) }
    }

    fun notifyMessageRes(resId: Int) {
        viewModelScope.launch { _events.emit(EditorUiEvent.ShowResMessage(resId)) }
    }

    private companion object {
        const val TAG = "EditorViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
        const val AUTO_SAVE_DELAY_MS = 1400L
        const val MAX_HISTORY = 45
    }
}
