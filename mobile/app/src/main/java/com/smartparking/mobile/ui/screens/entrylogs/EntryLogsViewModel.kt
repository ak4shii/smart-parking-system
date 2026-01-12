package com.smartparking.mobile.ui.screens.entrylogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartparking.mobile.data.model.EntryLog
import com.smartparking.mobile.data.model.ParkingSpace
import com.smartparking.mobile.data.repository.ParkingRepository
import com.smartparking.mobile.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class EntryLogsUiState(
    val parkingSpaces: List<ParkingSpace> = emptyList(),
    val selectedParkingSpaceId: Long? = null,
    val entryLogs: List<EntryLog> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val selectedParkingSpace: ParkingSpace? get() = parkingSpaces.find { it.id == selectedParkingSpaceId }
}

@HiltViewModel
class EntryLogsViewModel @Inject constructor(
    private val parkingRepository: ParkingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntryLogsUiState())
    val uiState: StateFlow<EntryLogsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val spacesResult = parkingRepository.getAllParkingSpaces()) {
                is Result.Success -> {
                    val spaces = spacesResult.data
                    val savedId = parkingRepository.selectedParkingSpaceId.first()
                    val selectedId = if (savedId != null && spaces.any { it.id == savedId }) {
                        savedId
                    } else {
                        spaces.firstOrNull()?.id
                    }

                    _uiState.value = _uiState.value.copy(
                        parkingSpaces = spaces,
                        selectedParkingSpaceId = selectedId
                    )

                    if (selectedId != null) {
                        loadEntryLogs(selectedId)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = spacesResult.message
                    )
                }
                Result.Loading -> {}
            }
        }
    }

    private fun loadEntryLogs(parkingSpaceId: Long) {
        viewModelScope.launch {
            when (val result = parkingRepository.getEntryLogsByParkingSpace(parkingSpaceId)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        entryLogs = result.data.sortedByDescending { 
                            parseDateTime(it.inTime) 
                        },
                        isLoading = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                Result.Loading -> {}
            }
        }
    }

    fun selectParkingSpace(id: Long) {
        viewModelScope.launch {
            parkingRepository.saveSelectedParkingSpaceId(id)
            _uiState.value = _uiState.value.copy(
                selectedParkingSpaceId = id,
                isLoading = true
            )
            loadEntryLogs(id)
        }
    }

    fun refresh() {
        val selectedId = _uiState.value.selectedParkingSpaceId
        if (selectedId != null) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            loadEntryLogs(selectedId)
        }
    }

    private fun parseDateTime(dateString: String): Date {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(dateString) ?: Date(0)
        } catch (e: Exception) {
            Date(0)
        }
    }
}
