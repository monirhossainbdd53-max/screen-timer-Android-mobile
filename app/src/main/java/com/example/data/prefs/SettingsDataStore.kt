package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "tracker_settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        const val DEFAULT_WARNING_SECONDS = 60
        const val MIN_WARNING_SECONDS = 30
        const val MAX_WARNING_SECONDS = 120
        private val KEY_WARNING_SECONDS = intPreferencesKey("warning_seconds_before_end")
    }

    val warningSecondsFlow: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        (prefs[KEY_WARNING_SECONDS] ?: DEFAULT_WARNING_SECONDS)
            .coerceIn(MIN_WARNING_SECONDS, MAX_WARNING_SECONDS)
    }

    suspend fun getWarningSeconds(): Int = warningSecondsFlow.first()

    suspend fun setWarningSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(MIN_WARNING_SECONDS, MAX_WARNING_SECONDS)
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_WARNING_SECONDS] = clamped
        }
    }
}
