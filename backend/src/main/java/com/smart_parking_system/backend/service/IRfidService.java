package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.CreateRfidRequestDto;
import com.smart_parking_system.backend.dto.RfidDto;

import java.util.List;

public interface IRfidService {

    RfidDto createRfid(CreateRfidRequestDto requestDto);

    RfidDto getRfidById(Integer id);

    List<RfidDto> getAllRfidsByMyParkingSpaces();

    void deleteRfid(Integer id);
}







