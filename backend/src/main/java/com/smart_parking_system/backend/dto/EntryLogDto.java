package com.smart_parking_system.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@ToString
public class EntryLogDto {
    private Integer id;
    private Integer rfidId;
    private String rfidCode;
    private String licensePlate;
    private Instant inTime;
    private Instant outTime;
    private Integer parkingSpaceId;
}
