package com.smartnotes.app.domain.model

sealed class AiAction {
    data object Summarize : AiAction()
    data class Rewrite(val tone: RewriteTone) : AiAction()
    data class AskQuestion(val question: String) : AiAction()
}
