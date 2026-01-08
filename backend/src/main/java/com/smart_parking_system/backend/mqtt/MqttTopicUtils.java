package com.smart_parking_system.backend.mqtt;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for parsing MQTT topic strings.
 * 
 * New Topic Structure: sps/{mqttUsername}/...
 * Where mqttUsername = {ownerUsername}_{mcCode}
 * Example: sps/john_mc12345678/sensor/status
 */
@Slf4j
public final class MqttTopicUtils {

    private MqttTopicUtils() {
        // Utility class
    }

    /**
     * Parse MQTT username from topic.
     * Topic format: sps/{mqttUsername}/...
     * 
     * @param topic The full MQTT topic
     * @return The mqttUsername (e.g., "john_mc12345678")
     */
    public static String extractMqttUsername(String topic) {
        if (topic == null || topic.isEmpty()) {
            return null;
        }
        String[] parts = topic.split("/");
        if (parts.length < 2) {
            return null;
        }
        return parts[1]; // sps/{mqttUsername}/...
    }

    /**
     * Extract owner username from mqttUsername.
     * mqttUsername format: {ownerUsername}_{mcCode}
     * 
     * @param mqttUsername The combined MQTT username
     * @return The owner's username
     */
    public static String extractOwnerUsername(String mqttUsername) {
        if (mqttUsername == null || !mqttUsername.contains("_")) {
            return null;
        }
        int lastUnderscore = mqttUsername.lastIndexOf("_");
        return mqttUsername.substring(0, lastUnderscore);
    }

    /**
     * Extract device code (mcCode) from mqttUsername.
     * mqttUsername format: {ownerUsername}_{mcCode}
     * 
     * @param mqttUsername The combined MQTT username
     * @return The device code (mcCode)
     */
    public static String extractMcCode(String mqttUsername) {
        if (mqttUsername == null || !mqttUsername.contains("_")) {
            return null;
        }
        int lastUnderscore = mqttUsername.lastIndexOf("_");
        return mqttUsername.substring(lastUnderscore + 1);
    }

    /**
     * Build topic for publishing to a device.
     * 
     * @param baseTopic The base topic (e.g., "sps")
     * @param mqttUsername The device's MQTT username
     * @param subtopic The specific subtopic (e.g., "command", "camera")
     * @return Full topic path
     */
    public static String buildTopic(String baseTopic, String mqttUsername, String subtopic) {
        return baseTopic + "/" + mqttUsername + "/" + subtopic;
    }

    /**
     * Check if topic matches expected pattern.
     * 
     * @param topic The topic to check
     * @param expectedSuffix The expected suffix (e.g., "/entry/request")
     * @return true if matches
     */
    public static boolean topicEndsWith(String topic, String expectedSuffix) {
        return topic != null && topic.endsWith(expectedSuffix);
    }

    /**
     * Validate minimum topic parts.
     * 
     * @param topic The topic to validate
     * @param minParts Minimum number of parts expected
     * @return true if valid
     */
    public static boolean hasMinimumParts(String topic, int minParts) {
        if (topic == null) {
            return false;
        }
        return topic.split("/").length >= minParts;
    }
}

