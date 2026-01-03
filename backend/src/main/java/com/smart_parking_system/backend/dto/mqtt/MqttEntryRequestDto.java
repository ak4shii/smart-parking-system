package com.smart_parking_system.backend.dto.mqtt;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MqttEntryRequestDto {

    @NotBlank
    private String rfidCode;

    @NotBlank
    private String imageBase64;
}



