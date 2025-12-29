package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.SensorDto;
import com.smart_parking_system.backend.dto.mqtt.MqttSensorStatusDto;

public interface IMqttSensorService {

    SensorDto handleSensorStatus(String mcCode, MqttSensorStatusDto status);
}

