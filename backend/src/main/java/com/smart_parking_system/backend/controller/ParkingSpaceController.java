package com.smart_parking_system.backend.controller;

import com.smart_parking_system.backend.dto.AddParkingSpaceManagerRequestDto;
import com.smart_parking_system.backend.dto.CreateParkingSpaceRequestDto;
import com.smart_parking_system.backend.dto.ParkingSpaceDto;
import com.smart_parking_system.backend.dto.UpdateParkingSpaceRequestDto;
import com.smart_parking_system.backend.dto.TransferParkingSpaceOwnershipRequestDto;
import com.smart_parking_system.backend.dto.ParkingSpaceManagerDto;
import com.smart_parking_system.backend.service.IParkingSpaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parking-spaces")
@RequiredArgsConstructor
public class ParkingSpaceController {

    private final IParkingSpaceService parkingSpaceService;

    @PostMapping
    public ResponseEntity<ParkingSpaceDto> createParkingSpace(
            @Valid @RequestBody CreateParkingSpaceRequestDto requestDto) {
        try {
            ParkingSpaceDto createdParkingSpace = parkingSpaceService.createParkingSpace(requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdParkingSpace);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParkingSpaceDto> getParkingSpaceById(@PathVariable Integer id) {
        try {
            ParkingSpaceDto parkingSpace = parkingSpaceService.getParkingSpaceById(id);
            return ResponseEntity.ok(parkingSpace);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<ParkingSpaceDto>> getAllParkingSpaces() {
        try {
            List<ParkingSpaceDto> parkingSpaces = parkingSpaceService.getAllParkingSpaces();
            return ResponseEntity.ok(parkingSpaces);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParkingSpaceDto> updateParkingSpace(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateParkingSpaceRequestDto requestDto) {
        try {
            ParkingSpaceDto updatedParkingSpace = parkingSpaceService.updateParkingSpace(id, requestDto);
            return ResponseEntity.ok(updatedParkingSpace);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParkingSpace(@PathVariable Integer id) {
        try {
            parkingSpaceService.deleteParkingSpace(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/managers")
    public ResponseEntity<List<ParkingSpaceManagerDto>> getManagers(@PathVariable("id") Integer parkingSpaceId) {
        try {
            return ResponseEntity.ok(parkingSpaceService.getManagers(parkingSpaceId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/managers")
    public ResponseEntity<Void> addManager(
            @PathVariable("id") Integer parkingSpaceId,
            @Valid @RequestBody AddParkingSpaceManagerRequestDto requestDto) {
        try {
            parkingSpaceService.addManager(parkingSpaceId, requestDto.getEmail());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}/managers/{userId}")
    public ResponseEntity<Void> removeManager(
            @PathVariable("id") Integer parkingSpaceId,
            @PathVariable("userId") Integer managerUserId) {
        try {
            parkingSpaceService.removeManager(parkingSpaceId, managerUserId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/transfer-ownership")
    public ResponseEntity<Void> transferOwnership(
            @PathVariable("id") Integer parkingSpaceId,
            @Valid @RequestBody TransferParkingSpaceOwnershipRequestDto requestDto) {
        try {
            parkingSpaceService.transferOwnership(parkingSpaceId, requestDto.getNewOwnerUserId());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
