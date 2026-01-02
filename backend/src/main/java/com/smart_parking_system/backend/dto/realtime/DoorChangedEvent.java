package com.smart_parking_system.backend.dto.realtime;

import java.time.Instant;

public record DoorChangedEvent(
        String type,
        long eventId,
        Instant timestamp,
        Integer doorId,
        String doorName,
        Boolean isOpened,
        Integer microcontrollerId,
        Integer parkingSpaceId
) {
    public static DoorChangedEvent of(long eventId, Instant timestamp, Integer doorId, String doorName, Boolean isOpened, Integer microcontrollerId, Integer parkingSpaceId) {
        return new DoorChangedEvent("door_changed", eventId, timestamp, doorId, doorName, isOpened, microcontrollerId, parkingSpaceId);
    }
}

