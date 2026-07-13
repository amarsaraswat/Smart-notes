package com.smartnotes.app.ui.noteeditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val MAX_TAG_SUGGESTIONS = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    onBack: () -> Unit,
    onGoToSettings: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val exit = { viewModel.flushAndExit(onBack) }

    BackHandler(onBack = exit)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = exit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.deleteNote(onBack) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete note")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            BorderlessField(
                value = viewModel.title,
                onValueChange = viewModel::onTitleChange,
                placeholder = "Title",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )

            TagSection(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 10.dp)
            )

            if (viewModel.cachedSummary != null) {
                CachedSummaryCard(
                    summary = viewModel.cachedSummary.orEmpty(),
                    onClick = { viewModel.openAiPanel(AiPanelMode.SUMMARIZE) }
                )
            }

            BorderlessField(
                value = viewModel.content,
                onValueChange = viewModel::onContentChange,
                placeholder = "Start typing...",
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp)
            ) {
                AiActionButton("Summarize", Modifier.weight(1f)) { viewModel.openAiPanel(AiPanelMode.SUMMARIZE) }
                AiActionButton("Rewrite", Modifier.weight(1f)) { viewModel.openAiPanel(AiPanelMode.REWRITE) }
                AiActionButton("Ask", Modifier.weight(1f)) { viewModel.openAiPanel(AiPanelMode.ASK) }
            }
        }
    }

    val mode = viewModel.aiMode
    if (viewModel.aiPanelOpen && mode != null) {
        AiPanel(
            mode = mode,
            onDismiss = viewModel::closeAiPanel,
            onGoToSettings = { viewModel.closeAiPanel(); onGoToSettings() },
            summarizing = viewModel.summarizing,
            summaryText = viewModel.summaryText,
            rewriteTone = viewModel.rewriteTone,
            rewriteText = viewModel.rewriteText,
            rewriting = viewModel.rewriting,
            rewriteDone = viewModel.rewriteDone,
            onSelectTone = viewModel::selectTone,
            onReplaceRewrite = viewModel::replaceRewrite,
            onDiscardRewrite = viewModel::discardRewrite,
            askInput = viewModel.askInput,
            onAskInputChange = viewModel::onAskInputChange,
            onAskSubmit = viewModel::askSubmit,
            askHistory = viewModel.askHistory,
            asking = viewModel.asking,
            askCurrentQuestion = viewModel.askCurrentQuestion,
            askAnswerStream = viewModel.askAnswerStream
        )
    }
}

@Composable
private fun AiActionButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun CachedSummaryCard(summary: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "AI SUMMARY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BorderlessField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = fontSize) },
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = fontSize, fontWeight = fontWeight),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TagSection(viewModel: NoteEditorViewModel, modifier: Modifier = Modifier) {
    val allTagNames by viewModel.allTagNames.collectAsStateWithLifecycle()
    val tags = viewModel.tags
    val tagInput = viewModel.tagInput
    val suggestions = remember(tagInput, allTagNames, tags) {
        if (tagInput.isBlank()) {
            emptyList()
        } else {
            allTagNames.filter { candidate ->
                candidate.startsWith(tagInput, ignoreCase = true) &&
                    tags.none { it.equals(candidate, ignoreCase = true) }
            }.take(MAX_TAG_SUGGESTIONS)
        }
    }

    Column(modifier = modifier) {
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { viewModel.removeTag(tag) },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove tag $tag",
                                modifier = Modifier.size(InputChipDefaults.IconSize)
                            )
                        }
                    )
                }
            }
        }
        BorderlessField(
            value = tagInput,
            onValueChange = viewModel::onTagInputChange,
            placeholder = "+ tag",
            fontSize = 13.sp,
            modifier = Modifier.padding(top = if (tags.isNotEmpty()) 4.dp else 0.dp)
        )
        if (suggestions.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                items(suggestions) { suggestion ->
                    AssistChip(
                        onClick = { viewModel.commitTagInput(suggestion) },
                        label = { Text(suggestion) }
                    )
                }
            }
        }
    }
}
