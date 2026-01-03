package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.EntryLogDto;
import com.smart_parking_system.backend.entity.EntryLog;
import com.smart_parking_system.backend.entity.User;
import com.smart_parking_system.backend.repository.EntryLogRepository;
import com.smart_parking_system.backend.repository.UserParkingSpaceRepository;
import com.smart_parking_system.backend.repository.UserRepository;
import com.smart_parking_system.backend.service.IEntryLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EntryLogServiceImpl implements IEntryLogService {

    private final EntryLogRepository entryLogRepository;
    private final UserParkingSpaceRepository userParkingSpaceRepository;
    private final UserRepository userRepository;

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
