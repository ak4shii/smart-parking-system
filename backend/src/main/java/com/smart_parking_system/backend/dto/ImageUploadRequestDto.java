package com.smart_parking_system.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageUploadRequestDto {
    
    @NotBlank(message = "RFID code is required")
    private String rfidCode;
    
    @NotBlank(message = "Image is required")
    private String imageBase64;
}
