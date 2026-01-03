package com.smart_parking_system.backend.controller;

import com.smart_parking_system.backend.dto.CreateMicrocontrollerRequestDto;
import com.smart_parking_system.backend.dto.MicrocontrollerDto;
import com.smart_parking_system.backend.dto.UpdateMicrocontrollerRequestDto;
import com.smart_parking_system.backend.service.IMicrocontrollerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/microcontrollers")
@RequiredArgsConstructor
public class MicrocontrollerController {

    private final IMicrocontrollerService microcontrollerService;

    @PostMapping
    public ResponseEntity<?> createMicrocontroller(@Valid @RequestBody CreateMicrocontrollerRequestDto requestDto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(microcontrollerService.createMicrocontroller(requestDto));
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Bad Request";
            errorResponse.put("error", errorMessage);
            errorResponse.put("message", errorMessage);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error");
            errorResponse.put("message", e.getMessage() != null ? e.getMessage() : "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<MicrocontrollerDto> getMicrocontrollerById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(microcontrollerService.getMicrocontrollerById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<MicrocontrollerDto>> getAllMicrocontrollersByMyParkingSpaces() {
        try {
            return ResponseEntity.ok(microcontrollerService.getAllMicrocontrollersByMyParkingSpaces());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<MicrocontrollerDto> updateMicrocontroller(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateMicrocontrollerRequestDto requestDto
    ) {
        try {
            return ResponseEntity.ok(microcontrollerService.updateMicrocontroller(id, requestDto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMicrocontroller(@PathVariable Integer id) {
        try {
            microcontrollerService.deleteMicrocontroller(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

