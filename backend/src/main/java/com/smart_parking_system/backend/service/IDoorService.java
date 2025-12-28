package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.CreateDoorRequestDto;
import com.smart_parking_system.backend.dto.DoorDto;
import com.smart_parking_system.backend.dto.UpdateDoorRequestDto;

import java.util.List;

public interface IDoorService {

    DoorDto createDoor(CreateDoorRequestDto requestDto);

    DoorDto getDoorById(Integer id);

    List<DoorDto> getAllDoorsByMyParkingSpaces();

    DoorDto updateDoor(Integer id, UpdateDoorRequestDto requestDto);

    void deleteDoor(Integer id);
}

