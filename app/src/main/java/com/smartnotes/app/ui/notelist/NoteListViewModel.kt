package com.smartnotes.app.ui.notelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnotes.app.domain.model.Note
import com.smartnotes.app.domain.usecase.DeleteNoteUseCase
import com.smartnotes.app.domain.usecase.GetNotesUseCase
import com.smartnotes.app.domain.usecase.SaveNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteListViewModel @Inject constructor(
    getNotesUseCase: GetNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val saveNoteUseCase: SaveNoteUseCase
) : ViewModel() {

    val uiState: StateFlow<NoteListUiState> = getNotesUseCase()
        .map { notes -> NoteListUiState(notes = notes, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NoteListUiState()
        )

    private val _events = Channel<NoteListEvent>(Channel.BUFFERED)
    val events: Flow<NoteListEvent> = _events.receiveAsFlow()

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
