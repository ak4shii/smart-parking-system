package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.mqtt.MqttProvisionRequestDto;
import com.smart_parking_system.backend.dto.mqtt.MqttProvisionResponseDto;

public interface IMqttProvisionService {

    MqttProvisionResponseDto handleProvision(String mcCode, MqttProvisionRequestDto request);
}


