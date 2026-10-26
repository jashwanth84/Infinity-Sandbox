package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.PipelineMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "infinity_settings")

class SettingsDataStore(private val context: Context) {

    private val KEY_PIPELINE_MODE = stringPreferencesKey("pipeline_mode")
    private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")

    val pipelineModeFlow: Flow<PipelineMode> = context.dataStore.data.map { prefs ->
        val modeStr = prefs[KEY_PIPELINE_MODE] ?: PipelineMode.AUTO.name
        runCatching { PipelineMode.valueOf(modeStr) }.getOrDefault(PipelineMode.AUTO)
    }

    val isDarkThemeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_THEME] ?: true // Dark theme default as requested
    }

    suspend fun setPipelineMode(mode: PipelineMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PIPELINE_MODE] = mode.name
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DARK_THEME] = isDark
        }
    }
}
