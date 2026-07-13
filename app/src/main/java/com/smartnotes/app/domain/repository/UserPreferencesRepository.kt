package com.smartnotes.app.domain.repository

import com.smartnotes.app.domain.model.ThemeMode
import com.smartnotes.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setApiKey(apiKey: String)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setReminderEnabled(enabled: Boolean)
    suspend fun setReminderTime(time: String)
}
