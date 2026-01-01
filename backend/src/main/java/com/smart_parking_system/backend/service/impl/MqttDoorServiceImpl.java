package com.smart_parking_system.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_parking_system.backend.dto.DoorDto;
import com.smart_parking_system.backend.dto.mqtt.MqttDoorCommandDto;
import com.smart_parking_system.backend.dto.mqtt.MqttDoorStatusDto;
import com.smart_parking_system.backend.entity.Door;
import com.smart_parking_system.backend.repository.DoorRepository;
import com.smart_parking_system.backend.service.IMqttDoorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttDoorServiceImpl implements IMqttDoorService {

    private final DoorRepository doorRepository;
    private final ObjectMapper objectMapper;
    private final MessageChannel mqttOutboundChannel;

    @Value("${mqtt.base-topic}")
    private String baseTopic;

    @Override
    public void sendDoorCommand(Integer doorId, boolean open) {
        Door door = doorRepository.findById(doorId)
                .orElseThrow(() -> new RuntimeException("Door not found with id: " + doorId));

        String mcCode = door.getMc().getMcCode();
        MqttDoorCommandDto command = new MqttDoorCommandDto(doorId, open);

        try {
            String json = objectMapper.writeValueAsString(command);
            String topic = baseTopic + "/" + mcCode + "/door/command";

            mqttOutboundChannel.send(
                    MessageBuilder.withPayload(json)
                            .setHeader(MqttHeaders.TOPIC, topic)
                            .build()
            );

            log.info("Sent door command: doorId={}, open={}, topic={}", doorId, open, topic);
        } catch (JsonProcessingException e) {
            log.error("Failed to send door command for doorId: {}", doorId, e);
            throw new RuntimeException("Failed to send door command", e);
        }
    }

    @Override
    @Transactional
    public DoorDto handleDoorStatus(String mcCode, MqttDoorStatusDto status) {
        Door door = doorRepository.findById(status.getDoorId())
                .orElseThrow(() -> new RuntimeException("Door not found with id: " + status.getDoorId()));

        if (status.getIsOpened() != null) {
            door.setIsOpened(status.getIsOpened());
        }

        Door saved = doorRepository.save(door);
        doorRepository.flush();

        return toDto(saved);
    }

    private DoorDto toDto(Door door) {
        DoorDto dto = new DoorDto();
        dto.setId(door.getId());
        dto.setName(door.getName());
        dto.setIsOpened(door.getIsOpened());
        if (door.getMc() != null) {
            dto.setMicrocontrollerId(door.getMc().getId());
        }
        return dto;
    }
}



