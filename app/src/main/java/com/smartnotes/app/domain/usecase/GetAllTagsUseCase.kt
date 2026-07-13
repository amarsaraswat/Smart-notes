package com.smartnotes.app.domain.usecase

import com.smartnotes.app.domain.model.Tag
import com.smartnotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllTagsUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<Tag>> = repository.getAllTags()
}
