package com.smart_parking_system.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DoorDto {

    private Integer id;
    private String name;
    private Boolean isOpened;
    private Integer microcontrollerId;
}

