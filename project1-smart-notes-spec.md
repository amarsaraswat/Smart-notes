# Project 1: SmartNotes AI — Complete Build Spec

> **How to use this file:** Drop this into a new repo as `CLAUDE.md`, then open Claude Code in that directory and say "read CLAUDE.md and scaffold Phase 1." Work through phases in order — don't jump ahead. Each phase should compile and be committed before moving to the next.

---

## 1. Positioning

An offline-first note-taking app where the AI layer (Gemini API) summarizes, rewrites, and answers questions about a user's notes, with streaming (word-by-word) responses rendered live in Compose.

**Resume framing:** cloud LLM integration, streaming UI, offline-first architecture, Hilt DI, background work — the "hot skill" project.

---

## 2. Tech Stack

| Layer | Technology | Notes |
|---|---|---|
| UI | Jetpack Compose, Material 3 | Single-module is fine for this project (contrast with Project 2) |
| DI | Hilt | `@HiltAndroidApp`, `@AndroidEntryPoint`, module-scoped bindings |
| Local storage | Room | Notes table + FTS for search |
| Preferences | DataStore (Preferences) | API key storage (encrypted — see Security section), theme, reminder time |
| AI | Gemini API (`generativelanguage.googleapis.com`) via Ktor client or Google's official `generativeai` Kotlin SDK | Use streaming endpoint |
| Async | Kotlin Coroutines + Flow | `Flow<String>` for token-by-token streaming |
| Background work | WorkManager | Daily reminder notification |
| Testing | JUnit5, MockK, Turbine, Compose UI Testing | |

