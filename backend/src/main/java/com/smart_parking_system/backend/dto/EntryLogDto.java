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

    /**
     * Key/path of the license plate image in S3 (or CloudFront origin), e.g. "plates/ps-1/log-10.jpg".
     * Provision/YOLO pipeline should set this when available.
     */
    private String licensePlateImageKey;
}
