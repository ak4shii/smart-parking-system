package com.smart_parking_system.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateParkingSpaceRequestDto {

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100, message = "Name length must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Location is required")
    @Size(min = 3, max = 200, message = "Location length must be between 3 and 200 characters")
    private String location;
}








