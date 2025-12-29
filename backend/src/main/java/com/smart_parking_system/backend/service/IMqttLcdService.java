package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.LcdDto;
import com.smart_parking_system.backend.dto.mqtt.MqttLcdStatusDto;

public interface IMqttLcdService {

    void sendLcdCommand(Integer lcdId, String displayText);

    LcdDto handleLcdStatus(String mcCode, MqttLcdStatusDto status);
}



