package com.smart_parking_system.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SensorDto {
    
    private Integer id;
    private String name;
    private String type;
    private Integer slotId;
    private String slotName;
    private Integer microcontrollerId;
    private Integer parkingSpaceId;
}







