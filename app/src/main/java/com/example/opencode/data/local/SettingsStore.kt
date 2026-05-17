package com.example.opencode.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.opencode.data.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_SETTINGS = stringPreferencesKey("settings_json")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val settings: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_SETTINGS]
        if (raw.isNullOrBlank()) UserSettings()
        else try { json.decodeFromString<UserSettings>(raw) } catch (_: Exception) { UserSettings() }
    }

    suspend fun save(settings: UserSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_SETTINGS] = json.encodeToString(settings)
        }
    }
}
