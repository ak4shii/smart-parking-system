package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.EntryLogDto;

public interface IMqttEntryLogService {

    EntryLogDto handleEntry(String mcCode, String rfidCode, String imageBase64);

    EntryLogDto handleExit(String mcCode, String rfidCode);
}

