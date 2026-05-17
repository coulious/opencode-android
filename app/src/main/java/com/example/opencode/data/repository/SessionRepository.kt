package com.example.opencode.data.repository

import com.example.opencode.data.model.Command
import com.example.opencode.data.model.CommandRequest
import com.example.opencode.data.model.HealthResponse
import com.example.opencode.data.model.Message
import com.example.opencode.data.model.PromptPart
import com.example.opencode.data.model.PromptRequest
import com.example.opencode.data.model.Session
import com.example.opencode.data.model.Todo
import com.example.opencode.data.remote.OpenCodeApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor() {

    suspend fun health(api: OpenCodeApi): HealthResponse = api.health()

    suspend fun listSessions(api: OpenCodeApi): List<Session> = api.listSessions()

    suspend fun createSession(api: OpenCodeApi, title: String? = null): Session = api.createSession(title)

    suspend fun getSession(api: OpenCodeApi, id: String): Session = api.getSession(id)

    suspend fun deleteSession(api: OpenCodeApi, id: String): Boolean = api.deleteSession(id)

    suspend fun updateSession(api: OpenCodeApi, id: String, title: String): Session = api.updateSession(id, title)

    suspend fun listMessages(api: OpenCodeApi, sessionId: String, limit: Int? = null): List<Message> =
        api.listMessages(sessionId, limit)

    suspend fun sendMessage(api: OpenCodeApi, sessionId: String, text: String): Message {
        val request = PromptRequest(parts = listOf(PromptPart(text = text)))
        return try {
            val result = api.sendMessage(sessionId, request)
            android.util.Log.d("SessionRepo", "Sync send OK")
            result
        } catch (e: Exception) {
            android.util.Log.w("SessionRepo", "Sync failed: ${e.message}, trying async")
            api.sendMessageAsync(sessionId, request)
            android.util.Log.d("SessionRepo", "Async send OK")
            Message()
        }
    }

    suspend fun sendCommand(api: OpenCodeApi, sessionId: String, command: String, arguments: String = ""): Message =
        api.sendCommand(sessionId, CommandRequest(command = command, arguments = arguments))

    suspend fun abortSession(api: OpenCodeApi, sessionId: String): Boolean = api.abortSession(sessionId)

    suspend fun listCommands(api: OpenCodeApi): List<Command> = api.listCommands()

    suspend fun getSessionTodos(api: OpenCodeApi, sessionId: String): List<Todo> = api.getSessionTodos(sessionId)

    suspend fun shareSession(api: OpenCodeApi, sessionId: String): Session = api.shareSession(sessionId)

    suspend fun unshareSession(api: OpenCodeApi, sessionId: String): Session = api.unshareSession(sessionId)
}
