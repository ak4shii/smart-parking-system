package com.smart_parking_system.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_parking_system.backend.dto.mqtt.MqttProvisionRequestDto;
import com.smart_parking_system.backend.dto.mqtt.MqttProvisionResponseDto;
import com.smart_parking_system.backend.service.IMqttProvisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttProvisionHandler {

    private static final int MIN_TOPIC_PARTS = 3;
    private static final int MC_CODE_INDEX = 1;

    private final IMqttProvisionService mqttProvisionService;
    private final ObjectMapper objectMapper;
    private final MessageChannel mqttOutboundChannel;

    @Value("${mqtt.base-topic}")
    private String baseTopic;

    @ServiceActivator(inputChannel = "mqttProvisionInputChannel")
    public void handleProvisionMessage(Message<?> message) {
        if (message == null) {
            log.warn("Received null message in provision handler");
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
            handleProvision(mcCode, payload);
        } catch (Exception ex) {
            log.error("Error processing provision message. Topic: {}, mcCode: {}", topic, mcCode, ex);
            handleErrorResponse(mcCode, ex);
        }
    }

    private void handleProvision(String mcCode, String payload) throws Exception {
        MqttProvisionRequestDto request = objectMapper.readValue(payload, MqttProvisionRequestDto.class);
        MqttProvisionResponseDto response = mqttProvisionService.handleProvision(mcCode, request);

        publishResponse(baseTopic + "/" + mcCode + "/provision/response", response);
        log.info("Processed provision request for mcCode: {}, doors: {}, lcds: {}, sensors: {}",
                mcCode,
                request.getDoors() != null ? request.getDoors().size() : 0,
                request.getLcds() != null ? request.getLcds().size() : 0,
                request.getSensors() != null ? request.getSensors().size() : 0);
    }

    private void handleErrorResponse(String mcCode, Exception ex) {
        try {
            MqttProvisionResponseDto errorResponse = new MqttProvisionResponseDto(
                    false,
                    ex.getMessage(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>()
            );
            publishResponse(baseTopic + "/" + mcCode + "/provision/response", errorResponse);
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

