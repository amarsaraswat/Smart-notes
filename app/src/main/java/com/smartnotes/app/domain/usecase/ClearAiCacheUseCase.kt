package com.smartnotes.app.domain.usecase

import com.smartnotes.app.domain.repository.NoteRepository
import javax.inject.Inject

class ClearAiCacheUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke() = repository.clearAllAiSummaries()
}
