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

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttProvisionHandler {

    private final IMqttProvisionService mqttProvisionService;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.base-topic}")
    private String baseTopic;

    @ServiceActivator(inputChannel = "mqttProvisionChannel")
    public void handleProvisionRequest(Message<?> message) {
        try {
            String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
            String payload = new String((byte[]) message.getPayload());

            log.info("Received provision request from topic: {}", topic);

            String[] parts = topic.split("/");
            if (parts.length < 5) {
                log.error("Invalid provision topic format: {}", topic);
                return;
            }

            String username = parts[1];
            String mcCode = parts[2];

            MqttProvisionRequestDto request = objectMapper.readValue(payload, MqttProvisionRequestDto.class);

            mqttProvisionService.handleProvision(username, mcCode, request);

            log.info("Provisioning completed for mcCode: {}", mcCode);

        } catch (Exception e) {
            log.error("Error handling provision request", e);
        }
    }
}
