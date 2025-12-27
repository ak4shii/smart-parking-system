package com.smart_parking_system.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferParkingSpaceOwnershipRequestDto {

    @NotNull(message = "newOwnerUserId is required")
    private Integer newOwnerUserId;
}


