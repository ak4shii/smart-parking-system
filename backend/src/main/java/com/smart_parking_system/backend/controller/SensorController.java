package com.smart_parking_system.backend.controller;

import com.smart_parking_system.backend.dto.CreateSensorRequestDto;
import com.smart_parking_system.backend.dto.SensorDto;
import com.smart_parking_system.backend.dto.UpdateSensorRequestDto;
import com.smart_parking_system.backend.service.ISensorService;
import jakarta.validation.Valid;
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

    @PostMapping
    public ResponseEntity<SensorDto> createSensor(@Valid @RequestBody CreateSensorRequestDto requestDto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(sensorService.createSensor(requestDto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

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

    @PutMapping("/{id}")
    public ResponseEntity<SensorDto> updateSensor(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateSensorRequestDto requestDto
    ) {
        try {
            return ResponseEntity.ok(sensorService.updateSensor(id, requestDto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSensor(@PathVariable Integer id) {
        try {
            sensorService.deleteSensor(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}







