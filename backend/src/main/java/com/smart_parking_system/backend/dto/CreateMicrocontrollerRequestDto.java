package com.smart_parking_system.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateMicrocontrollerRequestDto {

    @NotBlank(message = "mcCode is required")
    @Size(min = 3, max = 100, message = "mcCode length must be between 3 and 100 characters")
    private String mcCode;

    @NotBlank(message = "name is required")
    @Size(min = 1, max = 100, message = "name length must be between 1 and 100 characters")
    private String name;

    @NotNull(message = "parkingSpaceId is required")
    private Integer parkingSpaceId;
}







