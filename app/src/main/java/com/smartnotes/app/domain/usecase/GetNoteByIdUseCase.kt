package com.smartnotes.app.domain.usecase

import com.smartnotes.app.domain.model.Note
import com.smartnotes.app.domain.repository.NoteRepository
import javax.inject.Inject

class GetNoteByIdUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(id: Long): Note? = repository.getNoteById(id)
}
