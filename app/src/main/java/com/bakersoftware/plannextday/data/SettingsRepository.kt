package com.bakersoftware.plannextday.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "plan_next_day_settings")

enum class ThemeChoice { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    val reminderMinutes: Int = 20 * 60,
    val remindersEnabled: Boolean = false,
    val nextWeekMissLimit: Int = 3,
    val lastReminderDate: String = "",
    val hasPlannedBefore: Boolean = false
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val reminderMinutes = intPreferencesKey("reminder_minutes")
        val remindersEnabled = booleanPreferencesKey("reminders_enabled")
        val nextWeekMissLimit = intPreferencesKey("next_week_miss_limit")
        val lastReminderDate = stringPreferencesKey("last_reminder_date")
        val hasPlannedBefore = booleanPreferencesKey("has_planned_before")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            theme = preferences[Keys.theme]?.let {
                runCatching { ThemeChoice.valueOf(it) }.getOrDefault(ThemeChoice.SYSTEM)
            } ?: ThemeChoice.SYSTEM,
            reminderMinutes = preferences[Keys.reminderMinutes] ?: 20 * 60,
            remindersEnabled = preferences[Keys.remindersEnabled] ?: false,
            nextWeekMissLimit = preferences[Keys.nextWeekMissLimit] ?: 3,
            lastReminderDate = preferences[Keys.lastReminderDate] ?: "",
            hasPlannedBefore = preferences[Keys.hasPlannedBefore] ?: false
        )
    }

    suspend fun setTheme(theme: ThemeChoice) {
        context.dataStore.edit { it[Keys.theme] = theme.name }
    }

    suspend fun setReminderMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.reminderMinutes] = minutes }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.remindersEnabled] = enabled }
    }

    suspend fun setNextWeekMissLimit(limit: Int) {
        context.dataStore.edit { it[Keys.nextWeekMissLimit] = limit.coerceIn(0, 99) }
    }

    suspend fun markReminderSent(date: String) {
        context.dataStore.edit { it[Keys.lastReminderDate] = date }
    }

    suspend fun markPlannedBefore() {
        context.dataStore.edit { it[Keys.hasPlannedBefore] = true }
    }
}
