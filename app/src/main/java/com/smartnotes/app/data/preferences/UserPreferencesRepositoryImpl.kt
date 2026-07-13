package com.smartnotes.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.smartnotes.app.domain.model.ThemeMode
import com.smartnotes.app.domain.model.UserPreferences
import com.smartnotes.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private object Keys {
    val API_KEY = stringPreferencesKey("api_key")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
    val REMINDER_TIME = stringPreferencesKey("reminder_time")
}

class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            apiKey = prefs[Keys.API_KEY] ?: "",
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: false,
            reminderTime = prefs[Keys.REMINDER_TIME] ?: "9:00 PM"
        )
    }

    override suspend fun setApiKey(apiKey: String) {
        dataStore.edit { it[Keys.API_KEY] = apiKey }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }
    }

    override suspend fun setReminderTime(time: String) {
        dataStore.edit { it[Keys.REMINDER_TIME] = time }
    }
}
