package com.smart_parking_system.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttCredentialsResponseDto {

    private String mqttHost;

    private Integer mqttPort;

    private String mqttUsername;

    private String mqttPassword;

    private String baseTopic;

    private String mcCode;

    private String deviceName;
}
