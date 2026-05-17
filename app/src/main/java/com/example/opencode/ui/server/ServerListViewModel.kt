package com.example.opencode.ui.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.opencode.data.model.ConnectionConfig
import com.example.opencode.data.repository.ConnectionRepository
import com.example.opencode.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerListUiState(
    val isConnecting: Boolean = false,
    val connectingId: String? = null,
    val connected: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val editingConfig: ConnectionConfig? = null,
)

@HiltViewModel
class ServerListViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val servers: StateFlow<List<ConnectionConfig>> = connectionRepository.servers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeServerId: StateFlow<String?> = connectionRepository.activeServerId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(ServerListUiState())
    val uiState: StateFlow<ServerListUiState> = _uiState.asStateFlow()

    fun connect(config: ConnectionConfig) {
        _uiState.value = _uiState.value.copy(isConnecting = true, connectingId = config.id, error = null)
        viewModelScope.launch {
            try {
                val api = connectionRepository.connect(config)
                sessionRepository.health(api)
                _uiState.value = _uiState.value.copy(isConnecting = false, connectingId = null, connected = true)
            } catch (e: Exception) {
                connectionRepository.disconnect()
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    connectingId = null,
                    error = "Cannot connect to ${config.displayName}: ${e.message}",
                )
            }
        }
    }

    fun addServer(config: ConnectionConfig) {
        viewModelScope.launch {
            connectionRepository.addServer(config)
            _uiState.value = _uiState.value.copy(showAddDialog = false)
        }
    }

    fun removeServer(id: String) {
        viewModelScope.launch {
            connectionRepository.removeServer(id)
        }
    }

    fun updateServer(config: ConnectionConfig) {
        viewModelScope.launch {
            connectionRepository.updateServer(config)
            _uiState.value = _uiState.value.copy(editingConfig = null)
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true, editingConfig = null)
    }

    fun showEditDialog(config: ConnectionConfig) {
        _uiState.value = _uiState.value.copy(editingConfig = config, showAddDialog = true)
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false, editingConfig = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
