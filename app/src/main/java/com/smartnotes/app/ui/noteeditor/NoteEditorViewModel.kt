package com.smartnotes.app.ui.noteeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnotes.app.domain.model.AiAction
import com.smartnotes.app.domain.model.AiStreamState
import com.smartnotes.app.domain.model.Note
import com.smartnotes.app.domain.model.RewriteTone
import com.smartnotes.app.domain.repository.UserPreferencesRepository
import com.smartnotes.app.domain.usecase.CacheAiSummaryUseCase
import com.smartnotes.app.domain.usecase.DeleteNoteUseCase
import com.smartnotes.app.domain.usecase.GetAllTagsUseCase
import com.smartnotes.app.domain.usecase.GetNoteByIdUseCase
import com.smartnotes.app.domain.usecase.ProcessNoteWithAiUseCase
import com.smartnotes.app.domain.usecase.SaveNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val AUTO_SAVE_DEBOUNCE_MS = 600L
const val NOTE_ID_ARG = "noteId"
const val NO_NOTE_ID = -1L

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val saveNoteUseCase: SaveNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val getNoteByIdUseCase: GetNoteByIdUseCase,
    private val processNoteWithAiUseCase: ProcessNoteWithAiUseCase,
    private val cacheAiSummaryUseCase: CacheAiSummaryUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    getAllTagsUseCase: GetAllTagsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var title by mutableStateOf("")
        private set

    var content by mutableStateOf("")
        private set

    var tags by mutableStateOf<List<String>>(emptyList())
        private set

    var tagInput by mutableStateOf("")
        private set

    var cachedSummary by mutableStateOf<String?>(null)
        private set

    // AI panel state
    var aiPanelOpen by mutableStateOf(false)
        private set
    var aiMode by mutableStateOf<AiPanelMode?>(null)
        private set
    var summaryText by mutableStateOf("")
        private set
    var summarizing by mutableStateOf(false)
        private set
    var rewriteTone by mutableStateOf<RewriteTone?>(null)
        private set
    var rewriteText by mutableStateOf("")
        private set
    var rewriting by mutableStateOf(false)
        private set
    var rewriteDone by mutableStateOf(false)
        private set
    var askInput by mutableStateOf("")
        private set
    var askCurrentQuestion by mutableStateOf("")
        private set
    var askAnswerStream by mutableStateOf("")
        private set
    var asking by mutableStateOf(false)
        private set
    var askHistory by mutableStateOf<List<AskEntry>>(emptyList())
        private set

    val allTagNames: StateFlow<List<String>> = getAllTagsUseCase()
        .map { list -> list.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var noteId: Long? = null
    private var createdAt: Long? = null
    private var autoSaveJob: Job? = null
    private var aiJob: Job? = null

    init {
        val existingId = savedStateHandle.get<Long>(NOTE_ID_ARG) ?: NO_NOTE_ID
        if (existingId != NO_NOTE_ID) {
            viewModelScope.launch {
                getNoteByIdUseCase(existingId)?.let { note -> loadNote(note) }
            }
        }
    }

    private fun loadNote(note: Note) {
        noteId = note.id
        createdAt = note.createdAt
        title = note.title
        content = note.content
        tags = note.tags
        cachedSummary = note.lastAiSummary
    }

    fun onTitleChange(value: String) {
        title = value
        scheduleAutoSave()
    }

    fun onContentChange(value: String) {
        content = value
        scheduleAutoSave()
    }

    fun onTagInputChange(value: String) {
        // A comma or newline in fast-typed/pasted text also commits the tag,
        // matching how most tag inputs behave.
        if (value.endsWith(",") || value.endsWith("\n")) {
            commitTagInput(value.dropLast(1))
        } else {
            tagInput = value
        }
    }

    fun commitTagInput(raw: String = tagInput) {
        addTag(raw)
        tagInput = ""
    }

    private fun addTag(raw: String) {
        val name = raw.trim()
        if (name.isEmpty()) return
        if (tags.any { it.equals(name, ignoreCase = true) }) return
        tags = tags + name
        scheduleAutoSave()
    }

    fun removeTag(name: String) {
        tags = tags.filterNot { it.equals(name, ignoreCase = true) }
        scheduleAutoSave()
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DEBOUNCE_MS)
            persist()
        }
    }

    // UC-1: called when leaving the editor. Flushes any pending debounced edit
    // immediately, and discards (deletes) the note if it ends up blank so no
    // empty rows are left behind.
    fun flushAndExit(onDone: () -> Unit) {
        autoSaveJob?.cancel()
        autoSaveJob = null
        if (tagInput.isNotBlank()) commitTagInput()
        viewModelScope.launch {
            persist()
            onDone()
        }
    }

    fun deleteNote(onDone: () -> Unit) {
        autoSaveJob?.cancel()
        aiJob?.cancel()
        val id = noteId ?: run { onDone(); return }
        viewModelScope.launch {
            deleteNoteUseCase(id)
            onDone()
        }
    }

    private suspend fun persist() {
        if (title.isBlank() && content.isBlank()) {
            noteId?.let { id ->
                deleteNoteUseCase(id)
                noteId = null
                createdAt = null
            }
            return
        }

        val now = System.currentTimeMillis()
        val startedAt = createdAt ?: now
        val savedId = saveNoteUseCase(
            Note(
                id = noteId ?: 0,
                title = title,
                content = content,
                createdAt = startedAt,
                updatedAt = now,
                lastAiSummary = cachedSummary,
                tags = tags
            )
        )
        noteId = noteId ?: savedId
        createdAt = startedAt
    }

    private fun currentNote(): Note = Note(
        id = noteId ?: 0,
        title = title,
        content = content,
        createdAt = createdAt ?: System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        lastAiSummary = cachedSummary,
        tags = tags
    )

    fun openAiPanel(mode: AiPanelMode) {
        aiJob?.cancel()
        viewModelScope.launch {
            val apiKeySet = userPreferencesRepository.preferences.first().apiKey.isNotBlank()
            if (!apiKeySet) {
                aiPanelOpen = true
                aiMode = AiPanelMode.NO_KEY
                return@launch
            }
            aiPanelOpen = true
            aiMode = mode
            rewriteTone = null
            rewriteText = ""
            rewriteDone = false
            summaryText = ""
            askInput = ""
            askHistory = emptyList()
            askAnswerStream = ""
            if (mode == AiPanelMode.SUMMARIZE) startSummarize()
        }
    }

    fun closeAiPanel() {
        aiJob?.cancel()
        summarizing = false
        rewriting = false
        asking = false
        aiPanelOpen = false
        aiMode = null
    }

    private fun startSummarize() {
        cachedSummary?.let { cached ->
            summaryText = cached
            summarizing = false
            return
        }
        summarizing = true
        summaryText = ""
        aiJob = viewModelScope.launch {
            processNoteWithAiUseCase(currentNote(), AiAction.Summarize).collect { state ->
                when (state) {
                    is AiStreamState.Streaming -> summaryText = state.partialText
                    is AiStreamState.Done -> {
                        summaryText = state.fullText
                        summarizing = false
                        cachedSummary = state.fullText
                        noteId?.let { id -> cacheAiSummaryUseCase(id, state.fullText) }
                    }
                    is AiStreamState.Error -> summarizing = false
                    else -> Unit
                }
            }
        }
    }

    fun selectTone(tone: RewriteTone) {
        aiJob?.cancel()
        rewriteTone = tone
        rewriting = true
        rewriteText = ""
        rewriteDone = false
        aiJob = viewModelScope.launch {
            processNoteWithAiUseCase(currentNote(), AiAction.Rewrite(tone)).collect { state ->
                when (state) {
                    is AiStreamState.Streaming -> rewriteText = state.partialText
                    is AiStreamState.Done -> {
                        rewriteText = state.fullText
                        rewriting = false
                        rewriteDone = true
                    }
                    is AiStreamState.Error -> rewriting = false
                    else -> Unit
                }
            }
        }
    }

    fun replaceRewrite() {
        content = rewriteText
        scheduleAutoSave()
        aiPanelOpen = false
        aiMode = null
        rewriteDone = false
        rewriteText = ""
        rewriteTone = null
    }

    fun discardRewrite() {
        rewriteText = ""
        rewriteDone = false
        rewriteTone = null
    }

    fun onAskInputChange(value: String) {
        askInput = value
    }

    fun askSubmit() {
        val question = askInput.trim()
        if (question.isEmpty() || asking) return
        aiJob?.cancel()
        asking = true
        askAnswerStream = ""
        askInput = ""
        askCurrentQuestion = question
        aiJob = viewModelScope.launch {
            processNoteWithAiUseCase(currentNote(), AiAction.AskQuestion(question)).collect { state ->
                when (state) {
                    is AiStreamState.Streaming -> askAnswerStream = state.partialText
                    is AiStreamState.Done -> {
                        askHistory = askHistory + AskEntry(question, state.fullText)
                        askAnswerStream = ""
                        askCurrentQuestion = ""
                        asking = false
                    }
                    is AiStreamState.Error -> asking = false
                    else -> Unit
                }
            }
        }
    }
}
