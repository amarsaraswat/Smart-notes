package com.smartnotes.app.ui.notelist

import com.smartnotes.app.domain.model.Note

sealed interface NoteListEvent {
    data class NoteDeleted(val note: Note) : NoteListEvent
}
