package com.smart_parking_system.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_parking_system.backend.dto.EntryLogDto;
import com.smart_parking_system.backend.dto.mqtt.MqttEntryRequestDto;
import com.smart_parking_system.backend.dto.mqtt.MqttEntryResponseDto;
import com.smart_parking_system.backend.dto.mqtt.MqttExitRequestDto;
import com.smart_parking_system.backend.dto.mqtt.MqttExitResponseDto;
import com.smart_parking_system.backend.service.IMqttEntryLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttEntryLogHandler {

    private static final int MIN_TOPIC_PARTS_FOR_ENTRY_EXIT = 4;
    private static final int MC_CODE_INDEX = 1;
    private static final int ACTION_INDEX = 2;
    private static final String ACTION_ENTRY = "entry";
    private static final String ACTION_EXIT = "exit";

    private final IMqttEntryLogService mqttEntryLogService;
    private final ObjectMapper objectMapper;
    private final MessageChannel mqttOutboundChannel;

    @Value("${MQTT_BASE_TOPIC:sps}")
    private String baseTopic;

    @ServiceActivator(inputChannel = "mqttEntryLogInputChannel")
    public void handleMessage(Message<?> message) {
        if (message == null) {
            log.warn("Received null message in entry log handler");
            return;
        }

        String topic = extractTopic(message);
        if (topic == null) {
            log.warn("Message missing topic header");
            return;
        }

        String[] topicParts = topic.split("/");
        if (topicParts.length < MIN_TOPIC_PARTS_FOR_ENTRY_EXIT) {
            log.debug("Topic has insufficient parts, ignoring: {}", topic);
            return;
        }

        String mcCode = topicParts[MC_CODE_INDEX];
        String action = topicParts[ACTION_INDEX];
        String payload = extractPayload(message);

        try {
            if (ACTION_ENTRY.equalsIgnoreCase(action)) {
                handleEntryMessage(mcCode, payload);
            } else if (ACTION_EXIT.equalsIgnoreCase(action)) {
                handleExitMessage(mcCode, payload);
            } else {
                log.debug("Unknown action in topic: {}", topic);
            }
        } catch (Exception ex) {
            log.error("Error processing entry log message. Topic: {}, Action: {}", topic, action, ex);
            handleErrorResponse(mcCode, action, ex);
        }
    }

    private void handleEntryMessage(String mcCode, String payload) throws Exception {
        MqttEntryRequestDto request = objectMapper.readValue(payload, MqttEntryRequestDto.class);
        EntryLogDto created = mqttEntryLogService.handleEntry(mcCode, request.getRfidCode(), request.getImageBase64());

        MqttEntryResponseDto response = new MqttEntryResponseDto(true, "OK", created.getId());
        publishResponse(baseTopic + "/" + mcCode + "/entry/response", response);
        log.debug("Processed entry request for mcCode: {}, rfidCode: {}", mcCode, request.getRfidCode());
    }

    private void handleExitMessage(String mcCode, String payload) throws Exception {
        MqttExitRequestDto request = objectMapper.readValue(payload, MqttExitRequestDto.class);
        EntryLogDto updated = mqttEntryLogService.handleExit(mcCode, request.getRfidCode());

        MqttExitResponseDto response = new MqttExitResponseDto(true, "OK", updated.getId());
        publishResponse(baseTopic + "/" + mcCode + "/exit/response", response);
        log.debug("Processed exit request for mcCode: {}, rfidCode: {}", mcCode, request.getRfidCode());
    }

    private void handleErrorResponse(String mcCode, String action, Exception ex) {
        try {
            if (ACTION_ENTRY.equalsIgnoreCase(action)) {
                MqttEntryResponseDto errorResponse = new MqttEntryResponseDto(false, ex.getMessage(), null);
                publishResponse(baseTopic + "/" + mcCode + "/entry/response", errorResponse);
            } else if (ACTION_EXIT.equalsIgnoreCase(action)) {
                MqttExitResponseDto errorResponse = new MqttExitResponseDto(false, ex.getMessage(), null);
                publishResponse(baseTopic + "/" + mcCode + "/exit/response", errorResponse);
            }
        } catch (Exception e) {
            log.error("Failed to send error response for mcCode: {}, action: {}", mcCode, action, e);
        }
    }

    private String extractTopic(Message<?> message) {
        Object topicHeader = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        return topicHeader != null ? topicHeader.toString() : null;
    }

    private String extractPayload(Message<?> message) {
        Object payload = message.getPayload();
        return payload != null ? String.valueOf(payload) : null;
    }

    private void publishResponse(String topic, Object payload) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        mqttOutboundChannel.send(
                MessageBuilder.withPayload(json)
                        .setHeader(MqttHeaders.TOPIC, topic)
                        .build()
        );
    }
}



