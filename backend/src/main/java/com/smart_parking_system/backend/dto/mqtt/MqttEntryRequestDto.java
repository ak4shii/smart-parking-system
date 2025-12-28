package com.smart_parking_system.backend.dto.mqtt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MqttEntryRequestDto {

    @NotBlank
    @Size(min = 1, max = 100)
    private String rfidCode;

    @NotBlank
    @Size(min = 1, max = 50)
    private String licensePlate;
}