**Gemini API access:** requires a free Google AI Studio API key (https://aistudio.google.com/apikey). Free tier has rate limits (RPM/TPM/RPD caps that vary by model) — sufficient for a personal/demo project, not for production traffic. No credit card required for the free tier as of this writing, but **verify current limits directly in Google AI Studio before building**, since free-tier quotas change.

---

## 3. Architecture (single-module, layered internally)

```
app/
├── di/
│   ├── AppModule.kt              → Room DB, DataStore providers
│   ├── NetworkModule.kt          → Ktor client / Gemini SDK client provider
│   └── RepositoryModule.kt       → Bind repository interfaces to impls
├── data/
│   ├── local/
│   │   ├── NoteDao.kt
│   │   ├── NoteEntity.kt
│   │   ├── AppDatabase.kt
│   │   └── Converters.kt
│   ├── remote/
│   │   ├── GeminiApiClient.kt    → wraps SDK/Ktor calls, exposes Flow<String> for streaming
│   │   └── dto/                  → request/response models if using raw Ktor
│   ├── preferences/
│   │   └── UserPreferencesRepository.kt   → DataStore wrapper (theme, reminder time, API key)
│   └── repository/
│       ├── NoteRepositoryImpl.kt
│       └── AiRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   ├── Note.kt
│   │   └── AiAction.kt           → sealed class: Summarize, Rewrite(tone), AskQuestion(query)
│   ├── repository/
│   │   ├── NoteRepository.kt     → interface
│   │   └── AiRepository.kt       → interface
│   └── usecase/
│       ├── GetNotesUseCase.kt
│       ├── SaveNoteUseCase.kt
│       ├── DeleteNoteUseCase.kt
│       ├── SearchNotesUseCase.kt
│       └── ProcessNoteWithAiUseCase.kt   → returns Flow<AiStreamState>
├── ui/
│   ├── theme/
│   ├── notelist/
│   │   ├── NoteListScreen.kt
│   │   ├── NoteListViewModel.kt
│   │   └── NoteListUiState.kt
│   ├── noteeditor/
│   │   ├── NoteEditorScreen.kt
│   │   ├── NoteEditorViewModel.kt
│   │   └── AiPanel.kt            → the summarize/rewrite/ask UI + streaming text render
│   └── settings/
│       ├── SettingsScreen.kt     → API key entry, reminder time picker
│       └── SettingsViewModel.kt
├── worker/
│   └── DailyReminderWorker.kt
└── MainActivity.kt / SmartNotesApp.kt
```

**Why layered-but-single-module here (vs. Project 2's multi-module):** the point of this project is the AI/streaming integration, not architecture depth — that's Project 2's job. Keep this one focused so you finish it. If you want to demonstrate module discipline in *both* projects, you can later split `domain`/`data`/`ui` into Gradle modules as a stretch goal, but don't let that block shipping.

---

## 4. Data Model

```kotlin
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val tags: String,              // comma-separated, or use a separate join table if you want relational depth
    val createdAt: Long,
    val updatedAt: Long,
    val lastAiSummary: String? = null
)

// FTS shadow table for search
@Fts4(contentEntity = NoteEntity::class)
@Entity(tableName = "notes_fts")
data class NoteFts(
    val title: String,
    val content: String
)
```

```kotlin
sealed class AiAction {
    data object Summarize : AiAction()
    data class Rewrite(val tone: RewriteTone) : AiAction()
    data class AskQuestion(val question: String) : AiAction()
}

enum class RewriteTone { FORMAL, CASUAL, CONCISE, EXPANDED }

sealed class AiStreamState {
    data object Idle : AiStreamState()
    data object Loading : AiStreamState()
    data class Streaming(val partialText: String) : AiStreamState()
    data class Done(val fullText: String) : AiStreamState()
    data class Error(val message: String) : AiStreamState()
}
```

---

## 5. Streaming Implementation (the core technical showpiece)

Use Gemini's streaming generate-content endpoint. Expose it as a cold `Flow<String>` that emits accumulated text as chunks arrive, so the UI can render a typing effect.

```kotlin
// AiRepositoryImpl.kt
class AiRepositoryImpl @Inject constructor(
    private val geminiClient: GeminiApiClient
) : AiRepository {

    override fun streamAction(note: Note, action: AiAction): Flow<AiStreamState> = flow {
        emit(AiStreamState.Loading)
        val prompt = buildPrompt(note, action)
        var accumulated = ""
        try {
            geminiClient.streamGenerateContent(prompt).collect { chunk ->
                accumulated += chunk
                emit(AiStreamState.Streaming(accumulated))
            }
            emit(AiStreamState.Done(accumulated))
        } catch (e: Exception) {
            emit(AiStreamState.Error(e.message ?: "AI request failed"))
        }
    }.flowOn(Dispatchers.IO)

    private fun buildPrompt(note: Note, action: AiAction): String = when (action) {
        is AiAction.Summarize -> "Summarize the following note in 2-3 sentences:\n\n${note.content}"
        is AiAction.Rewrite -> "Rewrite the following note in a ${action.tone.name.lowercase()} tone:\n\n${note.content}"
        is AiAction.AskQuestion -> "Given this note:\n\n${note.content}\n\nAnswer this question: ${action.question}"
    }
}
```

```kotlin
// ViewModel side — collect into UI state
fun runAiAction(action: AiAction) {
    viewModelScope.launch {
        processNoteWithAiUseCase(currentNote, action).collect { state ->
            _uiState.update { it.copy(aiState = state) }
        }
    }
}
```

```kotlin
// Compose UI — render streaming text, this IS the "ChatGPT typing effect" your resume bullet references
@Composable
fun AiResponsePanel(state: AiStreamState) {
    when (state) {
        is AiStreamState.Streaming -> Text(state.partialText) // recomposes on every emission — the effect is free
        is AiStreamState.Done -> Text(state.fullText)
        is AiStreamState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
        AiStreamState.Loading -> CircularProgressIndicator()
        AiStreamState.Idle -> {}
    }
}
```

**If using the official Google Generative AI Kotlin SDK** (`com.google.ai.client.generativeai`), it exposes `generateContentStream()` returning a `Flow<GenerateContentResponse>` natively — prefer this over raw Ktor unless you specifically want the Ktor networking practice. Check current SDK docs before implementation, as the API surface has evolved.

---

## 6. Security: API Key Handling

Don't hardcode the Gemini API key or commit it. For a personal project:
- Store it in `local.properties` (gitignored) and inject via `BuildConfig` at build time, **or**
- Let the user paste their own key into a Settings screen, persisted via encrypted DataStore (`androidx.security.crypto` `EncryptedSharedPreferences` equivalent, or at minimum don't log it)

Mention in your README which approach you used and why — this is a small but real signal of security awareness.

---

## 7. Features Checklist

- [ ] Create / edit / delete notes (Compose UI, Room-backed)
- [ ] FTS-based local search across title + content
- [ ] Tagging (simple comma-separated or normalized join table — pick join table if you want to show relational modeling)
- [ ] AI panel per note: Summarize / Rewrite (tone picker) / Ask a question
- [ ] Streamed AI response rendering (typing effect)
- [ ] Cache last AI summary per note (`lastAiSummary` field) so it's visible without re-calling the API
- [ ] Daily reminder notification via WorkManager (`PeriodicWorkRequest`, min 15-min interval per WorkManager constraints — for "daily" use `setInitialDelay` calculated to the next target time, then re-schedule each run)
- [ ] Settings screen: API key entry, reminder time, theme toggle
- [ ] Error/empty states: no API key set, network failure, empty note list

---

## 8. WorkManager Daily Reminder (implementation note)

`PeriodicWorkRequest` has a **15-minute minimum interval** but no native "run at 8pm daily" scheduling. Correct pattern:

```kotlin
fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
    }
    val initialDelay = target.timeInMillis - now.timeInMillis

    val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "daily_reminder", ExistingPeriodicWorkPolicy.UPDATE, request
    )
}
```

Talking point: explain *why* you didn't just use `AlarmManager` — WorkManager is preferred for deferrable, guaranteed background work that survives reboots, while `AlarmManager` (`setExactAndAllowWhileIdle`) is for precise-time alarms. For a reminder notification, WorkManager's daily periodic pattern is the right call; know the tradeoff so you can explain it if asked.

---

## 9. Build Plan (4-5 weekends)

| Phase | Focus |
|---|---|
| 1 | Project scaffold, Hilt setup, Room DB + DAO + FTS, note list + editor CRUD UI (no AI yet) |
| 2 | Gemini API integration: non-streaming first (get a single request/response working end-to-end) |
| 3 | Convert to streaming: `Flow<String>` from SDK/Ktor → ViewModel → Compose typing effect |
| 4 | Tags, search, Settings screen (API key entry), error/empty states |
| 5 | WorkManager daily reminder, polish, tests, README with architecture diagram + demo GIF |

---

## 10. Testing Plan

- **Use cases**: unit test with fake `NoteRepository`/`AiRepository`
- **AiRepositoryImpl streaming**: test with Turbine, mocking the Gemini client to emit a canned sequence of chunks, assert the `AiStreamState` sequence (`Loading → Streaming(...) → Done`)
- **Room DAO**: Robolectric in-memory DB tests for CRUD + FTS search queries
- **Compose UI**: test the AI panel renders correctly for each `AiStreamState`, and that tapping "Summarize" triggers the expected ViewModel call

---

## 11. Resume Bullets

- "Integrated Google Gemini API with streaming response rendering in Jetpack Compose, enabling real-time AI summarization, rewriting, and Q&A on user notes"
- "Implemented offline-first architecture with Room database, FTS-based local search, and Hilt dependency injection"
- "Built a WorkManager-based daily reminder system with precise time-of-day scheduling"
- "Designed a sealed-class streaming state machine (`AiStreamState`) to cleanly model loading, partial, complete, and error states for LLM responses in Compose"

---

## 12. Claude Code Prompting Tips for This Project

- Paste this whole file as `CLAUDE.md` in repo root.
- Work phase by phase: `"Implement Phase 1 only. Don't touch AI/network code yet."`
- After each phase: `"Run the build and fix any compile errors before we continue."`
- For the streaming piece specifically, be explicit: `"Implement AiRepositoryImpl exactly per section 5 of CLAUDE.md — I want a real Flow<String>, not a fake delay-based simulation."` (Models sometimes default to a `delay()`-based fake streaming effect instead of real API streaming — check the diff.)
- Ask Claude Code to write tests *alongside* each phase, not as a final bolt-on step — this matches real practice and keeps coverage honest.
