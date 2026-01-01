package com.smart_parking_system.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_parking_system.backend.dto.LcdDto;
import com.smart_parking_system.backend.dto.mqtt.MqttLcdStatusDto;
import com.smart_parking_system.backend.dto.mqtt.MqttLcdResponseDto;
import com.smart_parking_system.backend.service.IMqttLcdService;
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
public class MqttLcdHandler {

    private static final int MIN_TOPIC_PARTS = 3;
    private static final int MC_CODE_INDEX = 1;

    private final IMqttLcdService mqttLcdService;
    private final ObjectMapper objectMapper;
    private final MessageChannel mqttOutboundChannel;

    @Value("${mqtt.base-topic}")
    private String baseTopic;

    @ServiceActivator(inputChannel = "mqttLcdInputChannel")
    public void handleLcdMessage(Message<?> message) {
        if (message == null) {
            log.warn("Received null message in LCD handler");
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
            handleLcdStatus(mcCode, payload);
        } catch (Exception ex) {
            log.error("Error processing LCD status message. Topic: {}, mcCode: {}", topic, mcCode, ex);
            handleErrorResponse(mcCode, ex);
        }
    }

    private void handleLcdStatus(String mcCode, String payload) throws Exception {
        MqttLcdStatusDto status = objectMapper.readValue(payload, MqttLcdStatusDto.class);
        LcdDto lcdDto = mqttLcdService.handleLcdStatus(mcCode, status);

        MqttLcdResponseDto response = new MqttLcdResponseDto(true, "OK", lcdDto.getId());
        publishResponse(baseTopic + "/" + mcCode + "/lcd/response", response);
        log.debug("Processed LCD status update for mcCode: {}, lcdId: {}, displayText: {}", mcCode, status.getLcdId(), status.getDisplayText());
    }

    private void handleErrorResponse(String mcCode, Exception ex) {
        try {
            MqttLcdResponseDto errorResponse = new MqttLcdResponseDto(false, ex.getMessage(), null);
            publishResponse(baseTopic + "/" + mcCode + "/lcd/response", errorResponse);
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



