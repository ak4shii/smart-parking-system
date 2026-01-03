package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.CreateMicrocontrollerRequestDto;
import com.smart_parking_system.backend.dto.MicrocontrollerDto;
import com.smart_parking_system.backend.dto.UpdateMicrocontrollerRequestDto;
import com.smart_parking_system.backend.dto.mqtt.MqttProvisionRequestDto;
import com.smart_parking_system.backend.entity.Microcontroller;
import com.smart_parking_system.backend.entity.ParkingSpace;
import com.smart_parking_system.backend.entity.User;
import com.smart_parking_system.backend.repository.MicrocontrollerRepository;
import com.smart_parking_system.backend.repository.ParkingSpaceRepository;
import com.smart_parking_system.backend.repository.UserParkingSpaceRepository;
import com.smart_parking_system.backend.repository.UserRepository;
import com.smart_parking_system.backend.service.IMicrocontrollerService;
import com.smart_parking_system.backend.service.IMqttProvisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MicrocontrollerServiceImpl implements IMicrocontrollerService {

    private final MicrocontrollerRepository microcontrollerRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final UserParkingSpaceRepository userParkingSpaceRepository;
    private final UserRepository userRepository;
    private final IMqttProvisionService mqttProvisionService;

    @Override
    @Transactional
    public MicrocontrollerDto createMicrocontroller(CreateMicrocontrollerRequestDto requestDto) {
        User currentUser = getCurrentUser();

        ParkingSpace ps = parkingSpaceRepository.findById(requestDto.getParkingSpaceId())
                .orElseThrow(() -> new RuntimeException(
                        "Parking space not found with id: " + requestDto.getParkingSpaceId()));

        requireMembership(currentUser.getId(), ps.getId());

        microcontrollerRepository.findByMcCode(requestDto.getMcCode()).ifPresent(mc -> {
            throw new RuntimeException("mcCode already exists");
        });

        MqttProvisionRequestDto provisionData = mqttProvisionService.checkForProvisionData(
                requestDto.getMcCode(),
                10);

        if (provisionData == null) {
            throw new RuntimeException("No provision data found for mcCode: " + requestDto.getMcCode() +
                    ". ESP32 must send provision request first.");
        }

        Microcontroller mc = new Microcontroller();
        mc.setMcCode(requestDto.getMcCode());
        mc.setName(requestDto.getName());
        mc.setOnline(false);
        mc.setPs(ps);

        Microcontroller saved = microcontrollerRepository.save(mc);
        microcontrollerRepository.flush();

        try {
            mqttProvisionService.handleProvision(requestDto.getMcCode(), provisionData);
        } catch (Exception e) {
            log.error("Failed to provision components for mcCode: {}", requestDto.getMcCode(), e);
        }

        return toDto(saved);
    }

    @Override
    public MicrocontrollerDto getMicrocontrollerById(Integer id) {
        User currentUser = getCurrentUser();

        Microcontroller mc = microcontrollerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Microcontroller not found with id: " + id));

        requireMembership(currentUser.getId(), mc.getPs().getId());

        return toDto(mc);
    }

    @Override
    public List<MicrocontrollerDto> getAllMicrocontrollersByMyParkingSpaces() {
        User currentUser = getCurrentUser();

        List<Integer> psIds = userParkingSpaceRepository.findParkingSpaceIdsByUserId(currentUser.getId());
        if (psIds.isEmpty())
            return List.of();

        return microcontrollerRepository.findAll().stream()
                .filter(mc -> mc.getPs() != null && psIds.contains(mc.getPs().getId()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MicrocontrollerDto updateMicrocontroller(Integer id, UpdateMicrocontrollerRequestDto requestDto) {
        User currentUser = getCurrentUser();

        Microcontroller mc = microcontrollerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Microcontroller not found with id: " + id));

        requireMembership(currentUser.getId(), mc.getPs().getId());

        mc.setName(requestDto.getName());

        Microcontroller saved = microcontrollerRepository.save(mc);
        microcontrollerRepository.flush();

        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteMicrocontroller(Integer id) {
        User currentUser = getCurrentUser();

        Microcontroller mc = microcontrollerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Microcontroller not found with id: " + id));

        requireMembership(currentUser.getId(), mc.getPs().getId());

        microcontrollerRepository.delete(mc);
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

    private MicrocontrollerDto toDto(Microcontroller mc) {
        MicrocontrollerDto dto = new MicrocontrollerDto();
        BeanUtils.copyProperties(mc, dto);
        dto.setId(mc.getId());
        if (mc.getPs() != null) {
            dto.setParkingSpaceId(mc.getPs().getId());
        }
        return dto;
    }
}
