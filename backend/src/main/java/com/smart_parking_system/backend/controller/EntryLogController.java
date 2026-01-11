package com.smart_parking_system.backend.controller;

import com.smart_parking_system.backend.dto.EntryLogDto;
import com.smart_parking_system.backend.service.IEntryLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.Map;

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

    @PostMapping(value = "/upload-image", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadImage(
            @RequestParam("rfidCode") String rfidCode,
            @RequestPart("image") MultipartFile image) {
        try {
            byte[] imageBytes = image.getBytes();
            String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

            EntryLogDto entryLog = entryLogService.updateEntryWithImage(rfidCode, imageBase64);

            log.info("Image uploaded and processed for rfidCode: {}, entryLogId: {}, licensePlate: {}",
                    rfidCode, entryLog.getId(), entryLog.getLicensePlate());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Image processed successfully",
                    "licensePlate", entryLog.getLicensePlate(),
                    "entryLogId", entryLog.getId()));
        } catch (RuntimeException e) {
            log.error("Error uploading image for rfidCode: {}", rfidCode, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error uploading image for rfidCode: {}", rfidCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }
}
