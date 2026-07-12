package com.smartnotes.app.ui.noteeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnotes.app.domain.model.Note
import com.smartnotes.app.domain.usecase.DeleteNoteUseCase
import com.smartnotes.app.domain.usecase.SaveNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val AUTO_SAVE_DEBOUNCE_MS = 600L

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val saveNoteUseCase: SaveNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase
) : ViewModel() {

    var title by mutableStateOf("")
        private set

    var content by mutableStateOf("")
        private set

    // Null until the first non-blank auto-save persists a row; used to upsert on
    // subsequent edits instead of inserting a new row each time.
    private var noteId: Long? = null
    private var createdAt: Long? = null
    private var autoSaveJob: Job? = null

    fun onTitleChange(value: String) {
        title = value
        scheduleAutoSave()
    }

    fun onContentChange(value: String) {
        content = value
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
        viewModelScope.launch {
            persist()
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
                updatedAt = now
            )
        )
        noteId = noteId ?: savedId
        createdAt = startedAt
    }
}
