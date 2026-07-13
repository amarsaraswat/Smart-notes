package com.smartnotes.app.domain.repository

import com.smartnotes.app.domain.model.AiAction
import com.smartnotes.app.domain.model.AiStreamState
import com.smartnotes.app.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface AiRepository {
    fun streamAction(note: Note, action: AiAction): Flow<AiStreamState>
}
