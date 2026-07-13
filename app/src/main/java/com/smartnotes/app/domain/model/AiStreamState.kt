package com.smartnotes.app.domain.model

sealed class AiStreamState {
    data object Idle : AiStreamState()
    data object Loading : AiStreamState()
    data class Streaming(val partialText: String) : AiStreamState()
    data class Done(val fullText: String) : AiStreamState()
    data class Error(val message: String) : AiStreamState()
}
