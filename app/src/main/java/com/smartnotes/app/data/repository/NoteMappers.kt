package com.smartnotes.app.data.repository

import com.smartnotes.app.data.local.NoteEntity
import com.smartnotes.app.domain.model.Note

fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastAiSummary = lastAiSummary
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    tags = "",
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastAiSummary = lastAiSummary
)
