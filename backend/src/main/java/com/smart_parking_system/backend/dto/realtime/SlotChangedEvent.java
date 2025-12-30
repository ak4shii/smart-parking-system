package com.smart_parking_system.backend.dto.realtime;

import java.time.Instant;

public record SlotChangedEvent(
        String type,
        long eventId,
        Instant timestamp,
        Integer slotId,
        Boolean isOccupied,
        Integer parkingSpaceId
) {
    public static SlotChangedEvent of(long eventId, Instant timestamp, Integer slotId, Boolean isOccupied, Integer parkingSpaceId) {
        return new SlotChangedEvent("slot_changed", eventId, timestamp, slotId, isOccupied, parkingSpaceId);
    }
}

