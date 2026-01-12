package com.smartparking.mobile.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Long,
    val username: String,
    val email: String,
    val role: String,
    val enabled: Boolean,
    val createdAt: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val user: User?,
    val jwtToken: String?
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class RegisterResponse(
    val message: String,
    val mqttUsername: String? = null,
    val mqttPassword: String? = null,
    val mqttBrokerUri: String? = null
)
