package com.smartnotes.app.domain.repository

import com.smartnotes.app.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getNotes(): Flow<List<Note>>
    suspend fun saveNote(note: Note): Long
}
