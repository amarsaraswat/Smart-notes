package com.smartnotes.app.ui.noteeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartnotes.app.domain.model.RewriteTone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPanel(
    mode: AiPanelMode,
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit,
    summarizing: Boolean,
    summaryText: String,
    rewriteTone: RewriteTone?,
    rewriteText: String,
    rewriting: Boolean,
    rewriteDone: Boolean,
    onSelectTone: (RewriteTone) -> Unit,
    onReplaceRewrite: () -> Unit,
    onDiscardRewrite: () -> Unit,
    askInput: String,
    onAskInputChange: (String) -> Unit,
    onAskSubmit: () -> Unit,
    askHistory: List<AskEntry>,
    asking: Boolean,
    askCurrentQuestion: String,
    askAnswerStream: String
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val title = when (mode) {
        AiPanelMode.NO_KEY -> "AI features"
        AiPanelMode.SUMMARIZE -> "Summarize"
        AiPanelMode.REWRITE -> "Rewrite"
        AiPanelMode.ASK -> "Ask a question"
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)

            when (mode) {
                AiPanelMode.NO_KEY -> NoKeyContent(onGoToSettings)
                AiPanelMode.SUMMARIZE -> SummarizeContent(summarizing, summaryText)
                AiPanelMode.REWRITE -> RewriteContent(
                    rewriteTone, rewriteText, rewriting, rewriteDone,
                    onSelectTone, onReplaceRewrite, onDiscardRewrite
                )
                AiPanelMode.ASK -> AskContent(
                    askInput, onAskInputChange, onAskSubmit,
                    askHistory, asking, askCurrentQuestion, askAnswerStream
                )
            }
        }
    }
}

@Composable
private fun NoKeyContent(onGoToSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = "Add your Gemini API key in Settings to unlock AI features.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
        )
        Button(onClick = onGoToSettings) { Text("Go to Settings") }
    }
}

@Composable
private fun SummarizeContent(summarizing: Boolean, summaryText: String) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)) {
        if (summarizing && summaryText.isEmpty()) {
            Text("Thinking…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (summaryText.isNotEmpty()) {
            Text(summaryText, style = MaterialTheme.typography.bodyLarge)
            if (!summarizing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 14.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Cached — shown instantly next time you open this note",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RewriteContent(
    rewriteTone: RewriteTone?,
    rewriteText: String,
    rewriting: Boolean,
    rewriteDone: Boolean,
    onSelectTone: (RewriteTone) -> Unit,
    onReplaceRewrite: () -> Unit,
    onDiscardRewrite: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RewriteTone.entries.forEach { tone ->
                FilterChip(
                    selected = tone == rewriteTone,
                    onClick = { onSelectTone(tone) },
                    label = { Text(tone.label) }
                )
            }
        }
        if (rewriting && rewriteText.isEmpty()) {
            Text(
                "Rewriting…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        if (rewriteText.isNotEmpty()) {
            Text(
                text = "PREVIEW",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text(rewriteText, style = MaterialTheme.typography.bodyLarge)
        }
        if (rewriteDone) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
            ) {
                Button(onClick = onReplaceRewrite, modifier = Modifier.weight(1f)) {
                    Text("Replace note")
                }
                OutlinedButton(onClick = onDiscardRewrite, modifier = Modifier.weight(1f)) {
                    Text("Discard")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AskContent(
    askInput: String,
    onAskInputChange: (String) -> Unit,
    onAskSubmit: () -> Unit,
    askHistory: List<AskEntry>,
    asking: Boolean,
    askCurrentQuestion: String,
    askAnswerStream: String
) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(askHistory) { entry ->
                Column {
                    Text(entry.question, style = MaterialTheme.typography.titleSmall)
                    Text(
                        entry.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            if (asking) {
                item {
                    Column {
                        Text(askCurrentQuestion, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = askAnswerStream.ifEmpty { "Thinking…" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            OutlinedTextField(
                value = askInput,
                onValueChange = onAskInputChange,
                placeholder = { Text("Ask anything about this note") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onAskSubmit) {
                Box(
                    modifier = Modifier
                        .size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (asking) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Ask")
                    }
                }
            }
        }
    }
}
