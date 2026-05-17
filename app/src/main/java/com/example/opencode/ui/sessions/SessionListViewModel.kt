package com.example.opencode.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.opencode.data.model.Session
import com.example.opencode.data.repository.ConnectionRepository
import com.example.opencode.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionListUiState(
    val sessions: List<Session> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCreating: Boolean = false,
)

@HiltViewModel
class SessionListViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionListUiState())
    val uiState: StateFlow<SessionListUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    fun loadSessions() {
        val api = connectionRepository.getApi() ?: return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val sessions = sessionRepository.listSessions(api)
                _uiState.value = _uiState.value.copy(
                    sessions = sessions.sortedByDescending { it.time.updated },
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load sessions",
                )
            }
        }
    }

    fun createSession(title: String? = null) {
        val api = connectionRepository.getApi() ?: return
        _uiState.value = _uiState.value.copy(isCreating = true)
        viewModelScope.launch {
            try {
                sessionRepository.createSession(api, title)
                loadSessions()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    error = e.message ?: "Failed to create session",
                )
            }
        }
    }

    fun deleteSession(id: String) {
        val api = connectionRepository.getApi() ?: return
        viewModelScope.launch {
            try {
                sessionRepository.deleteSession(api, id)
                loadSessions()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete session",
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
