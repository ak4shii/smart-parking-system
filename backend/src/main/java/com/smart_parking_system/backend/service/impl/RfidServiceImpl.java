package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.CreateRfidRequestDto;
import com.smart_parking_system.backend.dto.RfidDto;
import com.smart_parking_system.backend.entity.ParkingSpace;
import com.smart_parking_system.backend.entity.Rfid;
import com.smart_parking_system.backend.entity.User;
import com.smart_parking_system.backend.repository.ParkingSpaceRepository;
import com.smart_parking_system.backend.repository.RfidRepository;
import com.smart_parking_system.backend.repository.UserParkingSpaceRepository;
import com.smart_parking_system.backend.repository.UserRepository;
import com.smart_parking_system.backend.service.IRfidService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RfidServiceImpl implements IRfidService {

    private final RfidRepository rfidRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final UserParkingSpaceRepository userParkingSpaceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public RfidDto createRfid(CreateRfidRequestDto requestDto) {
        User currentUser = getCurrentUser();

        ParkingSpace ps = parkingSpaceRepository.findById(requestDto.getParkingSpaceId())
                .orElseThrow(() -> new RuntimeException("Parking space not found with id: " + requestDto.getParkingSpaceId()));

        requireMembership(currentUser.getId(), ps.getId());

        rfidRepository.findByRfidCode(requestDto.getRfidCode()).ifPresent(r -> {
            throw new RuntimeException("rfidCode already exists");
        });

        Rfid rfid = new Rfid();
        rfid.setRfidCode(requestDto.getRfidCode());
        rfid.setPs(ps);
        rfid.setCurrentlyUsed(false);

        Rfid saved = rfidRepository.save(rfid);
        rfidRepository.flush();

        return toDto(saved);
    }

    @Override
    public RfidDto getRfidById(Integer id) {
        User currentUser = getCurrentUser();

        Rfid rfid = rfidRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RFID not found with id: " + id));

        requireMembership(currentUser.getId(), rfid.getPs().getId());

        return toDto(rfid);
    }

    @Override
    public List<RfidDto> getAllRfidsByMyParkingSpaces() {
        User currentUser = getCurrentUser();

        List<Integer> psIds = userParkingSpaceRepository.findParkingSpaceIdsByUserId(currentUser.getId());
        if (psIds.isEmpty()) {
            return List.of();
        }

        return rfidRepository.findAll().stream()
                .filter(r -> r.getPs() != null && psIds.contains(r.getPs().getId()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteRfid(Integer id) {
        User currentUser = getCurrentUser();

        Rfid rfid = rfidRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RFID not found with id: " + id));

        requireMembership(currentUser.getId(), rfid.getPs().getId());

        rfidRepository.delete(rfid);
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

    private RfidDto toDto(Rfid rfid) {
        RfidDto dto = new RfidDto();
        dto.setId(rfid.getId());
        dto.setRfidCode(rfid.getRfidCode());
        dto.setCurrentlyUsed(rfid.getCurrentlyUsed());
        if (rfid.getPs() != null) {
            dto.setParkingSpaceId(rfid.getPs().getId());
        }
        return dto;
    }
}

