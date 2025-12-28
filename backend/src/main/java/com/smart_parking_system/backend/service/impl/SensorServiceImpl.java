package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.CreateSensorRequestDto;
import com.smart_parking_system.backend.dto.SensorDto;
import com.smart_parking_system.backend.dto.UpdateSensorRequestDto;
import com.smart_parking_system.backend.entity.Microcontroller;
import com.smart_parking_system.backend.entity.Sensor;
import com.smart_parking_system.backend.entity.Slot;
import com.smart_parking_system.backend.entity.User;
import com.smart_parking_system.backend.repository.MicrocontrollerRepository;
import com.smart_parking_system.backend.repository.SensorRepository;
import com.smart_parking_system.backend.repository.SlotRepository;
import com.smart_parking_system.backend.repository.UserParkingSpaceRepository;
import com.smart_parking_system.backend.repository.UserRepository;
import com.smart_parking_system.backend.service.ISensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SensorServiceImpl implements ISensorService {

    private final SensorRepository sensorRepository;
    private final SlotRepository slotRepository;
    private final MicrocontrollerRepository microcontrollerRepository;
    private final UserParkingSpaceRepository userParkingSpaceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SensorDto createSensor(CreateSensorRequestDto requestDto) {
        User currentUser = getCurrentUser();

        Slot slot = slotRepository.findById(requestDto.getSlotId())
                .orElseThrow(() -> new RuntimeException("Slot not found with id: " + requestDto.getSlotId()));

        Microcontroller mc = microcontrollerRepository.findById(requestDto.getMicrocontrollerId())
                .orElseThrow(() -> new RuntimeException("Microcontroller not found with id: " + requestDto.getMicrocontrollerId()));

        Integer psIdFromSlot = slot.getPs().getId();
        Integer psIdFromMc = mc.getPs().getId();
        
        if (!psIdFromSlot.equals(psIdFromMc)) {
            throw new RuntimeException("Slot and Microcontroller must belong to the same parking space");
        }

        requireMembership(currentUser.getId(), psIdFromSlot);

        Sensor sensor = new Sensor();
        sensor.setName(requestDto.getName());
        sensor.setType(requestDto.getType());
        sensor.setSlot(slot);
        sensor.setMc(mc);

        Sensor saved = sensorRepository.save(sensor);
        sensorRepository.flush();

        return toDto(saved);
    }

    @Override
    public SensorDto getSensorById(Integer id) {
        User currentUser = getCurrentUser();

        Sensor sensor = sensorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sensor not found with id: " + id));

        Integer psId = sensor.getSlot().getPs().getId();
        requireMembership(currentUser.getId(), psId);

        return toDto(sensor);
    }

    @Override
    public List<SensorDto> getAllSensorsByMyParkingSpaces() {
        User currentUser = getCurrentUser();
        List<Integer> psIds = userParkingSpaceRepository.findParkingSpaceIdsByUserId(currentUser.getId());
        if (psIds.isEmpty()) {
            return List.of();
        }

        return sensorRepository.findAll().stream()
                .filter(s -> s.getSlot() != null && s.getSlot().getPs() != null && psIds.contains(s.getSlot().getPs().getId()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SensorDto updateSensor(Integer id, UpdateSensorRequestDto requestDto) {
        User currentUser = getCurrentUser();

        Sensor sensor = sensorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sensor not found with id: " + id));

        Integer psId = sensor.getSlot().getPs().getId();
        requireMembership(currentUser.getId(), psId);

        sensor.setName(requestDto.getName());
        sensor.setType(requestDto.getType());

        Sensor saved = sensorRepository.save(sensor);
        sensorRepository.flush();

        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteSensor(Integer id) {
        User currentUser = getCurrentUser();

        Sensor sensor = sensorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sensor not found with id: " + id));

        Integer psId = sensor.getSlot().getPs().getId();
        requireMembership(currentUser.getId(), psId);

        sensorRepository.delete(sensor);
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

    private SensorDto toDto(Sensor sensor) {
        SensorDto dto = new SensorDto();
        dto.setId(sensor.getId());
        dto.setName(sensor.getName());
        dto.setType(sensor.getType());
        if (sensor.getSlot() != null) {
            dto.setSlotId(sensor.getSlot().getId());
            if (sensor.getSlot().getPs() != null) {
                dto.setParkingSpaceId(sensor.getSlot().getPs().getId());
            }
        }
        if (sensor.getMc() != null) {
            dto.setMicrocontrollerId(sensor.getMc().getId());
        }
        return dto;
    }
}







