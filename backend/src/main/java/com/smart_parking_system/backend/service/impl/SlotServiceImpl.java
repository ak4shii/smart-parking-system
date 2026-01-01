package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.CreateSlotRequestDto;
import com.smart_parking_system.backend.dto.SlotDto;
import com.smart_parking_system.backend.entity.ParkingSpace;
import com.smart_parking_system.backend.entity.Slot;
import com.smart_parking_system.backend.entity.User;
import com.smart_parking_system.backend.repository.ParkingSpaceRepository;
import com.smart_parking_system.backend.repository.SlotRepository;
import com.smart_parking_system.backend.repository.UserParkingSpaceRepository;
import com.smart_parking_system.backend.repository.UserRepository;
import com.smart_parking_system.backend.service.ISlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smart_parking_system.backend.service.realtime.RealtimeEventPublisher;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotServiceImpl implements ISlotService {

    private final SlotRepository slotRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final UserParkingSpaceRepository userParkingSpaceRepository;
    private final UserRepository userRepository;
    private final RealtimeEventPublisher eventPublisher;

    @Override
    @Transactional
    public SlotDto createSlot(CreateSlotRequestDto requestDto) {
        User currentUser = getCurrentUser();

        ParkingSpace ps = parkingSpaceRepository.findById(requestDto.getParkingSpaceId())
                .orElseThrow(() -> new RuntimeException("Parking space not found with id: " + requestDto.getParkingSpaceId()));

        requireMembership(currentUser.getId(), ps.getId());

        if (slotRepository.findByName(requestDto.getName()).isPresent()) {
            throw new RuntimeException("Slot with name '" + requestDto.getName() + "' already exists");
        }

        Slot slot = new Slot();
        slot.setName(requestDto.getName());
        slot.setPs(ps);
        slot.setIsOccupied(false);

        Slot saved = slotRepository.save(slot);
        slotRepository.flush();

        return toDto(saved);
    }

    @Override
    public SlotDto getSlotById(Integer id) {
        User currentUser = getCurrentUser();

        Slot slot = slotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found with id: " + id));

        requireMembership(currentUser.getId(), slot.getPs().getId());

        return toDto(slot);
    }

    @Override
    public List<SlotDto> getAllSlotsByMyParkingSpaces() {
        User currentUser = getCurrentUser();
        List<Integer> psIds = userParkingSpaceRepository.findParkingSpaceIdsByUserId(currentUser.getId());
        if (psIds.isEmpty()) {
            return List.of();
        }

        return slotRepository.findAll().stream()
                .filter(s -> s.getPs() != null && psIds.contains(s.getPs().getId()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSlot(Integer id) {
        User currentUser = getCurrentUser();

        Slot slot = slotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found with id: " + id));

        requireMembership(currentUser.getId(), slot.getPs().getId());

        slotRepository.delete(slot);
    }

    @Override
    @Transactional
    public SlotDto updateSlotStatus(Integer slotId, Boolean isOccupied) {
        User currentUser = getCurrentUser();

        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found with id: " + slotId));

        requireMembership(currentUser.getId(), slot.getPs().getId());

        slot.setIsOccupied(isOccupied);
        Slot saved = slotRepository.save(slot);
        slotRepository.flush();

        // Push real-time update to all subscribed clients
        eventPublisher.publishSlotChanged(saved.getId(), saved.getIsOccupied(), saved.getPs().getId());

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

    private SlotDto toDto(Slot slot) {
        SlotDto dto = new SlotDto();
        dto.setId(slot.getId());
        dto.setName(slot.getName());
        dto.setIsOccupied(slot.getIsOccupied());
        if (slot.getPs() != null) {
            dto.setParkingSpaceId(slot.getPs().getId());
        }
        return dto;
    }
}







