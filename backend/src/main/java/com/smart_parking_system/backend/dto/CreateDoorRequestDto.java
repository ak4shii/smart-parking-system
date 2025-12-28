package com.smart_parking_system.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDoorRequestDto {

    private String name;

    @NotNull
    private Integer microcontrollerId;

    private Boolean isOpened;
}



