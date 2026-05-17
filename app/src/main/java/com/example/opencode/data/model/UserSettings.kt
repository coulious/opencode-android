package com.example.opencode.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val userName: String = "User",
    val themeColor: Int = 0,
)
