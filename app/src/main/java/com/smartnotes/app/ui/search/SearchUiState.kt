package com.smartnotes.app.ui.search

import com.smartnotes.app.domain.model.Note

data class SearchUiState(
    val notes: List<Note> = emptyList()
)
