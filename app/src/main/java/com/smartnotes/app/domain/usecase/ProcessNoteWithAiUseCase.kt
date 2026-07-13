package com.smartnotes.app.domain.usecase

import com.smartnotes.app.domain.model.AiAction
import com.smartnotes.app.domain.model.AiStreamState
import com.smartnotes.app.domain.model.Note
import com.smartnotes.app.domain.repository.AiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProcessNoteWithAiUseCase @Inject constructor(
    private val repository: AiRepository
) {
    operator fun invoke(note: Note, action: AiAction): Flow<AiStreamState> =
        repository.streamAction(note, action)
}
