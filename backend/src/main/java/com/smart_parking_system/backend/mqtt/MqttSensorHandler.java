package com.smart_parking_system.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_parking_system.backend.dto.mqtt.MqttSensorStatusDto;
import com.smart_parking_system.backend.service.IMqttSensorService;
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
public class MqttSensorHandler {

    private static final int MIN_TOPIC_PARTS = 4;

    private final IMqttSensorService mqttSensorService;
    private final ObjectMapper objectMapper;

    @ServiceActivator(inputChannel = "mqttSensorInputChannel")
    public void handleSensorMessage(Message<?> message) {
        if (message == null) {
            log.warn("Received null message in sensor handler");
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
            MqttSensorStatusDto status = objectMapper.readValue(payload, MqttSensorStatusDto.class);
            mqttSensorService.handleSensorStatus(mcCode, status);
            log.debug("Processed sensor status for mcCode: {}, sensorId: {}, isOccupied: {}",
                    mcCode, status.getSensorId(), status.getIsOccupied());
        } catch (Exception ex) {
            log.error("Error processing sensor status. Topic: {}, mcCode: {}", topic, mcCode, ex);
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
