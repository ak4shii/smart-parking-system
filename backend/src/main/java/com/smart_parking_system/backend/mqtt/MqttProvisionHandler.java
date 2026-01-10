package com.smart_parking_system.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_parking_system.backend.dto.mqtt.MqttProvisionRequestDto;
import com.smart_parking_system.backend.service.IMqttProvisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Handles MQTT device provisioning requests.
 * 
 * Topic: sps/{mqttUsername}/provision/request
 * Where mqttUsername = {ownerUsername}_{mcCode}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttProvisionHandler {

    private static final int MIN_TOPIC_PARTS = 3;

    private final IMqttProvisionService mqttProvisionService;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.base-topic}")
    private String baseTopic;

    @ServiceActivator(inputChannel = "mqttProvisionChannel")
    public void handleProvisionRequest(Message<?> message) {
        log.info(">>> MqttProvisionHandler: Message Received! <<<");
        try {
            String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
            log.info(">>> Received topic: {}", topic);

            Object payloadObj = message.getPayload();
            log.info(">>> Payload Class: {}", payloadObj != null ? payloadObj.getClass().getName() : "null");

            String payload;
            if (payloadObj instanceof byte[]) {
                payload = new String((byte[]) payloadObj);
            } else if (payloadObj != null) {
                payload = payloadObj.toString();
            } else {
                log.error("Payload is null!");
                return;
            }
            log.info(">>> Payload Content: {}", payload);

            if (!MqttTopicUtils.hasMinimumParts(topic, MIN_TOPIC_PARTS)) {
                log.error("Invalid provision topic format: {}", topic);
                return;
            }

            String mqttUsername = MqttTopicUtils.extractMqttUsername(topic);
            log.info(">>> Extracted mqttUsername: {}", mqttUsername);

            String ownerUsername = MqttTopicUtils.extractOwnerUsername(mqttUsername);
            String mcCode = MqttTopicUtils.extractMcCode(mqttUsername);
            log.info(">>> Extracted owner: {}, mcCode: {}", ownerUsername, mcCode);

            if (mcCode == null || ownerUsername == null) {
                log.error("Could not parse mqttUsername: {}", mqttUsername);
                return;
            }

            MqttProvisionRequestDto request = objectMapper.readValue(payload, MqttProvisionRequestDto.class);
            log.info(">>> DTO Parsed Successfully. Calling service...");

            mqttProvisionService.handleProvision(ownerUsername, mcCode, request);

            log.info("Provisioning completed for mcCode: {}", mcCode);

        } catch (Exception e) {
            log.error("Error handling provision request", e);
        }
    }
}
