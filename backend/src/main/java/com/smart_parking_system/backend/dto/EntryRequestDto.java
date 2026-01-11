package com.smart_parking_system.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntryRequestDto {

    @NotBlank
    private String mcCode;

    @NotBlank
    private String rfidCode;

    private MultipartFile image;
}
