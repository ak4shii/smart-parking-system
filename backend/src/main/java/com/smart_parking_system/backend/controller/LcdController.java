package com.smart_parking_system.backend.controller;

import com.smart_parking_system.backend.dto.CreateLcdRequestDto;
import com.smart_parking_system.backend.dto.LcdDto;
import com.smart_parking_system.backend.dto.UpdateLcdRequestDto;
import com.smart_parking_system.backend.service.ILcdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lcds")
@RequiredArgsConstructor
public class LcdController {

    private final ILcdService lcdService;

    @PostMapping
    public ResponseEntity<LcdDto> createLcd(@Valid @RequestBody CreateLcdRequestDto requestDto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(lcdService.createLcd(requestDto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<LcdDto> getLcdById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(lcdService.getLcdById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<LcdDto>> getAllLcdsByMyParkingSpaces() {
        try {
            return ResponseEntity.ok(lcdService.getAllLcdsByMyParkingSpaces());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<LcdDto> updateLcd(@PathVariable Integer id, @Valid @RequestBody UpdateLcdRequestDto requestDto) {
        try {
            return ResponseEntity.ok(lcdService.updateLcd(id, requestDto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLcd(@PathVariable Integer id) {
        try {
            lcdService.deleteLcd(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

