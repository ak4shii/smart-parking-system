package com.smartparking.mobile.data.repository

import com.smartparking.mobile.data.api.ApiService
import com.smartparking.mobile.data.local.TokenDataStore
import com.smartparking.mobile.data.model.LoginRequest
import com.smartparking.mobile.data.model.LoginResponse
import com.smartparking.mobile.data.model.RegisterRequest
import com.smartparking.mobile.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
}

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenDataStore: TokenDataStore
) {

    val currentUser: Flow<User?> = tokenDataStore.user
    val token: Flow<String?> = tokenDataStore.token

    suspend fun isLoggedIn(): Boolean {
        return !tokenDataStore.token.first().isNullOrEmpty()
    }

    suspend fun login(email: String, password: String): AuthResult<User> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.jwtToken != null && body.user != null) {
                    // Save token and user
                    tokenDataStore.saveToken(body.jwtToken)
                    tokenDataStore.saveUser(body.user)
                    AuthResult.Success(body.user)
                } else {
                    AuthResult.Error(body?.message ?: "Login failed")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                AuthResult.Error(errorBody ?: "Login failed")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun register(username: String, email: String, password: String): AuthResult<String> {
        return try {
            val response = apiService.register(RegisterRequest(username, email, password))
            
            if (response.isSuccessful) {
                val body = response.body()
                AuthResult.Success(body?.message ?: "Registration successful")
            } else {
                val errorBody = response.errorBody()?.string()
                AuthResult.Error(errorBody ?: "Registration failed")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun logout() {
        tokenDataStore.clearAll()
    }
}
