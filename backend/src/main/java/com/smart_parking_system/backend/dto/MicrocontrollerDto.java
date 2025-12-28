package com.smart_parking_system.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@ToString
public class MicrocontrollerDto {

    private Integer id;
    private String mcCode;
    private String name;
    private Boolean online;
    private Long uptimeSec;
    private Instant lastSeen;
    private Integer parkingSpaceId;
}





