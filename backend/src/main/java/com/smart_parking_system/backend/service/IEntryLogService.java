package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.EntryLogDto;

import java.util.List;

public interface IEntryLogService {

    EntryLogDto getEntryLogById(Integer id);

    List<EntryLogDto> getEntryLogsByParkingSpace(Integer parkingSpaceId);

    EntryLogDto handleEntry(String mcCode, String rfidCode, String imageBase64);

    EntryLogDto handleExit(String mcCode, String rfidCode);

    EntryLogDto createPendingEntry(String mcCode, String rfidCode);

    EntryLogDto updateEntryWithImage(String rfidCode, String imageBase64);
}
