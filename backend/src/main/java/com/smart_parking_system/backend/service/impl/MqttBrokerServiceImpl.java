package com.smart_parking_system.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_parking_system.backend.service.IMqttBrokerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttBrokerServiceImpl implements IMqttBrokerService {

    private final MessageChannel mqttOutboundChannel;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.base-topic}")
    private String baseTopic;

    private static final String DYNSEC_TOPIC = "$CONTROL/dynamic-security/v1";

    @Override
    public void createBrokerUser(String username, String password) {
        try {
            Map<String, Object> command = new HashMap<>();
            command.put("command", "createClient");

            Map<String, Object> client = new HashMap<>();
            client.put("username", username);
            client.put("password", password);
            client.put("textname", "ESP32 Device User");
            client.put("textdescription", "Auto-generated user for Smart Parking System");

            command.put("client", client);

            String payload = objectMapper.writeValueAsString(command);

            mqttOutboundChannel.send(
                    MessageBuilder.withPayload(payload)
                            .setHeader(MqttHeaders.TOPIC, DYNSEC_TOPIC)
                            .build());

            log.info("Created Mosquitto broker user: {}", username);
        } catch (Exception e) {
            log.error("Failed to create Mosquitto broker user: {}", username, e);
            throw new RuntimeException("Failed to create MQTT broker account", e);
        }
    }

    @Override
    public void deleteBrokerUser(String username) {
        try {
            Map<String, Object> command = new HashMap<>();
            command.put("command", "deleteClient");
            command.put("username", username);

            String payload = objectMapper.writeValueAsString(command);

            mqttOutboundChannel.send(
                    MessageBuilder.withPayload(payload)
                            .setHeader(MqttHeaders.TOPIC, DYNSEC_TOPIC)
                            .build());

            log.info("Deleted Mosquitto broker user: {}", username);
        } catch (Exception e) {
            log.error("Failed to delete Mosquitto broker user: {}", username, e);
            throw new RuntimeException("Failed to delete MQTT broker account", e);
        }
    }

    @Override
    public void setUserAcl(String username, Integer userId) {
        try {
            String roleName = "role_user_" + userId;
            createRole(roleName, userId);

            assignRoleToClient(username, roleName);

            log.info("Set ACL for user {} to topic pattern: sps/{}/# ", username, userId);
        } catch (Exception e) {
            log.error("Failed to set ACL for user: {}", username, e);
            throw new RuntimeException("Failed to set MQTT ACL", e);
        }
    }

    private void createRole(String roleName, Integer userId) throws Exception {
        Map<String, Object> command = new HashMap<>();
        command.put("command", "createRole");
        command.put("rolename", roleName);
        command.put("textname", "User " + userId + " Role");
        command.put("textdescription", "ACL role for user ID " + userId);

        Map<String, Object> acl = new HashMap<>();
        acl.put("acltype", "subscribePattern");
        acl.put("topic", baseTopic + "/" + userId + "/#");
        acl.put("priority", 0);
        acl.put("allow", true);

        command.put("acls", List.of(acl));

        String payload = objectMapper.writeValueAsString(command);

        mqttOutboundChannel.send(
                MessageBuilder.withPayload(payload)
                        .setHeader(MqttHeaders.TOPIC, DYNSEC_TOPIC)
                        .build());
    }

    private void assignRoleToClient(String username, String roleName) throws Exception {
        Map<String, Object> command = new HashMap<>();
        command.put("command", "addClientRole");
        command.put("username", username);
        command.put("rolename", roleName);

        String payload = objectMapper.writeValueAsString(command);

        mqttOutboundChannel.send(
                MessageBuilder.withPayload(payload)
                        .setHeader(MqttHeaders.TOPIC, DYNSEC_TOPIC)
                        .build());
    }
}
