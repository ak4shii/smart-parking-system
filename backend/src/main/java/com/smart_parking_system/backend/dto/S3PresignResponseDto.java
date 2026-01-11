package com.smart_parking_system.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class S3PresignResponseDto {
    private String url;
}

