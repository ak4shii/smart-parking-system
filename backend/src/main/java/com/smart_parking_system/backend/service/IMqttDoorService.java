package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.DoorDto;
import com.smart_parking_system.backend.dto.mqtt.MqttDoorStatusDto;

public interface IMqttDoorService {

    void sendDoorCommand(Integer doorId, boolean open);

    DoorDto handleDoorStatus(String mcCode, MqttDoorStatusDto status);
}



