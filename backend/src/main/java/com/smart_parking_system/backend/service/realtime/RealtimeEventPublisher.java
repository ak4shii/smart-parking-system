package com.smart_parking_system.backend.service.realtime;

import com.smart_parking_system.backend.dto.realtime.DoorChangedEvent;
import com.smart_parking_system.backend.dto.realtime.EntryLogEvent;
import com.smart_parking_system.backend.dto.realtime.LcdChangedEvent;
import com.smart_parking_system.backend.dto.realtime.RfidChangedEvent;
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

    private final AtomicLong eventIdSeq = new AtomicLong(0);

    public long nextEventId() {
        return eventIdSeq.incrementAndGet();
    }

    public void publishSlotChanged(Integer slotId, Boolean isOccupied, Integer parkingSpaceId) {
        long eventId = nextEventId();
        SlotChangedEvent event = SlotChangedEvent.of(eventId, Instant.now(), slotId, isOccupied, parkingSpaceId);
        messagingTemplate.convertAndSend("/topic/overview_updates", event);
    }

    public void publishVehicleEntered(Integer entryLogId, String licensePlate, String rfidCode,
            Integer parkingSpaceId) {
        long eventId = nextEventId();
        EntryLogEvent event = EntryLogEvent.entered(eventId, Instant.now(), entryLogId, licensePlate, rfidCode,
                parkingSpaceId);
        messagingTemplate.convertAndSend("/topic/entrylog_new_events", event);
    }

    public void publishVehicleExited(Integer entryLogId, String licensePlate, String rfidCode, Integer parkingSpaceId) {
        long eventId = nextEventId();
        EntryLogEvent event = EntryLogEvent.exited(eventId, Instant.now(), entryLogId, licensePlate, rfidCode,
                parkingSpaceId);
        messagingTemplate.convertAndSend("/topic/entrylog_new_events", event);
    }

    public void publishDoorChanged(Integer doorId, String doorName, Boolean isOpened, Integer microcontrollerId,
            Integer parkingSpaceId) {
        long eventId = nextEventId();
        DoorChangedEvent event = DoorChangedEvent.of(eventId, Instant.now(), doorId, doorName, isOpened,
                microcontrollerId, parkingSpaceId);
        messagingTemplate.convertAndSend("/topic/door_updates", event);
    }

    public void publishLcdChanged(Integer lcdId, String lcdName, String display, Integer microcontrollerId,
            Integer parkingSpaceId) {
        long eventId = nextEventId();
        LcdChangedEvent event = LcdChangedEvent.of(eventId, Instant.now(), lcdId, lcdName, display, microcontrollerId,
                parkingSpaceId);
        messagingTemplate.convertAndSend("/topic/lcd_updates", event);
    }

    public void publishRfidChanged(Integer rfidId, String rfidCode, Boolean currentlyUsed, Integer parkingSpaceId) {
        long eventId = nextEventId();
        RfidChangedEvent event = RfidChangedEvent.of(eventId, Instant.now(), rfidId, rfidCode, currentlyUsed,
                parkingSpaceId);
        messagingTemplate.convertAndSend("/topic/rfid_updates", event);
    }
}
