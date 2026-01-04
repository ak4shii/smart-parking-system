package com.smart_parking_system.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_parking_system.backend.dto.mqtt.MqttCameraCommandDto;
import com.smart_parking_system.backend.dto.mqtt.MqttDoorControlDto;
import com.smart_parking_system.backend.dto.mqtt.MqttEntryRequestDto;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttEntryRequestHandler {

    private final IEntryLogService entryLogService;
    private final MessageChannel mqttOutboundChannel;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.base-topic}")
    private String baseTopic;

    @ServiceActivator(inputChannel = "mqttEntryRequestChannel")
    public void handleEntryRequest(Message<?> message) {
        try {
            String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
            String payload = new String((byte[]) message.getPayload());

            log.info("Received entry request from topic: {}", topic);

            String[] parts = topic.split("/");
            if (parts.length < 4) {
                log.error("Invalid topic format: {}", topic);
                return;
            }

            String username = parts[1];
            String mcCode = parts[2];

            MqttEntryRequestDto request = objectMapper.readValue(payload, MqttEntryRequestDto.class);

            entryLogService.createPendingEntry(mcCode, request.getRfidCode());

            publishCameraCommand(username, mcCode, request.getRfidCode());

            publishDoorCommand(username, mcCode, "entry", "open");

            log.info("Entry request processed for mcCode: {}, rfidCode: {}", mcCode, request.getRfidCode());

        } catch (Exception e) {
            log.error("Error handling entry request", e);
        }
    }

    private void publishCameraCommand(String username, String mcCode, String rfidCode) {
        try {
            MqttCameraCommandDto command = new MqttCameraCommandDto("camera", "snap", rfidCode);
            String payload = objectMapper.writeValueAsString(command);
            String topic = baseTopic + "/" + username + "/" + mcCode + "/camera";

            mqttOutboundChannel.send(
                    MessageBuilder.withPayload(payload)
                            .setHeader(MqttHeaders.TOPIC, topic)
                            .build());

            log.info("Published camera command to topic: {}", topic);
        } catch (Exception e) {
            log.error("Failed to publish camera command", e);
        }
    }

    private void publishDoorCommand(String username, String mcCode, String commandType, String command) {
        try {
            MqttDoorControlDto doorCommand = new MqttDoorControlDto(commandType, command);
            String payload = objectMapper.writeValueAsString(doorCommand);
            String topic = baseTopic + "/" + username + "/" + mcCode + "/command";

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
