package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.CreateDoorRequestDto;
import com.smart_parking_system.backend.dto.DoorDto;
import com.smart_parking_system.backend.dto.UpdateDoorRequestDto;
import com.smart_parking_system.backend.entity.Door;
import com.smart_parking_system.backend.entity.Microcontroller;
import com.smart_parking_system.backend.entity.User;
import com.smart_parking_system.backend.repository.DoorRepository;
import com.smart_parking_system.backend.repository.MicrocontrollerRepository;
import com.smart_parking_system.backend.repository.UserParkingSpaceRepository;
import com.smart_parking_system.backend.repository.UserRepository;
import com.smart_parking_system.backend.service.IDoorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoorServiceImpl implements IDoorService {

    private final DoorRepository doorRepository;
    private final MicrocontrollerRepository microcontrollerRepository;
    private final UserParkingSpaceRepository userParkingSpaceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public DoorDto createDoor(CreateDoorRequestDto requestDto) {
        User currentUser = getCurrentUser();

        Microcontroller mc = microcontrollerRepository.findById(requestDto.getMicrocontrollerId())
                .orElseThrow(() -> new RuntimeException("Microcontroller not found with id: " + requestDto.getMicrocontrollerId()));

        requireMembership(currentUser.getId(), mc.getPs().getId());

        Door door = new Door();
        door.setName(requestDto.getName());
        door.setIsOpened(requestDto.getIsOpened() != null ? requestDto.getIsOpened() : Boolean.FALSE);
        door.setMc(mc);

        Door saved = doorRepository.save(door);
        doorRepository.flush();

        return toDto(saved);
    }

    @Override
    public DoorDto getDoorById(Integer id) {
        User currentUser = getCurrentUser();

        Door door = doorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Door not found with id: " + id));

        requireMembership(currentUser.getId(), door.getMc().getPs().getId());

        return toDto(door);
    }

    @Override
    public List<DoorDto> getAllDoorsByMyParkingSpaces() {
        User currentUser = getCurrentUser();
        List<Integer> psIds = userParkingSpaceRepository.findParkingSpaceIdsByUserId(currentUser.getId());
        if (psIds.isEmpty()) {
            return List.of();
        }

        return doorRepository.findAll().stream()
                .filter(d -> d.getMc() != null && d.getMc().getPs() != null && psIds.contains(d.getMc().getPs().getId()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DoorDto updateDoor(Integer id, UpdateDoorRequestDto requestDto) {
        User currentUser = getCurrentUser();

        Door door = doorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Door not found with id: " + id));

        requireMembership(currentUser.getId(), door.getMc().getPs().getId());

        if (requestDto.getName() != null) {
            door.setName(requestDto.getName());
        }
        if (requestDto.getIsOpened() != null) {
            door.setIsOpened(requestDto.getIsOpened());
        }

        Door saved = doorRepository.save(door);
        doorRepository.flush();

        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteDoor(Integer id) {
        User currentUser = getCurrentUser();

        Door door = doorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Door not found with id: " + id));

        requireMembership(currentUser.getId(), door.getMc().getPs().getId());

        doorRepository.delete(door);
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

    private DoorDto toDto(Door door) {
        DoorDto dto = new DoorDto();
        dto.setId(door.getId());
        dto.setName(door.getName());
        dto.setIsOpened(door.getIsOpened());
        if (door.getMc() != null) {
            dto.setMicrocontrollerId(door.getMc().getId());
        }
        return dto;
    }
}



