package com.smart_parking_system.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_parking_system.backend.dto.LcdDto;
import com.smart_parking_system.backend.dto.mqtt.MqttLcdCommandDto;
import com.smart_parking_system.backend.dto.mqtt.MqttLcdStatusDto;
import com.smart_parking_system.backend.entity.Lcd;
import com.smart_parking_system.backend.repository.LcdRepository;
import com.smart_parking_system.backend.service.IMqttLcdService;
import com.smart_parking_system.backend.service.realtime.RealtimeEventPublisher;
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
public class MqttLcdServiceImpl implements IMqttLcdService {

    private final LcdRepository lcdRepository;
    private final ObjectMapper objectMapper;
    private final MessageChannel mqttOutboundChannel;
    private final RealtimeEventPublisher realtimeEventPublisher;

    @Value("${mqtt.base-topic}")
    private String baseTopic;

    @Override
    public void sendLcdCommand(Integer lcdId, String displayText) {
        Lcd lcd = lcdRepository.findById(lcdId)
                .orElseThrow(() -> new RuntimeException("LCD not found with id: " + lcdId));

        String mcCode = lcd.getMc().getMcCode();
        MqttLcdCommandDto command = new MqttLcdCommandDto(lcdId, displayText);

        try {
            String json = objectMapper.writeValueAsString(command);
            String topic = baseTopic + "/" + mcCode + "/lcd/command";

            mqttOutboundChannel.send(
                    MessageBuilder.withPayload(json)
                            .setHeader(MqttHeaders.TOPIC, topic)
                            .build());

            log.info("Sent LCD command: lcdId={}, displayText={}, topic={}", lcdId, displayText, topic);
        } catch (JsonProcessingException e) {
            log.error("Failed to send LCD command for lcdId: {}", lcdId, e);
            throw new RuntimeException("Failed to send LCD command", e);
        }
    }

    @Override
    @Transactional
    public LcdDto handleLcdStatus(String mcCode, MqttLcdStatusDto status) {
        Lcd lcd = lcdRepository.findById(status.getLcdId())
                .orElseThrow(() -> new RuntimeException("LCD not found with id: " + status.getLcdId()));

        if (lcd.getMc() == null || lcd.getMc().getMcCode() == null || !lcd.getMc().getMcCode().equals(mcCode)) {
            throw new RuntimeException("LCD does not belong to microcontroller: " + mcCode);
        }

        if (status.getDisplayText() != null) {
            lcd.setDisplayText(status.getDisplayText());
        }

        Lcd saved = lcdRepository.save(lcd);
        lcdRepository.flush();

        // Publish WebSocket event for real-time updates
        Integer parkingSpaceId = saved.getMc() != null && saved.getMc().getPs() != null
                ? saved.getMc().getPs().getId()
                : null;
        realtimeEventPublisher.publishLcdChanged(
                saved.getId(),
                saved.getName(),
                saved.getDisplayText(),
                saved.getMc() != null ? saved.getMc().getId() : null,
                parkingSpaceId);

        return toDto(saved);
    }

    private LcdDto toDto(Lcd lcd) {
        LcdDto dto = new LcdDto();
        dto.setId(lcd.getId());
        dto.setName(lcd.getName());
        dto.setDisplayText(lcd.getDisplayText());
        if (lcd.getMc() != null) {
            dto.setMicrocontrollerId(lcd.getMc().getId());
        }
        return dto;
    }
}
