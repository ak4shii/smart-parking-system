package com.smart_parking_system.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_parking_system.backend.dto.mqtt.MqttStatusRequestDto;
import com.smart_parking_system.backend.service.MqttMicrocontrollerStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttStatusHandler {

    private final MqttMicrocontrollerStatusService statusService;
    private final ObjectMapper objectMapper;
    private final MessageChannel mqttOutboundChannel;

    @ServiceActivator(inputChannel = "mqttStatusInputChannel")
    public void handleStatusMessage(Message<?> message) {
        String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
        String payload = (String) message.getPayload();

        try {
            String[] topicParts = topic.split("/");
            if (topicParts.length < 3) {
                log.warn("Invalid status topic format: {}", topic);
                return;
            }
            String mcCode = topicParts[1];

            MqttStatusRequestDto status = objectMapper.readValue(payload, MqttStatusRequestDto.class);
            statusService.handleStatus(mcCode, status);

            log.debug("Processed status update for mcCode: {}, status: {}", mcCode, status);

        } catch (Exception e) {
            log.error("Error processing status message. Topic: {}, Payload: {}", topic, payload, e);
        }
    }
}