package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.EntryLogDto;
import com.smart_parking_system.backend.dto.mqtt.MqttEntryRequestDto;
import com.smart_parking_system.backend.dto.mqtt.MqttEntryResponseDto;
import com.smart_parking_system.backend.dto.mqtt.MqttExitRequestDto;
import com.smart_parking_system.backend.dto.mqtt.MqttExitResponseDto;
import com.smart_parking_system.backend.entity.EntryLog;
import com.smart_parking_system.backend.entity.Rfid;
import com.smart_parking_system.backend.entity.User;
import com.smart_parking_system.backend.repository.EntryLogRepository;
import com.smart_parking_system.backend.repository.RfidRepository;
import com.smart_parking_system.backend.repository.UserParkingSpaceRepository;
import com.smart_parking_system.backend.repository.UserRepository;
import com.smart_parking_system.backend.service.IEntryLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.smart_parking_system.backend.service.realtime.RealtimeEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EntryLogServiceImpl implements IEntryLogService {

    private final EntryLogRepository entryLogRepository;
    private final UserParkingSpaceRepository userParkingSpaceRepository;
    private final UserRepository userRepository;
    private final RfidRepository rfidRepository;
    private final RealtimeEventPublisher eventPublisher;

    @Override
    @Transactional
    public MqttEntryResponseDto createEntry(MqttEntryRequestDto requestDto) {
        User currentUser = getCurrentUser();

        Rfid rfid = rfidRepository.findByRfidCode(requestDto.getRfidCode())
                .orElseThrow(() -> new RuntimeException("RFID not found"));
      
        if (Boolean.TRUE.equals(rfid.getCurrentlyUsed())) {
            throw new RuntimeException("This RFID is currently used");
        }

        entryLogRepository.findActiveByRfidId(rfid.getId()).ifPresent(active -> {
            throw new RuntimeException("This RFID already has an active entry log");
        });

        Integer psId = rfid.getPs().getId();
        requireMembership(currentUser.getId(), psId);

        EntryLog entryLog = new EntryLog();
        entryLog.setRfid(rfid);
        entryLog.setLicensePlate(requestDto.getLicensePlate());
        entryLog.setInTime(Instant.now());
        entryLog.setOutTime(null);

        rfid.setCurrentlyUsed(true);
        rfidRepository.save(rfid);

        EntryLog saved = entryLogRepository.save(entryLog);
        entryLogRepository.flush();
      
        eventPublisher.publishVehicleEntered(saved.getId(), saved.getLicensePlate(), rfid.getRfidCode(), psId);

        MqttEntryResponseDto responseDto = new MqttEntryResponseDto();
        responseDto.setAllowed(true);
        responseDto.setMessage("Allowed");
        responseDto.setEntryLogId(saved.getId());
        return responseDto;
    }

    @Override
    @Transactional
    public MqttExitResponseDto exit(MqttExitRequestDto requestDto) {
        User currentUser = getCurrentUser();

        Rfid rfid = rfidRepository.findByRfidCode(requestDto.getRfidCode())
                .orElseThrow(() -> new RuntimeException("RFID not found"));
      
        EntryLog active = entryLogRepository.findActiveByRfidId(rfid.getId())
                .orElseThrow(() -> new RuntimeException("No active entry log for this RFID"));

        Integer psId = rfid.getPs().getId();
        requireMembership(currentUser.getId(), psId);

        active.setOutTime(Instant.now());

        rfid.setCurrentlyUsed(false);
        rfidRepository.save(rfid);

        EntryLog saved = entryLogRepository.save(active);
        entryLogRepository.flush();

        eventPublisher.publishVehicleExited(saved.getId(), saved.getLicensePlate(), rfid.getRfidCode(), psId);

        MqttExitResponseDto responseDto = new MqttExitResponseDto();
        responseDto.setEntryLogId(saved.getId());
        responseDto.setMessage("Exited");
        responseDto.setSuccess(true);

        return responseDto;
    }

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



