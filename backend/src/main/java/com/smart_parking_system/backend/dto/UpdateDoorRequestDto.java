package com.smart_parking_system.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDoorRequestDto {

    private String name;
    private Boolean isOpened;
}

