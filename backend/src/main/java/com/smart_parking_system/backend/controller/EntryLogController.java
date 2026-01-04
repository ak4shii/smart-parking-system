package com.smart_parking_system.backend.controller;

import com.smart_parking_system.backend.dto.EntryLogDto;
import com.smart_parking_system.backend.dto.EntryResponseDto;
import com.smart_parking_system.backend.dto.ExitResponseDto;
import com.smart_parking_system.backend.service.IEntryLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;

@Slf4j
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

    @PostMapping("/entry")
    public ResponseEntity<EntryResponseDto> handleEntry(
            @RequestParam("mcCode") String mcCode,
            @RequestParam("rfidCode") String rfidCode,
            @RequestParam("image") MultipartFile image) {
        try {
            byte[] imageBytes = image.getBytes();
            String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

            EntryLogDto entryLog = entryLogService.handleEntry(mcCode, rfidCode, imageBase64);

            EntryResponseDto response = new EntryResponseDto(true, "Entry recorded successfully", entryLog.getId());

            log.info("Entry recorded for mcCode: {}, rfidCode: {}, entryLogId: {}", mcCode, rfidCode, entryLog.getId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error handling entry for mcCode: {}, rfidCode: {}", mcCode, rfidCode, e);
            EntryResponseDto errorResponse = new EntryResponseDto(false, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error handling entry for mcCode: {}, rfidCode: {}", mcCode, rfidCode, e);
            EntryResponseDto errorResponse = new EntryResponseDto(false, "Internal server error", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/exit")
    public ResponseEntity<ExitResponseDto> handleExit(
            @RequestParam("mcCode") String mcCode,
            @RequestParam("rfidCode") String rfidCode) {
        try {
            EntryLogDto entryLog = entryLogService.handleExit(mcCode, rfidCode);

            ExitResponseDto response = new ExitResponseDto(true, "Exit recorded successfully", entryLog.getId());

            log.info("Exit recorded for mcCode: {}, rfidCode: {}, entryLogId: {}", mcCode, rfidCode, entryLog.getId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error handling exit for mcCode: {}, rfidCode: {}", mcCode, rfidCode, e);
            ExitResponseDto errorResponse = new ExitResponseDto(false, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error handling exit for mcCode: {}, rfidCode: {}", mcCode, rfidCode, e);
            ExitResponseDto errorResponse = new ExitResponseDto(false, "Internal server error", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
