package com.smartnotes.app.ui.noteeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnotes.app.domain.model.Note
import com.smartnotes.app.domain.usecase.SaveNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val saveNoteUseCase: SaveNoteUseCase
) : ViewModel() {

    var title by mutableStateOf("")
        private set

    var content by mutableStateOf("")
        private set

    fun onTitleChange(value: String) {
        title = value
    }

    fun onContentChange(value: String) {
        content = value
    }

    fun save(onSaved: () -> Unit) {
        if (title.isBlank() && content.isBlank()) {
            onSaved()
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            saveNoteUseCase(
                Note(title = title, content = content, createdAt = now, updatedAt = now)
            )
            onSaved()
        }
    }
}
