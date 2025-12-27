package com.smart_parking_system.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateEntryRequestDto {

    @NotBlank(message = "rfidCode is required")
    @Size(min = 1, max = 100)
    private String rfidCode;

    @NotBlank(message = "licensePlate is required")
    @Size(min = 1, max = 50)
    private String licensePlate;
}


