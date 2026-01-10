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

/**
 * Implementation of MQTT credential management service.
 * Generates secure credentials and syncs with Mosquitto broker.
 */
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

    // Path to Mosquitto password file (configurable for Docker/local environments)
    @Value("${mqtt.password-file:/mosquitto/config/passwords}")
    private String mosquittoPasswordFile;

    @Override
    @Transactional
    public MqttCredentialsResponseDto generateCredentials(Microcontroller mc, String ownerUsername) {
        // Generate MQTT username: {ownerUsername}_{mcCode}
        String mqttUsername = ownerUsername + "_" + mc.getMcCode();

        // Generate secure random password (24 bytes = 32 chars base64)
        String plainPassword = generateSecurePassword();

        // Hash password for storage
        String passwordHash = passwordEncoder.encode(plainPassword);

        // Update microcontroller entity
        mc.setMqttUsername(mqttUsername);
        mc.setMqttPasswordHash(passwordHash);
        mc.setMqttEnabled(true);
        microcontrollerRepository.save(mc);

        // Sync to Mosquitto password file
        syncToMosquitto(mqttUsername, plainPassword);

        log.info("Generated MQTT credentials for device: {} with username: {}", mc.getMcCode(), mqttUsername);

        return buildCredentialsResponse(mc, mqttUsername, plainPassword);
    }

    @Override
    @Transactional
    public MqttCredentialsResponseDto regenerateCredentials(Microcontroller mc, String ownerUsername) {
        // Remove old credentials from Mosquitto if exists
        if (mc.getMqttUsername() != null) {
            removeFromMosquitto(mc.getMqttUsername());
        }

        // Generate new credentials
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
            // Use mosquitto_passwd tool to add/update user
            // -b flag for batch mode (password from command line)
            ProcessBuilder pb = new ProcessBuilder(
                    "mosquitto_passwd", "-b", mosquittoPasswordFile, mqttUsername, plainPassword);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Successfully synced MQTT credentials to Mosquitto for user: {}", mqttUsername);

                // Reload Mosquitto to activate new credentials immediately
                if (reloadMosquitto()) {
                    log.info("Mosquitto reloaded - credentials active for user: {}", mqttUsername);
                } else {
                    log.warn("Mosquitto reload failed - manual restart may be required for user: {}", mqttUsername);
                }

                return true;
            } else {
                // Read error output
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
            // Don't fail the operation - credentials are still in database
            // Manual sync can be done later
            return false;
        }
    }

    @Override
    public boolean removeFromMosquitto(String mqttUsername) {
        try {
            // Use mosquitto_passwd tool to delete user
            // -D flag for delete
            ProcessBuilder pb = new ProcessBuilder(
                    "mosquitto_passwd", "-D", mosquittoPasswordFile, mqttUsername);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Successfully removed MQTT user from Mosquitto: {}", mqttUsername);

                // Reload Mosquitto to deactivate removed credentials immediately
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

    /**
     * Reload Mosquitto configuration by sending SIGHUP signal.
     * This achieves graceful reload without disconnecting active clients.
     * 
     * @return true if reload was successful, false otherwise
     */
    private boolean reloadMosquitto() {
        try {
            // Send SIGHUP signal to Mosquitto container
            // This causes Mosquitto to reload config files without full restart
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "kill", "--signal=HUP", "sps-mosquitto");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Successfully sent SIGHUP signal to Mosquitto - configuration reloaded");
                return true;
            } else {
                // Read error output
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
                Thread.currentThread().interrupt(); // Restore interrupt status
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

        // Verify ownership by checking username prefix
        String expectedPrefix = ownerUsername + "_";
        if (!mc.getMqttUsername().startsWith(expectedPrefix)) {
            throw new RuntimeException("Access denied: Device does not belong to user");
        }

        // Return info without password (password is only shown once during generation)
        return MqttCredentialsResponseDto.builder()
                .mqttHost(mqttBrokerHost)
                .mqttPort(mqttBrokerPort)
                .mqttUsername(mc.getMqttUsername())
                .mqttPassword(null) // Never return stored password
                .baseTopic(baseTopic + "/" + mc.getMqttUsername())
                .mcCode(mc.getMcCode())
                .deviceName(mc.getName())
                .build();
    }

    /**
     * Generate a cryptographically secure random password.
     * Uses 24 random bytes encoded as Base64 (32 characters).
     */
    private String generateSecurePassword() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Build the credentials response DTO.
     */
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
