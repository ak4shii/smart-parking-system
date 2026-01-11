package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.MqttCredentialsResponseDto;
import com.smart_parking_system.backend.entity.Microcontroller;

public interface IMqttCredentialService {

    MqttCredentialsResponseDto generateCredentials(Microcontroller mc, String ownerUsername);

    MqttCredentialsResponseDto regenerateCredentials(Microcontroller mc, String ownerUsername);

    void revokeCredentials(Microcontroller mc);

    boolean syncToMosquitto(String mqttUsername, String plainPassword);

    boolean removeFromMosquitto(String mqttUsername);

    MqttCredentialsResponseDto getCredentialsInfo(String mcCode, String ownerUsername);
}
