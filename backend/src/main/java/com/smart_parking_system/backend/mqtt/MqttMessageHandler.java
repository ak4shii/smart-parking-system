package com.smart_parking_system.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_parking_system.backend.dto.EntryLogDto;
import com.smart_parking_system.backend.dto.mqtt.MqttEntryRequestDto;
import com.smart_parking_system.backend.dto.mqtt.MqttEntryResponseDto;
import com.smart_parking_system.backend.dto.mqtt.MqttExitRequestDto;
import com.smart_parking_system.backend.dto.mqtt.MqttExitResponseDto;
import com.smart_parking_system.backend.service.IMqttEntryLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MqttMessageHandler {

    private final IMqttEntryLogService IMqttEntryLogService;
    private final ObjectMapper objectMapper;
    private final MessageChannel mqttOutboundChannel;

    @Value("${mqtt.base-topic}")
    private String baseTopic;

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        String payload = String.valueOf(message.getPayload());

        try {
            String[] parts = topic.split("/");
            if (parts.length < 4) {
                return;
            }

            String mcCode = parts[1];
            String action = parts[2];

            if ("entry".equalsIgnoreCase(action)) {
                MqttEntryRequestDto req = objectMapper.readValue(payload, MqttEntryRequestDto.class);
                EntryLogDto created = IMqttEntryLogService.handleEntry(mcCode, req.getRfidCode(), req.getLicensePlate());

                MqttEntryResponseDto resp = new MqttEntryResponseDto(true, "OK", created.getId());
                publish(baseTopic + "/" + mcCode + "/entry/response", resp);

            } else if ("exit".equalsIgnoreCase(action)) {
                MqttExitRequestDto req = objectMapper.readValue(payload, MqttExitRequestDto.class);
                EntryLogDto updated = IMqttEntryLogService.handleExit(mcCode, req.getRfidCode());

                MqttExitResponseDto resp = new MqttExitResponseDto(true, "OK", updated.getId());
                publish(baseTopic + "/" + mcCode + "/exit/response", resp);
            }

        } catch (Exception ex) {
            try {
                String[] parts = topic.split("/");
                if (parts.length >= 3) {
                    String mcCode = parts[1];
                    String action = parts[2];

                    if ("entry".equalsIgnoreCase(action)) {
                        publish(baseTopic + "/" + mcCode + "/entry/response",
                                new MqttEntryResponseDto(false, ex.getMessage(), null));
                    } else if ("exit".equalsIgnoreCase(action)) {
                        publish(baseTopic + "/" + mcCode + "/exit/response",
                                new MqttExitResponseDto(false, ex.getMessage(), null));
                    }
                }
            } catch (Exception ignored) {

            }
        }
    }

    private void publish(String topic, Object payload) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        mqttOutboundChannel.send(
                MessageBuilder.withPayload(json)
                        .setHeader(MqttHeaders.TOPIC, topic)
                        .build()
        );
    }
}



