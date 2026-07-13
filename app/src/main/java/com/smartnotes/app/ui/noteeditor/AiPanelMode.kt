package com.smartnotes.app.ui.noteeditor

enum class AiPanelMode {
    NO_KEY, SUMMARIZE, REWRITE, ASK
}

data class AskEntry(val question: String, val answer: String)
