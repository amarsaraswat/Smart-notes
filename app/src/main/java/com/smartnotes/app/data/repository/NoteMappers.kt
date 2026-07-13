package com.smartnotes.app.data.repository

import com.smartnotes.app.data.local.NoteEntity
import com.smartnotes.app.data.local.NoteWithTags
import com.smartnotes.app.domain.model.Note

fun NoteWithTags.toDomain(): Note = Note(
    id = note.id,
    title = note.title,
    content = note.content,
    createdAt = note.createdAt,
    updatedAt = note.updatedAt,
    lastAiSummary = note.lastAiSummary,
    tags = tags.map { it.name }.sorted()
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastAiSummary = lastAiSummary
)
