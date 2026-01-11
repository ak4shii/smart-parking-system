package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.mqtt.MqttProvisionRequestDto;
import com.smart_parking_system.backend.dto.mqtt.MqttProvisionResponseDto;

public interface IMqttProvisionService {

    MqttProvisionRequestDto checkForProvisionData(String username, String mcCode, int timeoutSeconds);

    MqttProvisionResponseDto handleProvision(String username, String mcCode, MqttProvisionRequestDto request);
}
