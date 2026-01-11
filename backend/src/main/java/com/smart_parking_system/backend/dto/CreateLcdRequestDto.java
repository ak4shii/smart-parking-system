package com.smart_parking_system.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateLcdRequestDto {

    private String name;

    private String displayText;

    @NotNull
    private Integer microcontrollerId;
}



