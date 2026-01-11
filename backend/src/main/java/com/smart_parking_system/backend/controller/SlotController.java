package com.smart_parking_system.backend.controller;

import com.smart_parking_system.backend.dto.SlotDto;
import com.smart_parking_system.backend.service.ISlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class SlotController {

    private final ISlotService slotService;

    @GetMapping("/{id}")
    public ResponseEntity<SlotDto> getSlotById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(slotService.getSlotById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<SlotDto>> getAllSlotsByMyParkingSpaces() {
        try {
            return ResponseEntity.ok(slotService.getAllSlotsByMyParkingSpaces());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
