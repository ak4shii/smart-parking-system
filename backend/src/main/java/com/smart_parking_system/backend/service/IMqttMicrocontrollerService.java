package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.MicrocontrollerDto;
import com.smart_parking_system.backend.dto.mqtt.MqttStatusRequestDto;

public interface IMqttMicrocontrollerService {

    MicrocontrollerDto handleStatus(String mcCode, MqttStatusRequestDto status);
}
