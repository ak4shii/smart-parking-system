package com.smart_parking_system.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class CreateSlotRequestDto {

    @NotNull(message = "parkingSpaceId is required")
    private Integer parkingSpaceId;

    @NotBlank(message = "name is required")
    private String name;
}







