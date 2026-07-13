package com.smartnotes.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getByIdWithTags(id: Long): NoteWithTags?

    @Transaction
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllWithTags(): Flow<List<NoteWithTags>>

    @Transaction
    @Query(
        """
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.id = notes_fts.rowid
        WHERE notes_fts MATCH :ftsQuery
        ORDER BY notes.updatedAt DESC
        """
    )
    fun searchWithTags(ftsQuery: String): Flow<List<NoteWithTags>>

    @Transaction
    @Query(
        """
        SELECT DISTINCT notes.* FROM notes
        JOIN note_tag_cross_ref ON notes.id = note_tag_cross_ref.noteId
        JOIN tags ON tags.id = note_tag_cross_ref.tagId
        WHERE tags.name = :tagName
        ORDER BY notes.updatedAt DESC
        """
    )
    fun getNotesByTagWithTags(tagName: String): Flow<List<NoteWithTags>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(refs: List<NoteTagCrossRef>)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun clearTagsForNote(noteId: Long)

    @Query("UPDATE notes SET lastAiSummary = :summary WHERE id = :id")
    suspend fun updateAiSummary(id: Long, summary: String)

    @Query("UPDATE notes SET lastAiSummary = NULL")
    suspend fun clearAllAiSummaries()
}
