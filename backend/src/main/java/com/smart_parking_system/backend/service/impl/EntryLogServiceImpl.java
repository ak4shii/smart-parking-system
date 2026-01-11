package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.EntryLogDto;
import com.smart_parking_system.backend.entity.EntryLog;
import com.smart_parking_system.backend.entity.Microcontroller;
import com.smart_parking_system.backend.entity.Rfid;
import com.smart_parking_system.backend.entity.User;
import com.smart_parking_system.backend.repository.EntryLogRepository;
import com.smart_parking_system.backend.repository.MicrocontrollerRepository;
import com.smart_parking_system.backend.repository.RfidRepository;
import com.smart_parking_system.backend.repository.UserParkingSpaceRepository;
import com.smart_parking_system.backend.repository.UserRepository;
import com.smart_parking_system.backend.service.IEntryLogService;
import com.smart_parking_system.backend.service.IYoloService;
import com.smart_parking_system.backend.service.realtime.RealtimeEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntryLogServiceImpl implements IEntryLogService {

    private final EntryLogRepository entryLogRepository;
    private final UserParkingSpaceRepository userParkingSpaceRepository;
    private final UserRepository userRepository;
    private final MicrocontrollerRepository microcontrollerRepository;
    private final RfidRepository rfidRepository;
    private final IYoloService yoloService;
    private final RealtimeEventPublisher eventPublisher;

    @Override
    public EntryLogDto getEntryLogById(Integer id) {
        User currentUser = getCurrentUser();

        EntryLog el = entryLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry log not found"));

        Integer psId = el.getRfid().getPs().getId();
        requireMembership(currentUser.getId(), psId);

        return toDto(el);
    }

    @Override
    public List<EntryLogDto> getEntryLogsByParkingSpace(Integer parkingSpaceId) {
        User currentUser = getCurrentUser();
        requireMembership(currentUser.getId(), parkingSpaceId);

        return entryLogRepository.findAllByParkingSpaceId(parkingSpaceId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EntryLogDto handleEntry(String mcCode, String rfidCode, String imageBase64) {
        Microcontroller mc = microcontrollerRepository.findByMcCode(mcCode)
                .orElseThrow(() -> new RuntimeException("Microcontroller not found: " + mcCode));

        Rfid rfid = rfidRepository.findByRfidCode(rfidCode)
                .orElseThrow(() -> new RuntimeException("RFID not found"));

        if (!rfid.getPs().getId().equals(mc.getPs().getId())) {
            throw new RuntimeException("RFID not usable in this parking space");
        }

        if (Boolean.TRUE.equals(rfid.getCurrentlyUsed())) {
            throw new RuntimeException("This RFID is currently used");
        }

        entryLogRepository.findActiveByRfidId(rfid.getId()).ifPresent(active -> {
            throw new RuntimeException("This RFID already has an active entry log");
        });

        String licensePlate = yoloService.detectLicensePlate(imageBase64);

        if (licensePlate == null || licensePlate.trim().isEmpty()) {
            log.warn("Failed to detect license plate for RFID: {}", rfidCode);
            licensePlate = "UNKNOWN";
        }

        EntryLog entryLog = new EntryLog();
        entryLog.setRfid(rfid);
        entryLog.setLicensePlate(licensePlate);
        entryLog.setInTime(Instant.now());
        entryLog.setOutTime(null);

        rfid.setCurrentlyUsed(true);
        rfidRepository.save(rfid);

        EntryLog saved = entryLogRepository.save(entryLog);
        entryLogRepository.flush();

        return toDto(saved);
    }

    @Override
    @Transactional
    public EntryLogDto handleExit(String mcCode, String rfidCode) {
        Microcontroller mc = microcontrollerRepository.findByMcCode(mcCode)
                .orElseThrow(() -> new RuntimeException("Microcontroller not found: " + mcCode));

        Rfid rfid = rfidRepository.findByRfidCode(rfidCode)
                .orElseThrow(() -> new RuntimeException("RFID not found"));

        if (!rfid.getPs().getId().equals(mc.getPs().getId())) {
            throw new RuntimeException("RFID not usable in this parking space");
        }

        EntryLog active = entryLogRepository.findActiveByRfidId(rfid.getId())
                .orElseThrow(() -> new RuntimeException("No active entry log for this RFID"));

        active.setOutTime(Instant.now());

        rfid.setCurrentlyUsed(false);
        rfidRepository.save(rfid);

        // Broadcast RFID status change via WebSocket
        eventPublisher.publishRfidChanged(rfid.getId(), rfid.getRfidCode(), false, rfid.getPs().getId());

        EntryLog saved = entryLogRepository.save(active);
        entryLogRepository.flush();

        // Broadcast entry log event via WebSocket
        eventPublisher.publishVehicleExited(saved.getId(), saved.getLicensePlate(), rfidCode, rfid.getPs().getId());

        return toDto(saved);
    }

    @Override
    @Transactional
    public EntryLogDto createPendingEntry(String mcCode, String rfidCode) {
        Microcontroller mc = microcontrollerRepository.findByMcCode(mcCode)
                .orElseThrow(() -> new RuntimeException("Microcontroller not found: " + mcCode));

        Rfid rfid = rfidRepository.findByRfidCode(rfidCode)
                .orElseThrow(() -> new RuntimeException("RFID not found"));

        if (!rfid.getPs().getId().equals(mc.getPs().getId())) {
            throw new RuntimeException("RFID not usable in this parking space");
        }

        if (Boolean.TRUE.equals(rfid.getCurrentlyUsed())) {
            throw new RuntimeException("This RFID is currently used");
        }

        entryLogRepository.findActiveByRfidId(rfid.getId()).ifPresent(active -> {
            throw new RuntimeException("This RFID already has an active entry log");
        });

        EntryLog entryLog = new EntryLog();
        entryLog.setRfid(rfid);
        entryLog.setLicensePlate(null);
        entryLog.setLicensePlateImageKey(null);
        entryLog.setInTime(Instant.now());
        entryLog.setOutTime(null);

        rfid.setCurrentlyUsed(true);
        rfidRepository.save(rfid);

        // Broadcast RFID status change via WebSocket
        eventPublisher.publishRfidChanged(rfid.getId(), rfid.getRfidCode(), true, rfid.getPs().getId());

        EntryLog saved = entryLogRepository.save(entryLog);
        entryLogRepository.flush();

        // Broadcast entry log event via WebSocket
        eventPublisher.publishVehicleEntered(saved.getId(), saved.getLicensePlate(), rfidCode, rfid.getPs().getId());

        return toDto(saved);
    }

    @Override
    @Transactional
    public EntryLogDto updateEntryWithImage(String rfidCode, String imageBase64) {
        Rfid rfid = rfidRepository.findByRfidCode(rfidCode)
                .orElseThrow(() -> new RuntimeException("RFID not found"));

        EntryLog entryLog = entryLogRepository.findActiveByRfidId(rfid.getId())
                .orElseThrow(() -> new RuntimeException("No pending entry log for this RFID"));

        String licensePlate = yoloService.detectLicensePlate(imageBase64);

        if (licensePlate == null || licensePlate.trim().isEmpty()) {
            log.warn("Failed to detect license plate for RFID: {}", rfidCode);
            licensePlate = "UNKNOWN";
        }

        entryLog.setLicensePlate(licensePlate);

        EntryLog saved = entryLogRepository.save(entryLog);
        entryLogRepository.flush();

        return toDto(saved);
    }

    private void requireMembership(Integer userId, Integer parkingSpaceId) {
        if (userParkingSpaceRepository.findByUserIdAndPsId(userId, parkingSpaceId).isEmpty()) {
            throw new RuntimeException("Forbidden");
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private EntryLogDto toDto(EntryLog el) {
        EntryLogDto dto = new EntryLogDto();
        dto.setId(el.getId());
        dto.setLicensePlate(el.getLicensePlate());
        dto.setLicensePlateImageKey(el.getLicensePlateImageKey());
        dto.setInTime(el.getInTime());
        dto.setOutTime(el.getOutTime());

        if (el.getRfid() != null) {
            dto.setRfidId(el.getRfid().getId());
            dto.setRfidCode(el.getRfid().getRfidCode());
            if (el.getRfid().getPs() != null) {
                dto.setParkingSpaceId(el.getRfid().getPs().getId());
            }
        }

        return dto;
    }
}
