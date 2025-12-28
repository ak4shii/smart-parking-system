package com.smart_parking_system.backend.controller;

import com.smart_parking_system.backend.dto.CreateRfidRequestDto;
import com.smart_parking_system.backend.dto.RfidDto;
import com.smart_parking_system.backend.service.IRfidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rfids")
@RequiredArgsConstructor
public class RfidController {

    private final IRfidService rfidService;

    @PostMapping
    public ResponseEntity<RfidDto> createRfid(@Valid @RequestBody CreateRfidRequestDto requestDto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(rfidService.createRfid(requestDto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<RfidDto> getRfidById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(rfidService.getRfidById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<RfidDto>> getAllRfidsByMyParkingSpaces() {
        try {
            return ResponseEntity.ok(rfidService.getAllRfidsByMyParkingSpaces());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRfid(@PathVariable Integer id) {
        try {
            rfidService.deleteRfid(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}





