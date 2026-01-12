package com.smartparking.mobile.data.repository

import com.smartparking.mobile.data.api.ApiService
import com.smartparking.mobile.data.model.Microcontroller
import com.smartparking.mobile.data.model.MqttCredentials
import com.smartparking.mobile.data.model.Sensor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun getAllSensors(): Result<List<Sensor>> {
        return try {
            val response = apiService.getAllSensors()
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Failed to fetch sensors")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getSensorsByParkingSpace(parkingSpaceId: Long): Result<List<Sensor>> {
        return when (val result = getAllSensors()) {
            is Result.Success -> {
                val filtered = result.data.filter { it.parkingSpaceId == parkingSpaceId }
                Result.Success(filtered)
            }
            is Result.Error -> result
            Result.Loading -> Result.Loading
        }
    }

    suspend fun getAllMicrocontrollers(): Result<List<Microcontroller>> {
        return try {
            val response = apiService.getAllMicrocontrollers()
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Failed to fetch microcontrollers")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getMicrocontrollersByParkingSpace(parkingSpaceId: Long): Result<List<Microcontroller>> {
        return when (val result = getAllMicrocontrollers()) {
            is Result.Success -> {
                val filtered = result.data.filter { it.parkingSpaceId == parkingSpaceId }
                Result.Success(filtered)
            }
            is Result.Error -> result
            Result.Loading -> Result.Loading
        }
    }

    suspend fun regenerateMqttCredentials(id: Long): Result<MqttCredentials> {
        return try {
            val response = apiService.regenerateMqttCredentials(id)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to regenerate credentials")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun revokeMqttCredentials(id: Long): Result<Unit> {
        return try {
            val response = apiService.revokeMqttCredentials(id)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("Failed to revoke credentials")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
