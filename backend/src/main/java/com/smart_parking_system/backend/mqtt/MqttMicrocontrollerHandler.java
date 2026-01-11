package com.smart_parking_system.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_parking_system.backend.dto.mqtt.MqttStatusRequestDto;
import com.smart_parking_system.backend.service.IMqttMicrocontrollerService;
import com.smart_parking_system.backend.util.MqttTopicUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttMicrocontrollerHandler {

    private static final int MIN_TOPIC_PARTS = 3;

    private final IMqttMicrocontrollerService mqttMicrocontrollerService;
    private final ObjectMapper objectMapper;

    @ServiceActivator(inputChannel = "mqttMicrocontrollerInputChannel")
    public void handleStatusMessage(Message<?> message) {
        if (message == null) {
            log.warn("Received null message in microcontroller handler");
            return;
        }

        String topic = extractTopic(message);
        if (topic == null) {
            log.warn("Message missing topic header");
            return;
        }

        if (!MqttTopicUtil.hasMinimumParts(topic, MIN_TOPIC_PARTS)) {
            log.debug("Topic has insufficient parts, ignoring: {}", topic);
            return;
        }

        String mqttUsername = MqttTopicUtil.extractMqttUsername(topic);
        String mcCode = MqttTopicUtil.extractMcCode(mqttUsername);

        if (mcCode == null) {
            log.error("Could not extract mcCode from mqttUsername: {}", mqttUsername);
            return;
        }

        String payload = extractPayload(message);

        try {
            MqttStatusRequestDto request = objectMapper.readValue(payload, MqttStatusRequestDto.class);
            mqttMicrocontrollerService.handleStatus(mcCode, request);
            log.debug("Processed status update for mcCode: {}", mcCode);
        } catch (Exception ex) {
            log.error("Error processing microcontroller status. Topic: {}, mcCode: {}", topic, mcCode, ex);
        }
    }

    private String extractTopic(Message<?> message) {
        Object topicHeader = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        return topicHeader != null ? topicHeader.toString() : null;
    }

    private String extractPayload(Message<?> message) {
        Object payload = message.getPayload();
        if (payload instanceof byte[]) {
            return new String((byte[]) payload);
        }
        return payload != null ? payload.toString() : null;
    }
}
