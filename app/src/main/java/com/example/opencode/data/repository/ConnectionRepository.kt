package com.example.opencode.data.repository

import com.example.opencode.data.local.ConnectionConfigStore
import com.example.opencode.data.model.ConnectionConfig
import com.example.opencode.data.remote.HttpClientFactory
import com.example.opencode.data.remote.OpenCodeApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepository @Inject constructor(
    private val configStore: ConnectionConfigStore,
    private val httpClientFactory: HttpClientFactory,
) {
    private val _api = MutableStateFlow<OpenCodeApi?>(null)
    val api: StateFlow<OpenCodeApi?> = _api.asStateFlow()

    private val _activeConfig = MutableStateFlow<ConnectionConfig?>(null)
    val activeConfig: StateFlow<ConnectionConfig?> = _activeConfig.asStateFlow()

    val servers: Flow<List<ConnectionConfig>> = configStore.servers
    val activeServerId: Flow<String?> = configStore.activeServerId

    suspend fun connect(config: ConnectionConfig): OpenCodeApi {
        val client = httpClientFactory.create(config)
        val api = OpenCodeApi(client)
        _api.value = api
        _activeConfig.value = config
        configStore.setActiveServer(config.id)
        return api
    }

    fun disconnect() {
        _api.value = null
        _activeConfig.value = null
    }

    fun getApi(): OpenCodeApi? = _api.value

    suspend fun addServer(config: ConnectionConfig) = configStore.addServer(config)
    suspend fun removeServer(id: String) = configStore.removeServer(id)
    suspend fun updateServer(config: ConnectionConfig) = configStore.updateServer(config)
    suspend fun saveServers(servers: List<ConnectionConfig>) = configStore.saveServers(servers)
}
