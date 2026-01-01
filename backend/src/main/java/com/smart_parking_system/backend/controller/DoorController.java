package com.smart_parking_system.backend.controller;

import com.smart_parking_system.backend.dto.CreateDoorRequestDto;
import com.smart_parking_system.backend.dto.DoorDto;
import com.smart_parking_system.backend.dto.UpdateDoorRequestDto;
import com.smart_parking_system.backend.service.IDoorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doors")
@RequiredArgsConstructor
public class DoorController {

    private final IDoorService doorService;

    @PostMapping
    public ResponseEntity<DoorDto> createDoor(@Valid @RequestBody CreateDoorRequestDto requestDto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(doorService.createDoor(requestDto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoorDto> getDoorById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(doorService.getDoorById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<DoorDto>> getAllDoorsByMyParkingSpaces() {
        try {
            return ResponseEntity.ok(doorService.getAllDoorsByMyParkingSpaces());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DoorDto> updateDoor(@PathVariable Integer id, @Valid @RequestBody UpdateDoorRequestDto requestDto) {
        try {
            return ResponseEntity.ok(doorService.updateDoor(id, requestDto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDoor(@PathVariable Integer id) {
        try {
            doorService.deleteDoor(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}



