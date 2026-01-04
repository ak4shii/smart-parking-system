package com.smart_parking_system.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RegisterResponseDto {

    private String message;
    private String mqttUsername;
    private String mqttPassword;
    private String mqttBroker;
}
