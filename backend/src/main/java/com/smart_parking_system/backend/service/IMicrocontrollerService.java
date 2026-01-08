package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.CreateMicrocontrollerRequestDto;
import com.smart_parking_system.backend.dto.MicrocontrollerDto;
import com.smart_parking_system.backend.dto.MqttCredentialsResponseDto;
import com.smart_parking_system.backend.dto.UpdateMicrocontrollerRequestDto;

import java.util.List;

public interface IMicrocontrollerService {

    MicrocontrollerDto createMicrocontroller(CreateMicrocontrollerRequestDto requestDto);

    MicrocontrollerDto getMicrocontrollerById(Integer id);

    List<MicrocontrollerDto> getAllMicrocontrollersByMyParkingSpaces();

    MicrocontrollerDto updateMicrocontroller(Integer id, UpdateMicrocontrollerRequestDto requestDto);

    void deleteMicrocontroller(Integer id);

    /**
     * Regenerate MQTT credentials for a device.
     * Old credentials will be revoked and new ones generated.
     *
     * @param id The microcontroller ID
     * @return New MQTT credentials (password shown once)
     */
    MqttCredentialsResponseDto regenerateMqttCredentials(Integer id);

    /**
     * Revoke MQTT access for a device without deleting it.
     *
     * @param id The microcontroller ID
     */
    void revokeMqttCredentials(Integer id);
}
