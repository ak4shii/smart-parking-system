package com.smart_parking_system.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSlotRequestDto {

    @NotNull(message = "parkingSpaceId is required")
    private Integer parkingSpaceId;
}







