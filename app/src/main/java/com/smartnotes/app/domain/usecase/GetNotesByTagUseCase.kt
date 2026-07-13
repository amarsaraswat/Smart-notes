package com.smartnotes.app.domain.usecase

import com.smartnotes.app.domain.model.Note
import com.smartnotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNotesByTagUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(tagName: String): Flow<List<Note>> = repository.getNotesByTag(tagName)
}
