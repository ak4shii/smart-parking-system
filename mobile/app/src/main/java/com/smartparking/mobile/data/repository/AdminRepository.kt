package com.smartparking.mobile.data.repository

import com.smartparking.mobile.data.api.ApiService
import com.smartparking.mobile.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

sealed class AdminResult<out T> {
    data class Success<T>(val data: T) : AdminResult<T>()
    data class Error(val message: String) : AdminResult<Nothing>()
}

@Singleton
class AdminRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun getAllParkingSpaces(): AdminResult<List<ParkingSpace>> {
        return try {
            val response = apiService.getAllParkingSpaces()
            if (response.isSuccessful) {
                AdminResult.Success(response.body() ?: emptyList())
            } else {
                AdminResult.Error("Failed to fetch parking spaces: ${response.code()}")
            }
        } catch (e: Exception) {
            AdminResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun createParkingSpace(name: String, location: String): AdminResult<ParkingSpace> {
        return try {
            val request = CreateParkingSpaceRequest(name, location)
            val response = apiService.createParkingSpace(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    AdminResult.Success(it)
                } ?: AdminResult.Error("Empty response")
            } else {
                AdminResult.Error("Failed to create parking space: ${response.code()}")
            }
        } catch (e: Exception) {
            AdminResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun updateParkingSpace(id: Long, name: String, location: String): AdminResult<ParkingSpace> {
        return try {
            val request = UpdateParkingSpaceRequest(name, location)
            val response = apiService.updateParkingSpace(id, request)
            if (response.isSuccessful) {
                response.body()?.let {
                    AdminResult.Success(it)
                } ?: AdminResult.Error("Empty response")
            } else {
                AdminResult.Error("Failed to update parking space: ${response.code()}")
            }
        } catch (e: Exception) {
            AdminResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun deleteParkingSpace(id: Long): AdminResult<Unit> {
        return try {
            val response = apiService.deleteParkingSpace(id)
            if (response.isSuccessful) {
                AdminResult.Success(Unit)
            } else {
                AdminResult.Error("Failed to delete parking space: ${response.code()}")
            }
        } catch (e: Exception) {
            AdminResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun createMicrocontroller(
        mcCode: String,
        name: String,
        parkingSpaceId: Long
    ): AdminResult<CreateMicrocontrollerResponse> {
        return try {
            val request = CreateMicrocontrollerRequest(mcCode, name, parkingSpaceId)
            val response = apiService.createMicrocontroller(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    AdminResult.Success(it)
                } ?: AdminResult.Error("Empty response")
            } else {
                AdminResult.Error("Failed to create microcontroller: ${response.code()}")
            }
        } catch (e: Exception) {
            AdminResult.Error(e.message ?: "Unknown error")
        }
    }
}
