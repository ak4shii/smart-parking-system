package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.EntryLogDto;
import com.smart_parking_system.backend.entity.EntryLog;
import com.smart_parking_system.backend.entity.Microcontroller;
import com.smart_parking_system.backend.entity.Rfid;
import com.smart_parking_system.backend.repository.EntryLogRepository;
import com.smart_parking_system.backend.repository.MicrocontrollerRepository;
import com.smart_parking_system.backend.repository.RfidRepository;
import com.smart_parking_system.backend.service.IMqttEntryLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MqttEntryLogServiceImpl implements IMqttEntryLogService {

    private final EntryLogRepository entryLogRepository;
    private final RfidRepository rfidRepository;
    private final MicrocontrollerRepository microcontrollerRepository;

    @Override
    @Transactional
    public EntryLogDto handleEntry(String mcCode, String rfidCode, String licensePlate) {
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

        EntryLog saved = entryLogRepository.save(active);
        entryLogRepository.flush();

        return toDto(saved);
    }

    private EntryLogDto toDto(EntryLog el) {
        EntryLogDto dto = new EntryLogDto();
        dto.setId(el.getId());
        dto.setLicensePlate(el.getLicensePlate());
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
