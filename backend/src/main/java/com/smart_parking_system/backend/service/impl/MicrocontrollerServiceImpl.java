package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.CreateMicrocontrollerRequestDto;
import com.smart_parking_system.backend.dto.MicrocontrollerDto;
import com.smart_parking_system.backend.dto.MqttCredentialsResponseDto;
import com.smart_parking_system.backend.dto.UpdateMicrocontrollerRequestDto;
import com.smart_parking_system.backend.entity.Microcontroller;
import com.smart_parking_system.backend.entity.ParkingSpace;
import com.smart_parking_system.backend.entity.User;
import com.smart_parking_system.backend.repository.MicrocontrollerRepository;
import com.smart_parking_system.backend.repository.ParkingSpaceRepository;
import com.smart_parking_system.backend.repository.UserParkingSpaceRepository;
import com.smart_parking_system.backend.repository.UserRepository;
import com.smart_parking_system.backend.service.IMicrocontrollerService;
import com.smart_parking_system.backend.service.IMqttCredentialService;
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
    private final IMqttCredentialService mqttCredentialService;

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

        Microcontroller mc = new Microcontroller();
        mc.setMcCode(requestDto.getMcCode());
        mc.setName(requestDto.getName());
        mc.setOnline(false);
        mc.setPs(ps);

        Microcontroller saved = microcontrollerRepository.save(mc);
        microcontrollerRepository.flush();

        // Generate MQTT credentials for the new device
        MqttCredentialsResponseDto mqttCredentials = mqttCredentialService.generateCredentials(saved, currentUser.getUsername());

        log.info("Microcontroller created: {} with MQTT username: {}. ESP32 can connect using provided credentials.", 
                requestDto.getMcCode(), mqttCredentials.getMqttUsername());

        MicrocontrollerDto dto = toDto(saved);
        // Include MQTT credentials in response (one-time display)
        dto.setMqttCredentials(mqttCredentials);
        
        return dto;
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

        // Revoke MQTT credentials before deleting
        mqttCredentialService.revokeCredentials(mc);

        microcontrollerRepository.delete(mc);
        log.info("Microcontroller deleted: {} and MQTT credentials revoked", mc.getMcCode());
    }

    /**
     * Regenerate MQTT credentials for a device.
     * Useful when credentials are compromised.
     */
    @Transactional
    public MqttCredentialsResponseDto regenerateMqttCredentials(Integer id) {
        User currentUser = getCurrentUser();

        Microcontroller mc = microcontrollerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Microcontroller not found with id: " + id));

        requireMembership(currentUser.getId(), mc.getPs().getId());

        MqttCredentialsResponseDto newCredentials = mqttCredentialService.regenerateCredentials(mc, currentUser.getUsername());
        
        log.info("MQTT credentials regenerated for device: {}", mc.getMcCode());
        
        return newCredentials;
    }

    /**
     * Revoke MQTT access for a device without deleting it.
     */
    @Transactional
    public void revokeMqttCredentials(Integer id) {
        User currentUser = getCurrentUser();

        Microcontroller mc = microcontrollerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Microcontroller not found with id: " + id));

        requireMembership(currentUser.getId(), mc.getPs().getId());

        mqttCredentialService.revokeCredentials(mc);
        
        log.info("MQTT credentials revoked for device: {}", mc.getMcCode());
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
        dto.setMqttUsername(mc.getMqttUsername());
        dto.setMqttEnabled(mc.getMqttEnabled());
        return dto;
    }
}
