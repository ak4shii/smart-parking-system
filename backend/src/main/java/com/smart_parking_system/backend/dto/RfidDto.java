package com.smart_parking_system.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RfidDto {
    
    private Integer id;
    private String rfidCode;
    private Boolean currentlyUsed;
    private Integer parkingSpaceId;
}





