package com.smartnotes.app.data.repository

import androidx.room.withTransaction
import com.smartnotes.app.data.local.AppDatabase
import com.smartnotes.app.data.local.NoteDao
import com.smartnotes.app.data.local.NoteTagCrossRef
import com.smartnotes.app.data.local.TagDao
import com.smartnotes.app.data.local.TagEntity
import com.smartnotes.app.domain.model.Note
import com.smartnotes.app.domain.model.Tag
import com.smartnotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val appDatabase: AppDatabase
) : NoteRepository {

    override suspend fun getNoteById(id: Long): Note? =
        noteDao.getByIdWithTags(id)?.toDomain()

    override fun getNotes(): Flow<List<Note>> =
        noteDao.getAllWithTags().map { entities -> entities.map { it.toDomain() } }

    override fun searchNotes(query: String): Flow<List<Note>> {
        val ftsQuery = buildFtsMatchQuery(query)
        if (ftsQuery.isBlank()) return getNotes()
        return noteDao.searchWithTags(ftsQuery).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getNotesByTag(tagName: String): Flow<List<Note>> =
        noteDao.getNotesByTagWithTags(tagName).map { entities -> entities.map { it.toDomain() } }

    override fun getAllTags(): Flow<List<Tag>> =
        tagDao.getAll().map { entities -> entities.map { Tag(id = it.id, name = it.name) } }

    override suspend fun saveNote(note: Note): Long = appDatabase.withTransaction {
        val id = noteDao.insert(note.toEntity())
        noteDao.clearTagsForNote(id)
        val tagIds = note.tags
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .map { name -> resolveTagId(name) }
        if (tagIds.isNotEmpty()) {
            noteDao.insertCrossRefs(tagIds.map { tagId -> NoteTagCrossRef(noteId = id, tagId = tagId) })
        }
        id
    }

    override suspend fun deleteNoteById(id: Long) =
        noteDao.deleteById(id)

    override suspend fun updateAiSummary(noteId: Long, summary: String) =
        noteDao.updateAiSummary(noteId, summary)

    override suspend fun clearAllAiSummaries() =
        noteDao.clearAllAiSummaries()

    // insert() with IGNORE returns -1 on a name conflict (tag already exists),
    // so fall back to looking up the id that already owns that name.
    private suspend fun resolveTagId(name: String): Long {
        val insertedId = tagDao.insert(TagEntity(name = name))
        return if (insertedId != -1L) insertedId else requireNotNull(tagDao.getIdByName(name))
    }

    // Prefix-matches each whitespace-separated token (FTS4 "term*" syntax),
    // stripped to alphanumerics so stray punctuation can't break MATCH syntax.
    private fun buildFtsMatchQuery(rawQuery: String): String =
        rawQuery.trim()
            .split(Regex("\\s+"))
            .mapNotNull { token -> token.filter { it.isLetterOrDigit() }.takeIf { it.isNotEmpty() } }
            .joinToString(" ") { "$it*" }
}
