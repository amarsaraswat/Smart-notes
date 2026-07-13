package com.smartnotes.app.domain.usecase

import com.smartnotes.app.domain.model.Note
import com.smartnotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(query: String): Flow<List<Note>> =
        if (query.isBlank()) repository.getNotes() else repository.searchNotes(query)
}
