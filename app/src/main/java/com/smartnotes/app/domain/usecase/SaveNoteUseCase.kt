package com.smartnotes.app.domain.usecase

import com.smartnotes.app.domain.model.Note
import com.smartnotes.app.domain.repository.NoteRepository
import javax.inject.Inject

class SaveNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note): Long = repository.saveNote(note)
}
