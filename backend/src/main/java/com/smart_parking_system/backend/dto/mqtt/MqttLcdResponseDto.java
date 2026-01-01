package com.smart_parking_system.backend.dto.mqtt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttLcdResponseDto {

    private boolean success;
    private String message;
    private Integer lcdId;
}




