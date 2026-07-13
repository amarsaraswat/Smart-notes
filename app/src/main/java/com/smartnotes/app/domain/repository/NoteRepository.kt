package com.smartnotes.app.domain.repository

import com.smartnotes.app.domain.model.Note
import com.smartnotes.app.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    suspend fun getNoteById(id: Long): Note?
    fun getNotes(): Flow<List<Note>>
    fun searchNotes(query: String): Flow<List<Note>>
    fun getNotesByTag(tagName: String): Flow<List<Note>>
    fun getAllTags(): Flow<List<Tag>>
    suspend fun saveNote(note: Note): Long
    suspend fun deleteNoteById(id: Long)
    suspend fun updateAiSummary(noteId: Long, summary: String)
    suspend fun clearAllAiSummaries()
}
