package com.smart_parking_system.backend.controller;

import com.smart_parking_system.backend.dto.LcdDto;
import com.smart_parking_system.backend.service.ILcdService;
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
}
