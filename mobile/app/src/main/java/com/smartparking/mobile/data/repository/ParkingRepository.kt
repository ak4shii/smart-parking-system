package com.smartparking.mobile.data.repository

import com.smartparking.mobile.data.api.ApiService
import com.smartparking.mobile.data.local.TokenDataStore
import com.smartparking.mobile.data.model.EntryLog
import com.smartparking.mobile.data.model.ParkingSpace
import com.smartparking.mobile.data.model.Slot
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

@Singleton
class ParkingRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenDataStore: TokenDataStore
) {

    val selectedParkingSpaceId: Flow<Long?> = tokenDataStore.selectedParkingSpaceId

    suspend fun saveSelectedParkingSpaceId(id: Long) {
        tokenDataStore.saveSelectedParkingSpaceId(id)
    }

    suspend fun getAllParkingSpaces(): Result<List<ParkingSpace>> {
        return try {
            val response = apiService.getAllParkingSpaces()
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Failed to fetch parking spaces")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getAllSlots(): Result<List<Slot>> {
        return try {
            val response = apiService.getAllSlots()
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Failed to fetch slots")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getSlotsByParkingSpace(parkingSpaceId: Long): Result<List<Slot>> {
        return when (val result = getAllSlots()) {
            is Result.Success -> {
                val filtered = result.data.filter { it.parkingSpaceId == parkingSpaceId }
                Result.Success(filtered)
            }
            is Result.Error -> result
            Result.Loading -> Result.Loading
        }
    }

    suspend fun getEntryLogsByParkingSpace(parkingSpaceId: Long): Result<List<EntryLog>> {
        return try {
            val response = apiService.getEntryLogsByParkingSpace(parkingSpaceId)
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Failed to fetch entry logs")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
