package com.smart_parking_system.backend.service.realtime;

import com.smart_parking_system.backend.dto.realtime.EntryLogEvent;
import com.smart_parking_system.backend.dto.realtime.SlotChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class RealtimeEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    // Simple in-memory event id generator.
    // If you need cross-restart replay, persist events in DB.
    private final AtomicLong eventIdSeq = new AtomicLong(0);

    public long nextEventId() {
        return eventIdSeq.incrementAndGet();
    }

    public void publishSlotChanged(Integer slotId, Boolean isOccupied, Integer parkingSpaceId) {
        long eventId = nextEventId();
        SlotChangedEvent event = SlotChangedEvent.of(eventId, Instant.now(), slotId, isOccupied, parkingSpaceId);
        messagingTemplate.convertAndSend("/topic/overview_updates", event);
    }

    public void publishVehicleEntered(Integer entryLogId, String licensePlate, String rfidCode, Integer parkingSpaceId) {
        long eventId = nextEventId();
        EntryLogEvent event = EntryLogEvent.entered(eventId, Instant.now(), entryLogId, licensePlate, rfidCode, parkingSpaceId);
        messagingTemplate.convertAndSend("/topic/entrylog_new_events", event);
    }

    public void publishVehicleExited(Integer entryLogId, String licensePlate, String rfidCode, Integer parkingSpaceId) {
        long eventId = nextEventId();
        EntryLogEvent event = EntryLogEvent.exited(eventId, Instant.now(), entryLogId, licensePlate, rfidCode, parkingSpaceId);
        messagingTemplate.convertAndSend("/topic/entrylog_new_events", event);
    }
}

