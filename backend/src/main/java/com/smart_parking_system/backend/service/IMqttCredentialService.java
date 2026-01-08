package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.MqttCredentialsResponseDto;
import com.smart_parking_system.backend.entity.Microcontroller;

/**
 * Service for managing MQTT credentials for IoT devices.
 * Handles generation, storage, and retrieval of MQTT authentication data.
 */
public interface IMqttCredentialService {

    /**
     * Generate new MQTT credentials for a microcontroller.
     * Creates a unique username based on owner and device code,
     * generates a secure random password, and stores the hash.
     *
     * @param mc The microcontroller entity
     * @param ownerUsername The username of the device owner
     * @return DTO containing the generated credentials (password in plain text for one-time display)
     */
    MqttCredentialsResponseDto generateCredentials(Microcontroller mc, String ownerUsername);

    /**
     * Regenerate MQTT credentials for an existing device.
     * Useful when credentials are compromised or need to be rotated.
     *
     * @param mc The microcontroller entity
     * @param ownerUsername The username of the device owner
     * @return DTO containing the new credentials
     */
    MqttCredentialsResponseDto regenerateCredentials(Microcontroller mc, String ownerUsername);

    /**
     * Revoke/disable MQTT access for a device.
     * Sets mqttEnabled to false and clears credentials.
     *
     * @param mc The microcontroller entity
     */
    void revokeCredentials(Microcontroller mc);

    /**
     * Add MQTT user to Mosquitto password file.
     * This syncs the database credentials with the broker.
     *
     * @param mqttUsername The MQTT username
     * @param plainPassword The plain text password
     * @return true if successful, false otherwise
     */
    boolean syncToMosquitto(String mqttUsername, String plainPassword);

    /**
     * Remove MQTT user from Mosquitto password file.
     *
     * @param mqttUsername The MQTT username to remove
     * @return true if successful, false otherwise
     */
    boolean removeFromMosquitto(String mqttUsername);

    /**
     * Get MQTT credentials for a device (without the plain password).
     * Used by ESP32 during initial setup via secure HTTPS endpoint.
     *
     * @param mcCode The microcontroller code
     * @param ownerUsername The owner's username for verification
     * @return DTO with broker info and username (password must be retrieved separately)
     */
    MqttCredentialsResponseDto getCredentialsInfo(String mcCode, String ownerUsername);
}

