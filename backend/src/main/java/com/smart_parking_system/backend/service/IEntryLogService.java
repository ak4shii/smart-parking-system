package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.EntryLogDto;
import com.smart_parking_system.backend.dto.mqtt.MqttEntryRequestDto;
import com.smart_parking_system.backend.dto.mqtt.MqttEntryResponseDto;
import com.smart_parking_system.backend.dto.mqtt.MqttExitRequestDto;
import com.smart_parking_system.backend.dto.mqtt.MqttExitResponseDto;

import java.util.List;

public interface IEntryLogService {

    MqttEntryResponseDto createEntry(MqttEntryRequestDto requestDto);

    MqttExitResponseDto exit(MqttExitRequestDto requestDto);

    EntryLogDto getEntryLogById(Integer id);

    List<EntryLogDto> getEntryLogsByParkingSpace(Integer parkingSpaceId);
}



