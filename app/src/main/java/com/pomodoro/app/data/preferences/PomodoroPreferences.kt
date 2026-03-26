package com.pomodoro.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.pomodoro.app.data.model.PomodoroConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pomodoro_settings")

/**
 * 用户配置持久化管理（DataStore）
 */
class PomodoroPreferences(private val context: Context) {

    companion object {
        val FOCUS_DURATION = intPreferencesKey("focus_duration")
        val SHORT_BREAK = intPreferencesKey("short_break")
        val LONG_BREAK = intPreferencesKey("long_break")
        val LONG_BREAK_INTERVAL = intPreferencesKey("long_break_interval")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    }

    val configFlow: Flow<PomodoroConfig> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { prefs ->
            PomodoroConfig(
                focusDurationMinutes = prefs[FOCUS_DURATION] ?: 25,
                shortBreakMinutes = prefs[SHORT_BREAK] ?: 5,
                longBreakMinutes = prefs[LONG_BREAK] ?: 15,
                longBreakInterval = prefs[LONG_BREAK_INTERVAL] ?: 4,
                soundEnabled = prefs[SOUND_ENABLED] ?: true,
                vibrationEnabled = prefs[VIBRATION_ENABLED] ?: true
            )
        }

    suspend fun saveConfig(config: PomodoroConfig) {
        context.dataStore.edit { prefs ->
            prefs[FOCUS_DURATION] = config.focusDurationMinutes
            prefs[SHORT_BREAK] = config.shortBreakMinutes
            prefs[LONG_BREAK] = config.longBreakMinutes
            prefs[LONG_BREAK_INTERVAL] = config.longBreakInterval
            prefs[SOUND_ENABLED] = config.soundEnabled
            prefs[VIBRATION_ENABLED] = config.vibrationEnabled
        }
    }
}
