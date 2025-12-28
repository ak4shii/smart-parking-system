package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.CreateSlotRequestDto;
import com.smart_parking_system.backend.dto.SlotDto;

import java.util.List;

public interface ISlotService {

    SlotDto createSlot(CreateSlotRequestDto requestDto);

    SlotDto getSlotById(Integer id);

    List<SlotDto> getAllSlotsByMyParkingSpaces();

    void deleteSlot(Integer id);
}





