package com.smart_parking_system.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_parking_system.backend.dto.mqtt.MqttDoorControlDto;
import com.smart_parking_system.backend.dto.mqtt.MqttExitRequestDto;
import com.smart_parking_system.backend.service.IEntryLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Handles MQTT exit gate requests.
 * 
 * Topic: sps/{mqttUsername}/exit/request
 * Where mqttUsername = {ownerUsername}_{mcCode}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttExitRequestHandler {

    private static final int MIN_TOPIC_PARTS = 3;

    private final IEntryLogService entryLogService;
    private final MessageChannel mqttOutboundChannel;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.base-topic}")
    private String baseTopic;

    @ServiceActivator(inputChannel = "mqttEntryRequestChannel")
    public void handleExitRequest(Message<?> message) {
        try {
            String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
            String payload = new String((byte[]) message.getPayload());

            // Only handle exit requests
            if (!MqttTopicUtils.topicEndsWith(topic, "/exit/request")) {
                return;
            }

            log.info("Received exit request from topic: {}", topic);

            if (!MqttTopicUtils.hasMinimumParts(topic, MIN_TOPIC_PARTS)) {
                log.error("Invalid topic format: {}", topic);
                return;
            }

            String mqttUsername = MqttTopicUtils.extractMqttUsername(topic);
            String mcCode = MqttTopicUtils.extractMcCode(mqttUsername);

            if (mcCode == null) {
                log.error("Could not extract mcCode from mqttUsername: {}", mqttUsername);
                return;
            }

            MqttExitRequestDto request = objectMapper.readValue(payload, MqttExitRequestDto.class);

            entryLogService.handleExit(mcCode, request.getRfidCode());

            publishDoorCommand(mqttUsername, "exit", "open");

            log.info("Exit request processed for mqttUsername: {}, rfidCode: {}", mqttUsername, request.getRfidCode());

        } catch (Exception e) {
            log.error("Error handling exit request", e);
        }
    }

    private void publishDoorCommand(String mqttUsername, String commandType, String command) {
        try {
            MqttDoorControlDto doorCommand = new MqttDoorControlDto(commandType, command);
            String payload = objectMapper.writeValueAsString(doorCommand);
            String topic = MqttTopicUtils.buildTopic(baseTopic, mqttUsername, "command");

            mqttOutboundChannel.send(
                    MessageBuilder.withPayload(payload)
                            .setHeader(MqttHeaders.TOPIC, topic)
                            .build());

            log.info("Published door command to topic: {}", topic);
        } catch (Exception e) {
            log.error("Failed to publish door command", e);
        }
    }
}
