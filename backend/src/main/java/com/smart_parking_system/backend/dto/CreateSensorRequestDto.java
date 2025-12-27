package com.smart_parking_system.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSensorRequestDto {

    @NotBlank(message = "name is required")
    @Size(min = 1, max = 100, message = "name length must be between 1 and 100 characters")
    private String name;

    @NotBlank(message = "type is required")
    @Size(min = 1, max = 50, message = "type length must be between 1 and 50 characters")
    private String type;

    @NotNull(message = "slotId is required")
    private Integer slotId;

    @NotNull(message = "microcontrollerId is required")
    private Integer microcontrollerId;
}

