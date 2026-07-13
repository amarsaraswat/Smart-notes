package com.smartnotes.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnotes.app.domain.model.ThemeMode
import com.smartnotes.app.domain.model.UserPreferences
import com.smartnotes.app.domain.repository.UserPreferencesRepository
import com.smartnotes.app.domain.usecase.ClearAiCacheUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val clearAiCacheUseCase: ClearAiCacheUseCase
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = userPreferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    fun onApiKeyChange(value: String) {
        viewModelScope.launch { userPreferencesRepository.setApiKey(value) }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { userPreferencesRepository.setThemeMode(mode) }
    }

    fun onToggleReminder() {
        viewModelScope.launch {
            userPreferencesRepository.setReminderEnabled(!preferences.value.reminderEnabled)
        }
    }

    fun onReminderTimeChange(time: String) {
        viewModelScope.launch { userPreferencesRepository.setReminderTime(time) }
    }

    fun onClearCache() {
        viewModelScope.launch { clearAiCacheUseCase() }
    }
}
