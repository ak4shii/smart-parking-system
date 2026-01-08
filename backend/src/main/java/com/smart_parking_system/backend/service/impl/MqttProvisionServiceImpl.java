package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.mqtt.MqttProvisionRequestDto;
import com.smart_parking_system.backend.dto.mqtt.MqttProvisionResponseDto;
import com.smart_parking_system.backend.entity.Door;
import com.smart_parking_system.backend.entity.Lcd;
import com.smart_parking_system.backend.entity.Microcontroller;
import com.smart_parking_system.backend.entity.Sensor;
import com.smart_parking_system.backend.entity.Slot;
import com.smart_parking_system.backend.repository.DoorRepository;
import com.smart_parking_system.backend.repository.LcdRepository;
import com.smart_parking_system.backend.repository.MicrocontrollerRepository;
import com.smart_parking_system.backend.repository.SensorRepository;
import com.smart_parking_system.backend.repository.SlotRepository;
import com.smart_parking_system.backend.service.IMqttProvisionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles device provisioning via MQTT.
 * 
 * Topic Structure:
 * - Request: sps/{mqttUsername}/provision/request
 * - Response: sps/{mqttUsername}/provision/response
 * 
 * Where mqttUsername = {ownerUsername}_{mcCode}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqttProvisionServiceImpl implements IMqttProvisionService {

    private final MicrocontrollerRepository microcontrollerRepository;
    private final DoorRepository doorRepository;
    private final LcdRepository lcdRepository;
    private final SensorRepository sensorRepository;
    private final SlotRepository slotRepository;
    private final MqttPahoClientFactory mqttClientFactory;
    private final ObjectMapper objectMapper;
    private final MessageChannel mqttOutboundChannel;

    @Value("${mqtt.client-id:sps-backend}")
    private String clientId;

    @Value("${mqtt.base-topic:sps}")
    private String baseTopic;

    @Override
    public MqttProvisionRequestDto checkForProvisionData(String username, String mcCode, int timeoutSeconds) {
        String mqttUsername = username + "_" + mcCode;
        String topic = baseTopic + "/" + mqttUsername + "/provision/request";
        CompletableFuture<String> messageFuture = new CompletableFuture<>();

        IMqttClient client = null;
        try {
            client = mqttClientFactory.getClientInstance(clientId + "-provision-check-" + mcCode, topic);

            final IMqttClient finalClient = client;

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("Connection lost during provision check for mcCode: {}", mcCode);
                    messageFuture.completeExceptionally(cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    log.info("Received provision message for mcCode: {}", mcCode);
                    messageFuture.complete(payload);

                    try {
                        finalClient.disconnect();
                    } catch (MqttException e) {
                        log.warn("Error disconnecting after receiving message", e);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            if (!client.isConnected()) {
                client.connect(mqttClientFactory.getConnectionOptions());
            }

            client.subscribe(topic, 1);

            String payload = messageFuture.get(timeoutSeconds, TimeUnit.SECONDS);

            if (payload != null && !payload.trim().isEmpty()) {
                return objectMapper.readValue(payload, MqttProvisionRequestDto.class);
            }

            return null;

        } catch (java.util.concurrent.TimeoutException e) {
            log.info("No provision data found for mcCode: {} (timeout)", mcCode);
            return null;
        } catch (Exception e) {
            log.error("Error checking for provision data for mcCode: {}", mcCode, e);
            throw new RuntimeException("Failed to check for provision data", e);
        } finally {
            if (client != null && client.isConnected()) {
                try {
                    client.disconnect();
                    client.close();
                } catch (MqttException e) {
                    log.warn("Error disconnecting MQTT client", e);
                }
            }
        }
    }

    @Override
    @Transactional
    public MqttProvisionResponseDto handleProvision(String username, String mcCode, MqttProvisionRequestDto request) {
        Microcontroller mc = microcontrollerRepository.findByMcCode(mcCode)
                .orElseThrow(() -> new RuntimeException("Microcontroller not found with mcCode: " + mcCode));

        List<MqttProvisionResponseDto.ComponentResponseDto> doorResponses = new ArrayList<>();
        List<MqttProvisionResponseDto.ComponentResponseDto> lcdResponses = new ArrayList<>();
        List<MqttProvisionResponseDto.ComponentResponseDto> sensorResponses = new ArrayList<>();

        if (request.getDoors() != null) {
            for (MqttProvisionRequestDto.ComponentDto doorDto : request.getDoors()) {
                Door door = findOrCreateDoor(mc, doorDto.getName());
                doorRepository.saveAndFlush(door);
                doorResponses.add(new MqttProvisionResponseDto.ComponentResponseDto(door.getId(), door.getName()));
            }
        }

        if (request.getLcds() != null) {
            for (MqttProvisionRequestDto.ComponentDto lcdDto : request.getLcds()) {
                Lcd lcd = findOrCreateLcd(mc, lcdDto.getName());
                lcdRepository.saveAndFlush(lcd);
                lcdResponses.add(new MqttProvisionResponseDto.ComponentResponseDto(lcd.getId(), lcd.getName()));
            }
        }

        if (request.getSensors() != null) {
            for (MqttProvisionRequestDto.SensorComponentDto sensorDto : request.getSensors()) {
                if (sensorDto.getSlotName() == null || sensorDto.getSlotName().trim().isEmpty()) {
                    continue;
                }

                Slot slot = findOrCreateSlot(mc, sensorDto.getSlotName());
                if (slot == null) {
                    continue;
                }

                Sensor sensor = findOrCreateSensor(mc, sensorDto, slot);
                sensorRepository.saveAndFlush(sensor);
                sensorResponses
                        .add(new MqttProvisionResponseDto.ComponentResponseDto(sensor.getId(), sensor.getName()));
            }
        }

        MqttProvisionResponseDto response = new MqttProvisionResponseDto(
                true,
                "Provisioning completed successfully",
                doorResponses,
                lcdResponses,
                sensorResponses);

        publishProvisionResponse(username, mcCode, response);

        return response;
    }

    private void publishProvisionResponse(String username, String mcCode, MqttProvisionResponseDto response) {
        try {
            String mqttUsername = username + "_" + mcCode;
            String topic = baseTopic + "/" + mqttUsername + "/provision/response";
            String json = objectMapper.writeValueAsString(response);

            mqttOutboundChannel.send(
                    org.springframework.messaging.support.MessageBuilder.withPayload(json)
                            .setHeader(org.springframework.integration.mqtt.support.MqttHeaders.TOPIC, topic)
                            .build());

            log.info("Published provision response to topic: {}", topic);
        } catch (Exception e) {
            log.error("Failed to publish provision response for mcCode: {}", mcCode, e);
        }
    }

    private Door findOrCreateDoor(Microcontroller mc, String name) {
        String normalizedName = name == null ? null : name.trim();
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new RuntimeException("Door name is required");
        }

        return doorRepository.findByName(normalizedName)
                .map(existing -> {
                    if (existing.getMc() != null && !existing.getMc().getId().equals(mc.getId())) {
                        throw new RuntimeException(
                                "Door name already exists and is assigned to another microcontroller: "
                                        + normalizedName);
                    }
                    if (existing.getMc() == null) {
                        existing.setMc(mc);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Door door = new Door();
                    door.setName(normalizedName);
                    door.setMc(mc);
                    door.setIsOpened(false);
                    return door;
                });
    }

    private Lcd findOrCreateLcd(Microcontroller mc, String name) {
        String normalizedName = name == null ? null : name.trim();
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new RuntimeException("LCD name is required");
        }

        return lcdRepository.findByName(normalizedName)
                .map(existing -> {
                    if (existing.getMc() != null && !existing.getMc().getId().equals(mc.getId())) {
                        throw new RuntimeException(
                                "LCD name already exists and is assigned to another microcontroller: "
                                        + normalizedName);
                    }
                    if (existing.getMc() == null) {
                        existing.setMc(mc);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Lcd lcd = new Lcd();
                    lcd.setName(normalizedName);
                    lcd.setMc(mc);
                    lcd.setDisplayText("");
                    return lcd;
                });
    }

    private Slot findOrCreateSlot(Microcontroller mc, String slotName) {
        if (mc.getPs() == null) {
            return null;
        }

        return slotRepository.findByName(slotName)
                .orElseGet(() -> {
                    Slot slot = new Slot();
                    slot.setName(slotName);
                    slot.setPs(mc.getPs());
                    slot.setIsOccupied(false);
                    slotRepository.save(slot);
                    return slot;
                });
    }

    private Sensor findOrCreateSensor(Microcontroller mc, MqttProvisionRequestDto.SensorComponentDto sensorDto,
            Slot slot) {
        String normalizedName = sensorDto.getName() == null ? null : sensorDto.getName().trim();
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new RuntimeException("Sensor name is required");
        }

        return sensorRepository.findByName(normalizedName)
                .map(existing -> {
                    if (existing.getMc() != null && !existing.getMc().getId().equals(mc.getId())) {
                        throw new RuntimeException(
                                "Sensor name already exists and is assigned to another microcontroller: "
                                        + normalizedName);
                    }
                    if (existing.getSlot() != null && !existing.getSlot().getId().equals(slot.getId())) {
                        throw new RuntimeException(
                                "Sensor name already exists and is assigned to another slot: " + normalizedName);
                    }
                    if (existing.getMc() == null) {
                        existing.setMc(mc);
                    }
                    if (existing.getSlot() == null) {
                        existing.setSlot(slot);
                    }
                    if (sensorDto.getType() != null) {
                        existing.setType(sensorDto.getType());
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Sensor sensor = new Sensor();
                    sensor.setName(normalizedName);
                    sensor.setType(sensorDto.getType());
                    sensor.setMc(mc);
                    sensor.setSlot(slot);
                    return sensor;
                });
    }
}
