package com.smart_parking_system.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateRfidRequestDto {

    @NotBlank(message = "rfidCode is required")
    @Size(min = 3, max = 100, message = "rfidCode length must be between 3 and 100 characters")
    private String rfidCode;

    @NotNull(message = "parkingSpaceId is required")
    private Integer parkingSpaceId;
}





