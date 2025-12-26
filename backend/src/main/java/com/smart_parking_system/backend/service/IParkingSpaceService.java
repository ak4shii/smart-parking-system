package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.CreateParkingSpaceRequestDto;
import com.smart_parking_system.backend.dto.ParkingSpaceDto;
import com.smart_parking_system.backend.dto.UpdateParkingSpaceRequestDto;
import com.smart_parking_system.backend.dto.ParkingSpaceManagerDto;

import java.util.List;

public interface IParkingSpaceService {

    ParkingSpaceDto createParkingSpace(CreateParkingSpaceRequestDto requestDto);

    List<ParkingSpaceDto> getAllParkingSpaces();

    ParkingSpaceDto getParkingSpaceById(Integer id);

    ParkingSpaceDto updateParkingSpace(Integer id, UpdateParkingSpaceRequestDto requestDto);

    void deleteParkingSpace(Integer id);

    void addManager(Integer parkingSpaceId, String managerEmail);

    void removeManager(Integer parkingSpaceId, Integer managerUserId);

    void transferOwnership(Integer parkingSpaceId, Integer newOwnerUserId);

    List<ParkingSpaceManagerDto> getManagers(Integer parkingSpaceId);
}

