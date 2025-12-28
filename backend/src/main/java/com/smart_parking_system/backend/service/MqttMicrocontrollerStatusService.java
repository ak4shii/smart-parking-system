package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.mqtt.MqttStatusRequestDto;

public interface MqttMicrocontrollerStatusService {

    void handleStatus(String mcCode, MqttStatusRequestDto status);
}
