package com.smartnotes.app.domain.usecase

import com.smartnotes.app.domain.repository.NoteRepository
import javax.inject.Inject

class CacheAiSummaryUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(noteId: Long, summary: String) =
        repository.updateAiSummary(noteId, summary)
}
