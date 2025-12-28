package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.CreateSensorRequestDto;
import com.smart_parking_system.backend.dto.SensorDto;
import com.smart_parking_system.backend.dto.UpdateSensorRequestDto;

import java.util.List;

public interface ISensorService {

    SensorDto createSensor(CreateSensorRequestDto requestDto);

    SensorDto getSensorById(Integer id);

    List<SensorDto> getAllSensorsByMyParkingSpaces();

    SensorDto updateSensor(Integer id, UpdateSensorRequestDto requestDto);

    void deleteSensor(Integer id);
}





