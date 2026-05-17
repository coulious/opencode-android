package com.example.opencode.data.remote

import com.example.opencode.data.model.Command
import com.example.opencode.data.model.CommandRequest
import com.example.opencode.data.model.HealthResponse
import com.example.opencode.data.model.Message
import com.example.opencode.data.model.PromptRequest
import com.example.opencode.data.model.Provider
import com.example.opencode.data.model.Session
import com.example.opencode.data.model.Todo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

data class PagedMessages(
    val messages: List<Message>,
    val nextCursor: String?,
)

@Singleton
class OpenCodeApi @Inject constructor(
    private val client: HttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun health(): HealthResponse = client.get("/global/health").body()

    suspend fun listSessions(): List<Session> = client.get("/session").body()

    suspend fun createSession(title: String? = null): Session {
        return client.post("/session") {
            contentType(ContentType.Application.Json)
            val body = buildMap<String, String> {
                if (title != null) put("title", title)
            }
            setBody(body)
        }.body()
    }

    suspend fun getSession(id: String): Session = client.get("/session/$id").body()

    suspend fun getSessionStatus(id: String): String {
        return try {
            val resp = client.get("/session/$id/status")
            val body = resp.bodyAsText()
            android.util.Log.d("OpenCodeApi", "GET /session/$id/status: $body")
            body
        } catch (e: Exception) {
            android.util.Log.e("OpenCodeApi", "getSessionStatus failed", e)
            "unknown"
        }
    }

    suspend fun deleteSession(id: String): Boolean = client.delete("/session/$id").body()

    suspend fun updateSession(id: String, title: String): Session {
        return client.patch("/session/$id") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("title" to title))
        }.body()
    }

    suspend fun listMessagesPaged(sessionId: String, limit: Int = 50, before: String? = null): PagedMessages {
        val resp: HttpResponse = client.get("/session/$sessionId/message") {
            url.parameters.append("limit", limit.toString())
            if (before != null) url.parameters.append("before", before)
        }
        val bodyText = resp.bodyAsText()
        android.util.Log.d("OpenCodeApi", "listMessagesPaged status=${resp.status} body=${bodyText.take(300)}")
        val messages = try {
            json.decodeFromString<List<Message>>(bodyText)
        } catch (e: Exception) {
            android.util.Log.e("OpenCodeApi", "listMessagesPaged parse failed", e)
            emptyList()
        }
        val nextCursor = resp.headers["X-Next-Cursor"]
        return PagedMessages(messages = messages, nextCursor = nextCursor)
    }

    suspend fun listMessages(sessionId: String, limit: Int? = null): List<Message> {
        val resp = client.get("/session/$sessionId/message") {
            if (limit != null) url.parameters.append("limit", limit.toString())
        }
        val body = resp.bodyAsText()
        android.util.Log.d("OpenCodeApi", "GET /session/$sessionId/message status=${resp.status} body=${body.take(300)}")
        return try {
            json.decodeFromString<List<Message>>(body)
        } catch (e: Exception) {
            android.util.Log.e("OpenCodeApi", "listMessages parse failed", e)
            emptyList()
        }
    }

    suspend fun sendMessage(sessionId: String, request: PromptRequest): Message {
        android.util.Log.d("OpenCodeApi", "POST /session/$sessionId/message body=$request")
        val resp = client.post("/session/$sessionId/message") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        android.util.Log.d("OpenCodeApi", "Response: status=${resp.status}")
        val bodyText = resp.bodyAsText()
        android.util.Log.d("OpenCodeApi", "Response body: ${bodyText.take(500)}")
        if (!resp.status.value.toString().startsWith("2")) {
            throw Exception("Server returned ${resp.status}: ${bodyText.take(200)}")
        }
        return try {
            json.decodeFromString<Message>(bodyText)
        } catch (e: Exception) {
            android.util.Log.e("OpenCodeApi", "Parse failed: ${e.message}")
            Message()
        }
    }

    suspend fun sendMessageAsync(sessionId: String, request: PromptRequest) {
        android.util.Log.d("OpenCodeApi", "POST /session/$sessionId/prompt_async body=$request")
        val resp = client.post("/session/$sessionId/prompt_async") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        android.util.Log.d("OpenCodeApi", "prompt_async response: status=${resp.status}")
        if (!resp.status.value.toString().startsWith("2")) {
            val body = resp.bodyAsText()
            android.util.Log.e("OpenCodeApi", "prompt_async failed: $body")
            throw Exception("Server returned ${resp.status}: ${body.take(200)}")
        }
    }

    suspend fun sendCommand(sessionId: String, request: CommandRequest): Message {
        return client.post("/session/$sessionId/command") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun abortSession(id: String): Boolean = client.post("/session/$id/abort").body()

    suspend fun listCommands(): List<Command> = client.get("/command").body()

    suspend fun listProviders(): Map<String, List<Provider>> = client.get("/provider").body()

    suspend fun getSessionTodos(sessionId: String): List<Todo> = client.get("/session/$sessionId/todo").body()

    suspend fun shareSession(id: String): Session = client.post("/session/$id/share").body()

    suspend fun unshareSession(id: String): Session = client.delete("/session/$id/share").body()
}
