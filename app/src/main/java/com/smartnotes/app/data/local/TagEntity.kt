package com.smartnotes.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Schema decision: join table over a comma-separated `tags` column on NoteEntity.
//
// A comma-separated column (the original NoteEntity.tags field) can't be indexed,
// can't enforce a canonical spelling for a tag, and turns "rename a tag" or
// "delete a tag everywhere" into an app-level string-rewrite across every note
// row instead of a single UPDATE/DELETE. It also can't answer "all notes with
// tag X" without a full-table LIKE scan. A normalized many-to-many (TagEntity +
// NoteTagCrossRef) gives O(1) tag lookup by name (unique index below), a single
// place to rename/delete a tag, and a proper JOIN for filter-by-tag (UC-6) and
// for autocomplete suggestions, at the cost of one extra join versus a raw
// string split. For a notes app where tag filtering and autocomplete are actual
// product requirements, that trade is worth it.
@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
