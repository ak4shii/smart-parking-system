package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.mqtt.MqttProvisionRequestDto;
import com.smart_parking_system.backend.dto.mqtt.MqttProvisionResponseDto;

public interface IMqttProvisionService {

    MqttProvisionRequestDto checkForProvisionData(String mcCode, int timeoutSeconds);

    MqttProvisionResponseDto handleProvision(String mcCode, MqttProvisionRequestDto request);
}
