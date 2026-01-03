package com.smart_parking_system.backend.dto.realtime;

import java.time.Instant;

public record LcdChangedEvent(
        String type,
        long eventId,
        Instant timestamp,
        Integer lcdId,
        String lcdName,
        String display,
        Integer microcontrollerId,
        Integer parkingSpaceId
) {
    public static LcdChangedEvent of(long eventId, Instant timestamp, Integer lcdId, String lcdName, String display, Integer microcontrollerId, Integer parkingSpaceId) {
        return new LcdChangedEvent("lcd_changed", eventId, timestamp, lcdId, lcdName, display, microcontrollerId, parkingSpaceId);
    }
}

