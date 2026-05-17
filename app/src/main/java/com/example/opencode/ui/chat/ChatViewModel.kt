package com.example.opencode.ui.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.opencode.data.model.Message
import com.example.opencode.data.model.MessageInfo
import com.example.opencode.data.model.Part
import com.example.opencode.data.remote.OpenCodeApi
import com.example.opencode.data.repository.ConnectionRepository
import com.example.opencode.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val isSending: Boolean = false,
    val error: String? = null,
    val inputText: String = "",
    val sessionTitle: String? = null,
    val todos: List<com.example.opencode.data.model.Todo> = emptyList(),
    val sessionInfo: com.example.opencode.data.model.Session? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val connectionRepository: ConnectionRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val sessionId: String = savedStateHandle["sessionId"] ?: ""
    private val pageSize = 50
    private var nextCursor: String? = null

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var pollJob: kotlinx.coroutines.Job? = null
    private val sendLock = AtomicBoolean(false)

    init {
        Log.d("ChatVM", "Init with sessionId=$sessionId")
        if (sessionId.isNotBlank()) {
            loadInitial()
            startPolling()
            loadSessionInfo()
        }
    }

    private fun loadSessionInfo() {
        val api = connectionRepository.getApi() ?: return
        viewModelScope.launch {
            try {
                val session = sessionRepository.getSession(api, sessionId)
                _uiState.value = _uiState.value.copy(sessionTitle = session.title.ifBlank { null })
            } catch (_: Exception) {}
        }
    }

    private fun startPolling() {
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(3000)
                refreshLatest()
            }
        }
    }

    private fun loadInitial() {
        val api = connectionRepository.getApi() ?: return
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                Log.d("ChatVM", "Loading messages for session=$sessionId")
                val result = api.listMessagesPaged(sessionId, limit = pageSize)
                Log.d("ChatVM", "Loaded ${result.messages.size} messages, cursor=${result.nextCursor}")
                nextCursor = result.nextCursor
                _uiState.value = _uiState.value.copy(
                    messages = result.messages,
                    isLoading = false,
                    hasMore = result.nextCursor != null,
                )
            } catch (e: Exception) {
                Log.e("ChatVM", "loadInitial failed", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private suspend fun refreshLatest() {
        if (_uiState.value.isSending) return
        val api = connectionRepository.getApi() ?: return
        try {
            val serverMessages = api.listMessages(sessionId, limit = 100)
            if (serverMessages.isNotEmpty()) {
                val current = _uiState.value.messages
                val serverIds = serverMessages.mapNotNull { it.info?.id }.filter { it.isNotBlank() }.toSet()
                val localOnly = current.filter { it.info?.id.isNullOrBlank() }
                val localOnServer = localOnly.filter { local ->
                    serverMessages.any { server ->
                        server.info?.role == local.info?.role &&
                        server.parts.any { sp -> sp.type == "text" && local.parts.any { lp -> lp.type == "text" && lp.text == sp.text } }
                    }
                }
                val localKept = localOnly - localOnServer.toSet()
                val merged = serverMessages + localKept
                Log.d("ChatVM", "refreshLatest: server=${serverMessages.size}, keptLocal=${localKept.size}")
                _uiState.value = _uiState.value.copy(messages = merged)
            }
        } catch (e: Exception) {
            Log.e("ChatVM", "refreshLatest failed", e)
        }
    }

    fun loadMore() {
        val api = connectionRepository.getApi() ?: return
        val cursor = nextCursor
        if (_uiState.value.isLoadingMore || cursor == null || !_uiState.value.hasMore) return

        _uiState.value = _uiState.value.copy(isLoadingMore = true)

        viewModelScope.launch {
            try {
                val result = api.listMessagesPaged(sessionId, limit = pageSize, before = cursor)
                nextCursor = result.nextCursor
                _uiState.value = _uiState.value.copy(
                    messages = result.messages + _uiState.value.messages,
                    isLoadingMore = false,
                    hasMore = result.nextCursor != null,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingMore = false, error = e.message)
            }
        }
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage() {
        if (!sendLock.compareAndSet(false, true)) return
        val api = connectionRepository.getApi()
        if (api == null) {
            _uiState.value = _uiState.value.copy(error = "Not connected")
            sendLock.set(false)
            return
        }
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) { sendLock.set(false); return }

        val localMsg = Message(
            info = MessageInfo(role = "user"),
            parts = listOf(Part(type = "text", text = text)),
        )
        _uiState.value = _uiState.value.copy(
            inputText = "",
            isSending = true,
            error = null,
            messages = _uiState.value.messages + localMsg,
        )

        viewModelScope.launch {
            try {
                sessionRepository.sendMessage(api, sessionId, text)
                Log.d("ChatVM", "Send success for: ${text.take(30)}")
                val serverOnly = _uiState.value.messages.filter { !it.info?.id.isNullOrBlank() }
                _uiState.value = _uiState.value.copy(messages = serverOnly)
                delay(1000)
                refreshLatest()
            } catch (e: Exception) {
                Log.e("ChatVM", "Send failed", e)
                _uiState.value = _uiState.value.copy(
                    error = "Send failed: ${e.message}",
                    messages = _uiState.value.messages.filterNot {
                        it.info?.id.isNullOrBlank() &&
                        it.parts.any { p -> p.text == text }
                    },
                )
            } finally {
                _uiState.value = _uiState.value.copy(isSending = false)
                sendLock.set(false)
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    fun loadTodos() {
        val api = connectionRepository.getApi() ?: return
        viewModelScope.launch {
            try {
                val todos = sessionRepository.getSessionTodos(api, sessionId)
                _uiState.value = _uiState.value.copy(todos = todos)
            } catch (_: Exception) {}
        }
    }

    fun loadSessionDetails() {
        val api = connectionRepository.getApi() ?: return
        viewModelScope.launch {
            try {
                val session = sessionRepository.getSession(api, sessionId)
                _uiState.value = _uiState.value.copy(sessionInfo = session)
            } catch (_: Exception) {}
        }
    }

    fun shareSession() {
        val api = connectionRepository.getApi() ?: return
        viewModelScope.launch {
            try {
                val session = sessionRepository.shareSession(api, sessionId)
                _uiState.value = _uiState.value.copy(sessionInfo = session)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Share failed: ${e.message}")
            }
        }
    }

    override fun onCleared() { super.onCleared(); pollJob?.cancel() }
}
