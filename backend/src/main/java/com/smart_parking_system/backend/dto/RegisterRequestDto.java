package com.smart_parking_system.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequestDto {

    @NotBlank(message = "Display name is required")
    @Size(min = 8, max = 50, message = "Display name length must be between 8 and 50 characters")
    private String username;

    @NotBlank(message = "Email address is required")
    @Email(message = "Email must be a valid format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 20, message = "Password length must be between 8 and 20 characters")
    private String password;

}
