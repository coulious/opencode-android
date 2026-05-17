package com.example.opencode.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ConnectionConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val hostname: String = "127.0.0.1",
    val port: Int = 4096,
    val username: String = "opencode",
    val password: String = "",
) {
    val baseUrl: String get() = "http://$hostname:$port"
    val isAuthEnabled: Boolean get() = password.isNotBlank()
    val displayName: String get() = name.ifBlank { "$hostname:$port" }
}
