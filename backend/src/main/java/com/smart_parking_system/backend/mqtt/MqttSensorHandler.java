package com.smart_parking_system.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_parking_system.backend.dto.SensorDto;
import com.smart_parking_system.backend.dto.mqtt.MqttSensorStatusDto;
import com.smart_parking_system.backend.dto.mqtt.MqttSensorResponseDto;
import com.smart_parking_system.backend.service.IMqttSensorService;
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
public class MqttSensorHandler {

    private static final int MIN_TOPIC_PARTS = 3;
    private static final int MC_CODE_INDEX = 1;

    private final IMqttSensorService mqttSensorService;
    private final ObjectMapper objectMapper;
    private final MessageChannel mqttOutboundChannel;

    @Value("${mqtt.base-topic}")
    private String baseTopic;

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

        String[] topicParts = topic.split("/");
        if (topicParts.length < MIN_TOPIC_PARTS) {
            log.debug("Topic has insufficient parts, ignoring: {}", topic);
            return;
        }

        String mcCode = topicParts[MC_CODE_INDEX];
        String payload = extractPayload(message);

        try {
            handleSensorStatus(mcCode, payload);
        } catch (Exception ex) {
            log.error("Error processing sensor status message. Topic: {}, mcCode: {}", topic, mcCode, ex);
            handleErrorResponse(mcCode, ex);
        }
    }

    private void handleSensorStatus(String mcCode, String payload) throws Exception {
        MqttSensorStatusDto status = objectMapper.readValue(payload, MqttSensorStatusDto.class);
        SensorDto sensorDto = mqttSensorService.handleSensorStatus(mcCode, status);

        MqttSensorResponseDto response = new MqttSensorResponseDto(true, "OK", sensorDto.getId());
        publishResponse(baseTopic + "/" + mcCode + "/sensor/response", response);
        log.debug("Processed sensor status update for mcCode: {}, sensorId: {}, isOccupied: {}", 
                mcCode, status.getSensorId(), status.getIsOccupied());
    }

    private void handleErrorResponse(String mcCode, Exception ex) {
        try {
            MqttSensorResponseDto errorResponse = new MqttSensorResponseDto(false, ex.getMessage(), null);
            publishResponse(baseTopic + "/" + mcCode + "/sensor/response", errorResponse);
        } catch (Exception e) {
            log.error("Failed to send error response for mcCode: {}", mcCode, e);
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

