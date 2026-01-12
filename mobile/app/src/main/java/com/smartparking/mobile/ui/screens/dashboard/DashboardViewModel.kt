package com.smartparking.mobile.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartparking.mobile.data.model.ParkingSpace
import com.smartparking.mobile.data.model.Slot
import com.smartparking.mobile.data.model.User
import com.smartparking.mobile.data.repository.AuthRepository
import com.smartparking.mobile.data.repository.ParkingRepository
import com.smartparking.mobile.data.repository.Result
import com.smartparking.mobile.data.websocket.WebSocketEvent
import com.smartparking.mobile.data.websocket.WebSocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SlotDisplay(
    val id: Long,
    val label: String,
    val occupied: Boolean,
    val parkingSpaceId: Long
)

data class DashboardUiState(
    val user: User? = null,
    val parkingSpaces: List<ParkingSpace> = emptyList(),
    val selectedParkingSpaceId: Long? = null,
    val slots: List<SlotDisplay> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isWebSocketConnected: Boolean = false
) {
    val totalSlots: Int get() = slots.size
    val occupiedSlots: Int get() = slots.count { it.occupied }
    val availableSlots: Int get() = totalSlots - occupiedSlots
    val selectedParkingSpace: ParkingSpace? get() = parkingSpaces.find { it.id == selectedParkingSpaceId }
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val parkingRepository: ParkingRepository,
    private val webSocketService: WebSocketService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadUser()
        loadData()
        connectWebSocket()
    }

    private fun connectWebSocket() {
        viewModelScope.launch {
            // Connect to WebSocket
            webSocketService.connect()
            
            // Listen for WebSocket events
            webSocketService.events.collect { event ->
                when (event) {
                    is WebSocketEvent.Connected -> {
                        _uiState.value = _uiState.value.copy(isWebSocketConnected = true)
                    }
                    is WebSocketEvent.Disconnected -> {
                        _uiState.value = _uiState.value.copy(isWebSocketConnected = false)
                    }
                    is WebSocketEvent.SlotUpdate -> {
                        val slot = event.slot
                        val selectedId = _uiState.value.selectedParkingSpaceId
                        if (slot.parkingSpaceId == selectedId) {
                            updateSlotFromWebSocket(slot)
                        }
                    }
                    is WebSocketEvent.OverviewUpdate -> {
                        // Refresh data when overview updates
                        loadSlots()
                    }
                    is WebSocketEvent.SensorUpdate -> {
                        // Sensor update may indicate slot change, refresh slots
                        loadSlots()
                    }
                    is WebSocketEvent.EntryLogEvent -> {
                        // New entry log, could refresh if needed
                    }
                    is WebSocketEvent.Error -> {
                        // Handle error silently or show a toast
                    }
                }
            }
        }
    }

    private fun updateSlotFromWebSocket(slot: Slot) {
        val currentSlots = _uiState.value.slots.toMutableList()
        val index = currentSlots.indexOfFirst { it.id == slot.id }
        if (index >= 0) {
            currentSlots[index] = currentSlots[index].copy(occupied = slot.isOccupied)
            _uiState.value = _uiState.value.copy(slots = currentSlots)
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(user = user)
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Load parking spaces
            when (val spacesResult = parkingRepository.getAllParkingSpaces()) {
                is Result.Success -> {
                    val spaces = spacesResult.data
                    
                    // Get saved or first parking space
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

                    // Load slots
                    loadSlots()
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

    private fun loadSlots() {
        viewModelScope.launch {
            when (val slotsResult = parkingRepository.getAllSlots()) {
                is Result.Success -> {
                    val allSlots = slotsResult.data
                    val selectedId = _uiState.value.selectedParkingSpaceId
                    
                    val filteredSlots = if (selectedId != null) {
                        allSlots.filter { it.parkingSpaceId == selectedId }
                    } else {
                        allSlots
                    }.sortedBy { it.id }

                    val displaySlots = filteredSlots.mapIndexed { index, slot ->
                        SlotDisplay(
                            id = slot.id,
                            label = "S${(index + 1).toString().padStart(2, '0')}",
                            occupied = slot.isOccupied,
                            parkingSpaceId = slot.parkingSpaceId
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        slots = displaySlots,
                        isLoading = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = slotsResult.message
                    )
                }
                Result.Loading -> {}
            }
        }
    }

    fun selectParkingSpace(id: Long) {
        viewModelScope.launch {
            parkingRepository.saveSelectedParkingSpaceId(id)
            _uiState.value = _uiState.value.copy(selectedParkingSpaceId = id)
            loadSlots()
        }
    }

    fun refresh() {
        loadData()
    }

    fun logout() {
        viewModelScope.launch {
            webSocketService.disconnect()
            authRepository.logout()
        }
    }

    fun updateSlotOccupancy(slotId: Long, isOccupied: Boolean) {
        _uiState.value = _uiState.value.copy(
            slots = _uiState.value.slots.map { slot ->
                if (slot.id == slotId) slot.copy(occupied = isOccupied) else slot
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        // Don't disconnect here - service is singleton and may be used by other ViewModels
    }
}
