package com.smart_parking_system.backend.controller;

import com.smart_parking_system.backend.dto.DoorDto;
import com.smart_parking_system.backend.service.IDoorService;
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
}
