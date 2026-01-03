package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.EntryLogDto;

import java.util.List;

public interface IEntryLogService {

    EntryLogDto getEntryLogById(Integer id);

    List<EntryLogDto> getEntryLogsByParkingSpace(Integer parkingSpaceId);
}
