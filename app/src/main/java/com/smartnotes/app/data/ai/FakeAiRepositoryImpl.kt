package com.smartnotes.app.data.ai

import com.smartnotes.app.domain.model.AiAction
import com.smartnotes.app.domain.model.AiStreamState
import com.smartnotes.app.domain.model.Note
import com.smartnotes.app.domain.model.RewriteTone
import com.smartnotes.app.domain.repository.AiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

private const val THINKING_DELAY_MS = 400L
private const val WORD_DELAY_MS = 40L

// Local, on-device stand-in for the Gemini-backed AiRepository (see claude.md
// section 5) — same Flow<AiStreamState> contract, so swapping in a real API
// client later only means providing a different AiRepository implementation.
class FakeAiRepositoryImpl @Inject constructor() : AiRepository {

    override fun streamAction(note: Note, action: AiAction): Flow<AiStreamState> = flow {
        emit(AiStreamState.Loading)
        delay(THINKING_DELAY_MS)

        val fullText = when (action) {
            is AiAction.Summarize -> genericSummary(note.content)
            is AiAction.Rewrite -> genericRewrite(note.content, action.tone)
            is AiAction.AskQuestion -> answerQuestion(note, action.question)
        }

        val words = fullText.split(" ")
        val builder = StringBuilder()
        for (word in words) {
            if (builder.isNotEmpty()) builder.append(" ")
            builder.append(word)
            emit(AiStreamState.Streaming(builder.toString()))
            delay(WORD_DELAY_MS)
        }
        emit(AiStreamState.Done(fullText))
    }.flowOn(Dispatchers.Default)

    private fun splitSentences(text: String): List<String> {
        val normalized = text.replace("\n", " ")
        val sentences = Regex("[^.!?]+[.!?]*")
            .findAll(normalized)
            .map { it.value.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        return sentences.ifEmpty { listOf(text) }
    }

    private fun genericSummary(body: String): String =
        splitSentences(body).take(2).joinToString(" ")

    private fun genericRewrite(body: String, tone: RewriteTone): String {
        val sentences = splitSentences(body)
        val gist = sentences.take(3).joinToString(" ")
        return when (tone) {
            RewriteTone.FORMAL -> "Please find a formal restatement of this entry below: $gist"
            RewriteTone.CASUAL -> "hey — so basically: $gist"
            RewriteTone.CONCISE -> sentences.take(1).joinToString(" ")
            RewriteTone.EXPANDED ->
                "$body\n\nAdditional context: this entry touches on several points worth " +
                    "elaborating, including the intent behind each item and how it connects " +
                    "to ongoing priorities."
        }
    }

    private fun answerQuestion(note: Note, question: String): String {
        val bodyLower = note.content.lowercase()
        val words = question.lowercase()
            .replace(Regex("[?.,!]"), "")
            .split(Regex("\\s+"))
            .filter { it.length > 3 }
        val found = words.any { bodyLower.contains(it) }
        if (!found) {
            return "I couldn't find anything in this note related to that — it may not be covered here."
        }
        val sentences = splitSentences(note.content)
        val hit = sentences.firstOrNull { s -> words.any { s.lowercase().contains(it) } }
            ?: sentences.firstOrNull()
            ?: note.content
        return "Based on this note: $hit"
    }
}
