package com.smartnotes.app.ui.notelist

import com.smartnotes.app.domain.model.Note

data class NoteListUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true
)
