package com.smartnotes.app.domain.model

data class UserPreferences(
    val apiKey: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "9:00 PM"
)
