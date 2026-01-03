package com.smart_parking_system.backend.controller;

import com.smart_parking_system.backend.dto.S3PresignRequestDto;
import com.smart_parking_system.backend.dto.S3PresignResponseDto;
import com.smart_parking_system.backend.service.IS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final IS3Service s3Service;

    @PostMapping("/presign-get")
    public ResponseEntity<S3PresignResponseDto> presignGet(@RequestBody S3PresignRequestDto req) {
        String url = s3Service.presignGetUrl(req.getKey());
        return ResponseEntity.ok(new S3PresignResponseDto(url));
    }
}

