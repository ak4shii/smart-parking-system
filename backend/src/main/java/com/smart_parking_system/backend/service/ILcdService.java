package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.CreateLcdRequestDto;
import com.smart_parking_system.backend.dto.LcdDto;
import com.smart_parking_system.backend.dto.UpdateLcdRequestDto;

import java.util.List;

public interface ILcdService {

    LcdDto createLcd(CreateLcdRequestDto requestDto);

    LcdDto getLcdById(Integer id);

    List<LcdDto> getAllLcdsByMyParkingSpaces();

    LcdDto updateLcd(Integer id, UpdateLcdRequestDto requestDto);

    void deleteLcd(Integer id);
}



