package com.smart_parking_system.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_parking_system.backend.dto.MicrocontrollerDto;
import com.smart_parking_system.backend.dto.mqtt.MqttStatusRequestDto;
import com.smart_parking_system.backend.dto.mqtt.MqttStatusResponseDto;
import com.smart_parking_system.backend.service.IMqttMicrocontrollerService;
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
public class MqttMicrocontrollerHandler {

    private static final int MIN_TOPIC_PARTS = 4;
    private static final int USERNAME_INDEX = 1;
    private static final int MC_CODE_INDEX = 2;

    private final IMqttMicrocontrollerService mqttMicrocontrollerService;
    private final ObjectMapper objectMapper;
    private final MessageChannel mqttOutboundChannel;

    @Value("${mqtt.base-topic}")
    private String baseTopic;

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

        String[] topicParts = topic.split("/");
        if (topicParts.length < MIN_TOPIC_PARTS) {
            log.debug("Topic has insufficient parts, ignoring: {}", topic);
            return;
        }

        String username = topicParts[USERNAME_INDEX];
        String mcCode = topicParts[MC_CODE_INDEX];
        String payload = extractPayload(message);

        try {
            handleStatus(username, mcCode, payload);
        } catch (Exception ex) {
            log.error("Error processing microcontroller status message. Topic: {}, mcCode: {}", topic, mcCode, ex);
            handleErrorResponse(username, mcCode, ex);
        }
    }

    private void handleStatus(String username, String mcCode, String payload) throws Exception {
        MqttStatusRequestDto request = objectMapper.readValue(payload, MqttStatusRequestDto.class);
        MicrocontrollerDto microcontrollerDto = mqttMicrocontrollerService.handleStatus(mcCode, request);

        MqttStatusResponseDto response = new MqttStatusResponseDto(true, "OK", microcontrollerDto.getId());
        publishResponse(baseTopic + "/" + username + "/" + mcCode + "/status/response", response);
        log.debug("Processed status update for mcCode: {}", mcCode);
    }

    private void handleErrorResponse(String username, String mcCode, Exception ex) {
        try {
            MqttStatusResponseDto errorResponse = new MqttStatusResponseDto(false, ex.getMessage(), null);
            publishResponse(baseTopic + "/" + username + "/" + mcCode + "/status/response", errorResponse);
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
                        .build());
    }
}
