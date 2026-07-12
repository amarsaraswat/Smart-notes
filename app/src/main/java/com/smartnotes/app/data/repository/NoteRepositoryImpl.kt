package com.smartnotes.app.data.repository

import com.smartnotes.app.data.local.NoteDao
import com.smartnotes.app.domain.model.Note
import com.smartnotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao
) : NoteRepository {

    override fun getNotes(): Flow<List<Note>> =
        noteDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveNote(note: Note): Long =
        noteDao.insert(note.toEntity())

    override suspend fun deleteNoteById(id: Long) =
        noteDao.deleteById(id)
}
