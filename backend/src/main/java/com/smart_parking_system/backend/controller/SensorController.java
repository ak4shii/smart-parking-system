package com.smart_parking_system.backend.controller;

import com.smart_parking_system.backend.dto.SensorDto;
import com.smart_parking_system.backend.service.ISensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
public class SensorController {

    private final ISensorService sensorService;

    @GetMapping("/{id}")
    public ResponseEntity<SensorDto> getSensorById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(sensorService.getSensorById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<SensorDto>> getAllSensorsByMyParkingSpaces() {
        try {
            return ResponseEntity.ok(sensorService.getAllSensorsByMyParkingSpaces());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
