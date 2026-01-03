package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.YoloResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoloService {

    private final RestTemplate restTemplate;

    @Value("${yolo.server.url:http://100.121.164.7:8000}")
    private String yoloServerUrl;

    YoloResponseDto responseDto;

    public String detectLicensePlate(String imageBase64) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("image", imageBase64);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<YoloResponseDto> response = restTemplate.postForEntity(
                    yoloServerUrl,
                    request,
                    YoloResponseDto.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                YoloResponseDto body = response.getBody();
                String licensePlate = body.getPlate();

                if (licensePlate != null && !licensePlate.trim().isEmpty()) {
                    log.info("License plate detected: {}, status: {}", licensePlate, body.getStatus());
                    return licensePlate;
                } else {
                    log.warn("YOLO server returned empty license plate, status: {}", body.getStatus());
                    return null;
                }
            } else {
                log.error("YOLO server returned non-OK status: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            log.error("Failed to detect license plate via YOLO server", e);
            return null;
        }
    }
}
