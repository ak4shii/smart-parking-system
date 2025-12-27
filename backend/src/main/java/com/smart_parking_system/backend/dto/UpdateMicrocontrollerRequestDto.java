package com.smart_parking_system.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateMicrocontrollerRequestDto {

    @NotBlank(message = "name is required")
    @Size(min = 1, max = 100, message = "name length must be between 1 and 100 characters")
    private String name;
}

