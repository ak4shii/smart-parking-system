package com.smart_parking_system.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExitRequestDto {

    @NotBlank
    private String mcCode;

    @NotBlank
    private String rfidCode;
}
