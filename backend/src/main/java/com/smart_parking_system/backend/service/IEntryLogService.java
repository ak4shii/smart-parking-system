package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.CreateEntryRequestDto;
import com.smart_parking_system.backend.dto.EntryLogDto;
import com.smart_parking_system.backend.dto.ExitRequestDto;

import java.util.List;

public interface IEntryLogService {

    EntryLogDto createEntry(CreateEntryRequestDto requestDto);

    EntryLogDto exit(ExitRequestDto requestDto);

    EntryLogDto getEntryLogById(Integer id);

    List<EntryLogDto> getEntryLogsByParkingSpace(Integer parkingSpaceId);
}
