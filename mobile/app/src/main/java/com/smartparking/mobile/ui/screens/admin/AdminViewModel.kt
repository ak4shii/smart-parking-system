package com.smartparking.mobile.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartparking.mobile.data.model.CreateMicrocontrollerResponse
import com.smartparking.mobile.data.model.MqttCredentials
import com.smartparking.mobile.data.model.ParkingSpace
import com.smartparking.mobile.data.repository.AdminRepository
import com.smartparking.mobile.data.repository.AdminResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val parkingSpaces: List<ParkingSpace> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    
    // Wizard state
    val showCreateWizard: Boolean = false,
    val wizardStep: Int = 1,
    val parkingSpaceName: String = "",
    val parkingSpaceLocation: String = "",
    val microcontrollerCode: String = "",
    val microcontrollerName: String = "",
    val isSubmitting: Boolean = false,
    
    // Edit state
    val showEditDialog: Boolean = false,
    val editingSpace: ParkingSpace? = null,
    val editName: String = "",
    val editLocation: String = "",
    
    // Delete state
    val showDeleteConfirm: Boolean = false,
    val deletingSpace: ParkingSpace? = null,
    
    // MQTT Credentials
    val showMqttCredentials: Boolean = false,
    val mqttCredentials: MqttCredentials? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadParkingSpaces()
    }

    fun loadParkingSpaces() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = adminRepository.getAllParkingSpaces()) {
                is AdminResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        parkingSpaces = result.data,
                        isLoading = false
                    )
                }
                is AdminResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    // ============== Create Wizard ==============
    
    fun showCreateWizard() {
        _uiState.value = _uiState.value.copy(
            showCreateWizard = true,
            wizardStep = 1,
            parkingSpaceName = "",
            parkingSpaceLocation = "",
            microcontrollerCode = "",
            microcontrollerName = ""
        )
    }

    fun hideCreateWizard() {
        _uiState.value = _uiState.value.copy(showCreateWizard = false)
    }

    fun updateParkingSpaceName(name: String) {
        _uiState.value = _uiState.value.copy(parkingSpaceName = name)
    }

    fun updateParkingSpaceLocation(location: String) {
        _uiState.value = _uiState.value.copy(parkingSpaceLocation = location)
    }

    fun updateMicrocontrollerCode(code: String) {
        _uiState.value = _uiState.value.copy(microcontrollerCode = code)
    }

    fun updateMicrocontrollerName(name: String) {
        _uiState.value = _uiState.value.copy(microcontrollerName = name)
    }

    fun nextStep(): Boolean {
        val state = _uiState.value
        
        when (state.wizardStep) {
            1 -> {
                if (state.parkingSpaceName.isBlank() || state.parkingSpaceLocation.isBlank()) {
                    _uiState.value = state.copy(error = "Please fill in all fields")
                    return false
                }
                if (state.parkingSpaceName.length < 3) {
                    _uiState.value = state.copy(error = "Name must be at least 3 characters")
                    return false
                }
            }
            2 -> {
                if (state.microcontrollerCode.isBlank() || state.microcontrollerName.isBlank()) {
                    _uiState.value = state.copy(error = "Please fill in all fields")
                    return false
                }
                if (state.microcontrollerCode.length < 3) {
                    _uiState.value = state.copy(error = "Code must be at least 3 characters")
                    return false
                }
            }
        }
        
        _uiState.value = state.copy(
            wizardStep = state.wizardStep + 1,
            error = null
        )
        return true
    }

    fun previousStep() {
        val state = _uiState.value
        if (state.wizardStep > 1) {
            _uiState.value = state.copy(wizardStep = state.wizardStep - 1)
        }
    }

    fun completeWizard() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.value = state.copy(isSubmitting = true, error = null)

            // Step 1: Create Parking Space
            when (val psResult = adminRepository.createParkingSpace(
                state.parkingSpaceName.trim(),
                state.parkingSpaceLocation.trim()
            )) {
                is AdminResult.Success -> {
                    val parkingSpace = psResult.data
                    
                    // Step 2: Create Microcontroller
                    when (val mcResult = adminRepository.createMicrocontroller(
                        state.microcontrollerCode.trim(),
                        state.microcontrollerName.trim(),
                        parkingSpace.id
                    )) {
                        is AdminResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isSubmitting = false,
                                showCreateWizard = false,
                                successMessage = "Parking space created successfully!",
                                mqttCredentials = mcResult.data.mqttCredentials,
                                showMqttCredentials = mcResult.data.mqttCredentials != null
                            )
                            loadParkingSpaces()
                        }
                        is AdminResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isSubmitting = false,
                                error = "Parking space created but microcontroller failed: ${mcResult.message}"
                            )
                            loadParkingSpaces()
                        }
                    }
                }
                is AdminResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = psResult.message
                    )
                }
            }
        }
    }

    // ============== Edit ==============
    
    fun showEditDialog(space: ParkingSpace) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            editingSpace = space,
            editName = space.name,
            editLocation = space.location
        )
    }

    fun hideEditDialog() {
        _uiState.value = _uiState.value.copy(
            showEditDialog = false,
            editingSpace = null
        )
    }

    fun updateEditName(name: String) {
        _uiState.value = _uiState.value.copy(editName = name)
    }

    fun updateEditLocation(location: String) {
        _uiState.value = _uiState.value.copy(editLocation = location)
    }

    fun saveEdit() {
        viewModelScope.launch {
            val state = _uiState.value
            val space = state.editingSpace ?: return@launch
            
            if (state.editName.isBlank() || state.editLocation.isBlank()) {
                _uiState.value = state.copy(error = "Please fill in all fields")
                return@launch
            }
            
            _uiState.value = state.copy(isSubmitting = true, error = null)
            
            when (val result = adminRepository.updateParkingSpace(
                space.id,
                state.editName.trim(),
                state.editLocation.trim()
            )) {
                is AdminResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        showEditDialog = false,
                        editingSpace = null,
                        successMessage = "Parking space updated!"
                    )
                    loadParkingSpaces()
                }
                is AdminResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = result.message
                    )
                }
            }
        }
    }

    // ============== Delete ==============
    
    fun showDeleteConfirm(space: ParkingSpace) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = true,
            deletingSpace = space
        )
    }

    fun hideDeleteConfirm() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = false,
            deletingSpace = null
        )
    }

    fun confirmDelete() {
        viewModelScope.launch {
            val state = _uiState.value
            val space = state.deletingSpace ?: return@launch
            
            _uiState.value = state.copy(isSubmitting = true, error = null)
            
            when (val result = adminRepository.deleteParkingSpace(space.id)) {
                is AdminResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        showDeleteConfirm = false,
                        deletingSpace = null,
                        successMessage = "Parking space deleted!"
                    )
                    loadParkingSpaces()
                }
                is AdminResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = result.message
                    )
                }
            }
        }
    }

    // ============== Utils ==============
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun hideMqttCredentials() {
        _uiState.value = _uiState.value.copy(
            showMqttCredentials = false,
            mqttCredentials = null
        )
    }
}
