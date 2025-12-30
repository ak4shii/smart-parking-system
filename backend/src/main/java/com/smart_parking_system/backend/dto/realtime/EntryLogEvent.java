package com.smart_parking_system.backend.dto.realtime;

import java.time.Instant;

public record EntryLogEvent(
        String type,
        long eventId,
        Instant timestamp,
        Integer entryLogId,
        String licensePlate,
        String rfidCode,
        Integer parkingSpaceId,
        String action
) {
    public static EntryLogEvent entered(long eventId, Instant timestamp, Integer entryLogId, String licensePlate, String rfidCode, Integer parkingSpaceId) {
        return new EntryLogEvent("entrylog_event", eventId, timestamp, entryLogId, licensePlate, rfidCode, parkingSpaceId, "vehicle_entered");
    }

    public static EntryLogEvent exited(long eventId, Instant timestamp, Integer entryLogId, String licensePlate, String rfidCode, Integer parkingSpaceId) {
        return new EntryLogEvent("entrylog_event", eventId, timestamp, entryLogId, licensePlate, rfidCode, parkingSpaceId, "vehicle_exited");
    }
}

