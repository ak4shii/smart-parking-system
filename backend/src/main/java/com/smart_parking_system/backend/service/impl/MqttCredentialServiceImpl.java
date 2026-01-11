package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.MqttCredentialsResponseDto;
import com.smart_parking_system.backend.entity.Microcontroller;
import com.smart_parking_system.backend.repository.MicrocontrollerRepository;
import com.smart_parking_system.backend.service.IMqttCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttCredentialServiceImpl implements IMqttCredentialService {

    private final MicrocontrollerRepository microcontrollerRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${mqtt.broker-host:localhost}")
    private String mqttBrokerHost;

    @Value("${mqtt.broker-port:1883}")
    private Integer mqttBrokerPort;

    @Value("${mqtt.base-topic:sps}")
    private String baseTopic;

    @Value("${mqtt.password-file:/mosquitto/config/passwords}")
    private String mosquittoPasswordFile;

    @Override
    @Transactional
    public MqttCredentialsResponseDto generateCredentials(Microcontroller mc, String ownerUsername) {
        String mqttUsername = ownerUsername + "_" + mc.getMcCode();

        String plainPassword = generateSecurePassword();

        String passwordHash = passwordEncoder.encode(plainPassword);

        mc.setMqttUsername(mqttUsername);
        mc.setMqttPasswordHash(passwordHash);
        mc.setMqttEnabled(true);
        microcontrollerRepository.save(mc);

        syncToMosquitto(mqttUsername, plainPassword);

        log.info("Generated MQTT credentials for device: {} with username: {}", mc.getMcCode(), mqttUsername);

        return buildCredentialsResponse(mc, mqttUsername, plainPassword);
    }

    @Override
    @Transactional
    public MqttCredentialsResponseDto regenerateCredentials(Microcontroller mc, String ownerUsername) {
        if (mc.getMqttUsername() != null) {
            removeFromMosquitto(mc.getMqttUsername());
        }

        return generateCredentials(mc, ownerUsername);
    }

    @Override
    @Transactional
    public void revokeCredentials(Microcontroller mc) {
        if (mc.getMqttUsername() != null) {
            removeFromMosquitto(mc.getMqttUsername());
            log.info("Revoked MQTT credentials for device: {}", mc.getMcCode());
        }

        mc.setMqttUsername(null);
        mc.setMqttPasswordHash(null);
        mc.setMqttEnabled(false);
        microcontrollerRepository.save(mc);
    }

    @Override
    public boolean syncToMosquitto(String mqttUsername, String plainPassword) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "exec", "sps-mosquitto",
                    "mosquitto_passwd", "-b", mosquittoPasswordFile, mqttUsername, plainPassword);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Successfully synced MQTT credentials to Mosquitto for user: {}", mqttUsername);

                if (reloadMosquitto()) {
                    log.info("Mosquitto reloaded - credentials active for user: {}", mqttUsername);
                } else {
                    log.warn("Mosquitto reload failed - manual restart may be required for user: {}", mqttUsername);
                }

                return true;
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    StringBuilder error = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        error.append(line);
                    }
                    log.error("Failed to sync MQTT credentials. Exit code: {}, Error: {}", exitCode, error);
                }
                return false;
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error syncing MQTT credentials to Mosquitto: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean removeFromMosquitto(String mqttUsername) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "exec", "sps-mosquitto",
                    "mosquitto_passwd", "-D", mosquittoPasswordFile, mqttUsername);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Successfully removed MQTT user from Mosquitto: {}", mqttUsername);
                if (reloadMosquitto()) {
                    log.info("Mosquitto reloaded - credentials revoked for user: {}", mqttUsername);
                } else {
                    log.warn("Mosquitto reload failed - user may still be able to connect temporarily: {}",
                            mqttUsername);
                }

                return true;
            } else {
                log.warn("Failed to remove MQTT user from Mosquitto. Exit code: {}", exitCode);
                return false;
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error removing MQTT user from Mosquitto: {}", e.getMessage());
            return false;
        }
    }

    private boolean reloadMosquitto() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "kill", "--signal=HUP", "sps-mosquitto");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Successfully sent SIGHUP signal to Mosquitto - configuration reloaded");
                return true;
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    StringBuilder error = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        error.append(line).append("\n");
                    }
                    log.error("Failed to send SIGHUP to Mosquitto. Exit code: {}, Error: {}", exitCode, error);
                }
                return false;
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error sending SIGHUP signal to Mosquitto: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    @Override
    public MqttCredentialsResponseDto getCredentialsInfo(String mcCode, String ownerUsername) {
        Microcontroller mc = microcontrollerRepository.findByMcCode(mcCode)
                .orElseThrow(() -> new RuntimeException("Microcontroller not found: " + mcCode));

        if (mc.getMqttUsername() == null) {
            throw new RuntimeException("MQTT credentials not generated for device: " + mcCode);
        }

        String expectedPrefix = ownerUsername + "_";
        if (!mc.getMqttUsername().startsWith(expectedPrefix)) {
            throw new RuntimeException("Access denied: Device does not belong to user");
        }

        return MqttCredentialsResponseDto.builder()
                .mqttHost(mqttBrokerHost)
                .mqttPort(mqttBrokerPort)
                .mqttUsername(mc.getMqttUsername())
                .mqttPassword(null)
                .baseTopic(baseTopic + "/" + mc.getMqttUsername())
                .mcCode(mc.getMcCode())
                .deviceName(mc.getName())
                .build();
    }

    private String generateSecurePassword() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private MqttCredentialsResponseDto buildCredentialsResponse(
            Microcontroller mc, String mqttUsername, String plainPassword) {
        return MqttCredentialsResponseDto.builder()
                .mqttHost(mqttBrokerHost)
                .mqttPort(mqttBrokerPort)
                .mqttUsername(mqttUsername)
                .mqttPassword(plainPassword)
                .baseTopic(baseTopic + "/" + mqttUsername)
                .mcCode(mc.getMcCode())
                .deviceName(mc.getName())
                .build();
    }
}
