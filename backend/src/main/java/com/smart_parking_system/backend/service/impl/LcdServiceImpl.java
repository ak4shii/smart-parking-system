package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.CreateLcdRequestDto;
import com.smart_parking_system.backend.dto.LcdDto;
import com.smart_parking_system.backend.dto.UpdateLcdRequestDto;
import com.smart_parking_system.backend.entity.Lcd;
import com.smart_parking_system.backend.entity.Microcontroller;
import com.smart_parking_system.backend.entity.User;
import com.smart_parking_system.backend.repository.LcdRepository;
import com.smart_parking_system.backend.repository.MicrocontrollerRepository;
import com.smart_parking_system.backend.repository.UserParkingSpaceRepository;
import com.smart_parking_system.backend.repository.UserRepository;
import com.smart_parking_system.backend.service.ILcdService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LcdServiceImpl implements ILcdService {

    private final LcdRepository lcdRepository;
    private final MicrocontrollerRepository microcontrollerRepository;
    private final UserParkingSpaceRepository userParkingSpaceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public LcdDto createLcd(CreateLcdRequestDto requestDto) {
        User currentUser = getCurrentUser();

        Microcontroller mc = microcontrollerRepository.findById(requestDto.getMicrocontrollerId())
                .orElseThrow(() -> new RuntimeException("Microcontroller not found with id: " + requestDto.getMicrocontrollerId()));

        requireMembership(currentUser.getId(), mc.getPs().getId());

        Lcd lcd = new Lcd();
        lcd.setName(requestDto.getName());
        lcd.setDisplayText(requestDto.getDisplayText());
        lcd.setMc(mc);

        Lcd saved = lcdRepository.save(lcd);
        lcdRepository.flush();

        return toDto(saved);
    }

    @Override
    public LcdDto getLcdById(Integer id) {
        User currentUser = getCurrentUser();

        Lcd lcd = lcdRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("LCD not found with id: " + id));

        requireMembership(currentUser.getId(), lcd.getMc().getPs().getId());

        return toDto(lcd);
    }

    @Override
    public List<LcdDto> getAllLcdsByMyParkingSpaces() {
        User currentUser = getCurrentUser();
        List<Integer> psIds = userParkingSpaceRepository.findParkingSpaceIdsByUserId(currentUser.getId());
        if (psIds.isEmpty()) {
            return List.of();
        }

        return lcdRepository.findAll().stream()
                .filter(l -> l.getMc() != null && l.getMc().getPs() != null && psIds.contains(l.getMc().getPs().getId()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LcdDto updateLcd(Integer id, UpdateLcdRequestDto requestDto) {
        User currentUser = getCurrentUser();

        Lcd lcd = lcdRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("LCD not found with id: " + id));

        requireMembership(currentUser.getId(), lcd.getMc().getPs().getId());

        if (requestDto.getName() != null) {
            lcd.setName(requestDto.getName());
        }
        if (requestDto.getDisplayText() != null) {
            lcd.setDisplayText(requestDto.getDisplayText());
        }

        Lcd saved = lcdRepository.save(lcd);
        lcdRepository.flush();

        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteLcd(Integer id) {
        User currentUser = getCurrentUser();

        Lcd lcd = lcdRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("LCD not found with id: " + id));

        requireMembership(currentUser.getId(), lcd.getMc().getPs().getId());

        lcdRepository.delete(lcd);
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

    private LcdDto toDto(Lcd lcd) {
        LcdDto dto = new LcdDto();
        dto.setId(lcd.getId());
        dto.setName(lcd.getName());
        dto.setDisplayText(lcd.getDisplayText());
        if (lcd.getMc() != null) {
            dto.setMicrocontrollerId(lcd.getMc().getId());
        }
        return dto;
    }
}

