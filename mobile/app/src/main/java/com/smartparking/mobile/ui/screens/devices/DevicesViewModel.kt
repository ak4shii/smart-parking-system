package com.smartparking.mobile.ui.screens.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartparking.mobile.data.model.Microcontroller
import com.smartparking.mobile.data.model.MqttCredentials
import com.smartparking.mobile.data.model.ParkingSpace
import com.smartparking.mobile.data.model.Sensor
import com.smartparking.mobile.data.model.Slot
import com.smartparking.mobile.data.repository.DeviceRepository
import com.smartparking.mobile.data.repository.ParkingRepository
import com.smartparking.mobile.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DevicesUiState(
    val parkingSpaces: List<ParkingSpace> = emptyList(),
    val selectedParkingSpaceId: Long? = null,
    val sensors: List<Sensor> = emptyList(),
    val microcontrollers: List<Microcontroller> = emptyList(),
    val slots: List<Slot> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    // Regenerate key states
    val isRegenerating: Boolean = false,
    val regeneratingMcId: Long? = null,
    val showMqttCredentials: Boolean = false,
    val mqttCredentials: MqttCredentials? = null,
    val successMessage: String? = null
) {
    val selectedParkingSpace: ParkingSpace? get() = parkingSpaces.find { it.id == selectedParkingSpaceId }
    
    val filteredSensors: List<Sensor> get() {
        val filtered = if (selectedParkingSpaceId != null) {
            sensors.filter { it.parkingSpaceId == selectedParkingSpaceId }
        } else {
            sensors
        }
        
        return if (searchQuery.isBlank()) {
            filtered
        } else {
            filtered.filter { 
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.type.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredMicrocontrollers: List<Microcontroller> get() {
        return if (selectedParkingSpaceId != null) {
            microcontrollers.filter { it.parkingSpaceId == selectedParkingSpaceId }
        } else {
            microcontrollers
        }
    }

    val totalSensors: Int get() = filteredSensors.size
    val activeSensors: Int get() = filteredSensors.count { sensor ->
        microcontrollers.find { it.id == sensor.microcontrollerId }?.online == true
    }
    val ultrasonicCount: Int get() = filteredSensors.count { it.type == "ultrasonic" }
    val infraredCount: Int get() = filteredSensors.count { it.type == "infrared" }
}

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val parkingRepository: ParkingRepository,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Load parking spaces first
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
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = spacesResult.message
                    )
                    return@launch
                }
                Result.Loading -> {}
            }

            // Load all other data in parallel
            loadDevices()
        }
    }

    private fun loadDevices() {
        viewModelScope.launch {
            // Load sensors
            val sensorsResult = deviceRepository.getAllSensors()
            val microcontrollersResult = deviceRepository.getAllMicrocontrollers()
            val slotsResult = parkingRepository.getAllSlots()

            when {
                sensorsResult is Result.Success && 
                microcontrollersResult is Result.Success && 
                slotsResult is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        sensors = sensorsResult.data,
                        microcontrollers = microcontrollersResult.data,
                        slots = slotsResult.data,
                        isLoading = false
                    )
                }
                sensorsResult is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = sensorsResult.message
                    )
                }
                microcontrollersResult is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = microcontrollersResult.message
                    )
                }
                slotsResult is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = slotsResult.message
                    )
                }
            }
        }
    }

    fun selectParkingSpace(id: Long) {
        viewModelScope.launch {
            parkingRepository.saveSelectedParkingSpaceId(id)
            _uiState.value = _uiState.value.copy(selectedParkingSpaceId = id)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun refresh() {
        loadData()
    }

    fun regenerateMqttCredentials(mcId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRegenerating = true,
                regeneratingMcId = mcId,
                error = null
            )

            when (val result = deviceRepository.regenerateMqttCredentials(mcId)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isRegenerating = false,
                        regeneratingMcId = null,
                        mqttCredentials = result.data,
                        showMqttCredentials = true,
                        successMessage = "MQTT credentials regenerated!"
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isRegenerating = false,
                        regeneratingMcId = null,
                        error = result.message
                    )
                }
                Result.Loading -> {}
            }
        }
    }

    fun hideMqttCredentials() {
        _uiState.value = _uiState.value.copy(
            showMqttCredentials = false,
            mqttCredentials = null
        )
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun getSlotLabel(slotId: Long): String {
        val slot = _uiState.value.slots.find { it.id == slotId } ?: return "Slot #$slotId"
        val parkingSpaceSlots = _uiState.value.slots
            .filter { it.parkingSpaceId == slot.parkingSpaceId }
            .sortedBy { it.id }
        val index = parkingSpaceSlots.indexOfFirst { it.id == slotId }
        return "S${(index + 1).toString().padStart(2, '0')}"
    }

    fun getMicrocontrollerName(mcId: Long): String {
        return _uiState.value.microcontrollers.find { it.id == mcId }?.name ?: "MC-$mcId"
    }

    fun isMicrocontrollerOnline(mcId: Long): Boolean {
        return _uiState.value.microcontrollers.find { it.id == mcId }?.online == true
    }
}
