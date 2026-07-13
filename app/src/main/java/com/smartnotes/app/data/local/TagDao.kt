package com.smartnotes.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Query("SELECT id FROM tags WHERE name = :name LIMIT 1")
    suspend fun getIdByName(name: String): Long?

    // Only tags currently attached to at least one note, so a deleted note's tags
    // don't linger forever as dead filter/autocomplete options.
    @Query(
        """
        SELECT DISTINCT tags.* FROM tags
        JOIN note_tag_cross_ref ON tags.id = note_tag_cross_ref.tagId
        ORDER BY tags.name ASC
        """
    )
    fun getAll(): Flow<List<TagEntity>>
}
