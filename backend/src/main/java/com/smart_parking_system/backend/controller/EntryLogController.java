package com.smart_parking_system.backend.controller;

import com.smart_parking_system.backend.dto.EntryLogDto;
import com.smart_parking_system.backend.service.IEntryLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entry-logs")
@RequiredArgsConstructor
public class EntryLogController {

    private final IEntryLogService entryLogService;

    @GetMapping("/{id}")
    public ResponseEntity<EntryLogDto> getById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(entryLogService.getEntryLogById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<EntryLogDto>> getByParkingSpace(@RequestParam("parkingSpaceId") Integer parkingSpaceId) {
        try {
            return ResponseEntity.ok(entryLogService.getEntryLogsByParkingSpace(parkingSpaceId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}



