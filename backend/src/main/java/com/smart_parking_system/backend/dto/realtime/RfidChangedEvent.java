package com.smart_parking_system.backend.dto.realtime;

import java.time.Instant;

public record RfidChangedEvent(
        String type,
        long eventId,
        Instant timestamp,
        Integer rfidId,
        String rfidCode,
        Boolean currentlyUsed,
        Integer parkingSpaceId) {
    public static RfidChangedEvent of(long eventId, Instant timestamp, Integer rfidId, String rfidCode,
            Boolean currentlyUsed, Integer parkingSpaceId) {
        return new RfidChangedEvent("rfid_changed", eventId, timestamp, rfidId, rfidCode, currentlyUsed,
                parkingSpaceId);
    }
}
