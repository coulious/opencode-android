package com.example.opencode.ui.stats

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

data class StatsUiState(
    val serverName: String? = null,
    val sessions: List<Session> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val api = connectionRepository.getApi()
        val config = connectionRepository.activeConfig.value
        _uiState.value = _uiState.value.copy(serverName = config?.displayName)

        if (api == null) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }

        viewModelScope.launch {
            try {
                val sessions = sessionRepository.listSessions(api)
                _uiState.value = _uiState.value.copy(
                    sessions = sessions.sortedByDescending { it.time.updated },
                    isLoading = false,
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
