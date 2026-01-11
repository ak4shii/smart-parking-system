package com.smart_parking_system.backend.dto.mqtt;

import lombok.Data;

@Data
public class MqttStatusRequestDto {

    private Boolean online;
    private Long uptimeSec;
}



