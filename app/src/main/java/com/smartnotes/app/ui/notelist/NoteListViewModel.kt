package com.smartnotes.app.ui.notelist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnotes.app.domain.model.Note
import com.smartnotes.app.domain.usecase.DeleteNoteUseCase
import com.smartnotes.app.domain.usecase.GetAllTagsUseCase
import com.smartnotes.app.domain.usecase.GetNotesByTagUseCase
import com.smartnotes.app.domain.usecase.SaveNoteUseCase
import com.smartnotes.app.domain.usecase.SearchNotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "NoteListViewModel"

// UC-6: the note list itself only filters by tag; free-text search (UC-5)
// lives on its own screen now (see ui/search).
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NoteListViewModel @Inject constructor(
    searchNotesUseCase: SearchNotesUseCase,
    getNotesByTagUseCase: GetNotesByTagUseCase,
    getAllTagsUseCase: GetAllTagsUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val saveNoteUseCase: SaveNoteUseCase
) : ViewModel() {

    private val selectedTag = MutableStateFlow<String?>(null)

    private val filteredNotes: Flow<List<Note>> = selectedTag
        .flatMapLatest { tag ->
            Log.d(TAG, "Executing query for tag=${tag ?: "none"}")
            if (tag != null) getNotesByTagUseCase(tag) else searchNotesUseCase("")
        }

    val uiState: StateFlow<NoteListUiState> = combine(
        filteredNotes,
        getAllTagsUseCase().map { tags -> tags.map { it.name } },
        selectedTag
    ) { notes, allTags, tag ->
        NoteListUiState(notes = notes, isLoading = false, allTags = allTags, selectedTag = tag)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NoteListUiState()
    )

    private val _events = Channel<NoteListEvent>(Channel.BUFFERED)
    val events: Flow<NoteListEvent> = _events.receiveAsFlow()

    // UC-6: tapping a selected tag chip again clears the filter.
    fun onTagFilterToggle(tag: String) {
        selectedTag.value = if (selectedTag.value == tag) null else tag
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            deleteNoteUseCase(note.id)
            _events.send(NoteListEvent.NoteDeleted(note))
        }
    }

    fun undoDelete(note: Note) {
        viewModelScope.launch {
            saveNoteUseCase(note)
        }
    }
}
