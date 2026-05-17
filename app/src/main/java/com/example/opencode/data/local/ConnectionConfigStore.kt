package com.example.opencode.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.opencode.data.model.ConnectionConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "opencode_settings")

@Singleton
class ConnectionConfigStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_SERVERS = stringPreferencesKey("servers")
        private val KEY_ACTIVE_ID = stringPreferencesKey("active_server_id")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val servers: Flow<List<ConnectionConfig>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_SERVERS]
        if (raw.isNullOrBlank()) emptyList()
        else try { json.decodeFromString<List<ConnectionConfig>>(raw) } catch (_: Exception) { emptyList() }
    }

    val activeServerId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_ID]
    }

    suspend fun saveServers(servers: List<ConnectionConfig>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVERS] = json.encodeToString(servers)
        }
    }

    suspend fun setActiveServer(id: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_ID] = id
        }
    }

    suspend fun addServer(config: ConnectionConfig) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_SERVERS]
            val list = if (current.isNullOrBlank()) emptyList()
            else try { json.decodeFromString<List<ConnectionConfig>>(current) } catch (_: Exception) { emptyList() }
            prefs[KEY_SERVERS] = json.encodeToString(list + config)
        }
    }

    suspend fun removeServer(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_SERVERS]
            val list = if (current.isNullOrBlank()) emptyList()
            else try { json.decodeFromString<List<ConnectionConfig>>(current) } catch (_: Exception) { emptyList() }
            prefs[KEY_SERVERS] = json.encodeToString(list.filter { it.id != id })
            if (prefs[KEY_ACTIVE_ID] == id) prefs.remove(KEY_ACTIVE_ID)
        }
    }

    suspend fun updateServer(config: ConnectionConfig) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_SERVERS]
            val list = if (current.isNullOrBlank()) emptyList()
            else try { json.decodeFromString<List<ConnectionConfig>>(current) } catch (_: Exception) { emptyList() }
            prefs[KEY_SERVERS] = json.encodeToString(list.map { if (it.id == config.id) config else it })
        }
    }
}
