package com.smart_parking_system.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SlotDto {
    
    private Integer id;
    private Integer parkingSpaceId;
    private Boolean isOccupied;
}





