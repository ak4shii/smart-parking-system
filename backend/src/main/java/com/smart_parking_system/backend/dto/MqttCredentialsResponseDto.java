package com.smart_parking_system.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for returning MQTT credentials to ESP32 devices during provisioning.
 * Contains all information needed for device to connect to MQTT broker.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttCredentialsResponseDto {

    /**
     * MQTT broker host address
     */
    private String mqttHost;

    /**
     * MQTT broker port (default: 1883)
     */
    private Integer mqttPort;

    /**
     * MQTT username for this device (format: {ownerUsername}_{mcCode})
     */
    private String mqttUsername;

    /**
     * MQTT password for this device (plain text, one-time display)
     */
    private String mqttPassword;

    /**
     * Base topic for this device (format: sps/{ownerUsername}_{mcCode})
     */
    private String baseTopic;

    /**
     * Device code for identification
     */
    private String mcCode;

    /**
     * Human-readable device name
     */
    private String deviceName;
}

