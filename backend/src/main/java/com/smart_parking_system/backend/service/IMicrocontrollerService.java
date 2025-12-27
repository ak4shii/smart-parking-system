package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.CreateMicrocontrollerRequestDto;
import com.smart_parking_system.backend.dto.MicrocontrollerDto;
import com.smart_parking_system.backend.dto.UpdateMicrocontrollerRequestDto;

import java.util.List;

public interface IMicrocontrollerService {

    MicrocontrollerDto createMicrocontroller(CreateMicrocontrollerRequestDto requestDto);

    MicrocontrollerDto getMicrocontrollerById(Integer id);

    List<MicrocontrollerDto> getAllMicrocontrollersByMyParkingSpaces();

    MicrocontrollerDto updateMicrocontroller(Integer id, UpdateMicrocontrollerRequestDto requestDto);

    void deleteMicrocontroller(Integer id);
}


