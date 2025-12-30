package com.smart_parking_system.backend.dto.mqtt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttLcdCommandDto {

    private Integer lcdId;
    private String displayText;
}


