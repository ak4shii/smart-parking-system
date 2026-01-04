package com.smart_parking_system.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExitResponseDto {

    private boolean success;
    private String message;
    private Integer entryLogId;
}
