package com.smart_parking_system.backend.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MqttTopicUtil {

    private MqttTopicUtil() {
        // Utility class
    }

    public static String extractMqttUsername(String topic) {
        if (topic == null || topic.isEmpty()) {
            return null;
        }
        String[] parts = topic.split("/");
        if (parts.length < 2) {
            return null;
        }
        return parts[1];
    }

    public static String extractOwnerUsername(String mqttUsername) {
        if (mqttUsername == null || !mqttUsername.contains("_")) {
            return null;
        }
        int lastUnderscore = mqttUsername.lastIndexOf("_");
        return mqttUsername.substring(0, lastUnderscore);
    }

    public static String extractMcCode(String mqttUsername) {
        if (mqttUsername == null || !mqttUsername.contains("_")) {
            return null;
        }
        int lastUnderscore = mqttUsername.lastIndexOf("_");
        return mqttUsername.substring(lastUnderscore + 1);
    }

    public static String buildTopic(String baseTopic, String mqttUsername, String subtopic) {
        return baseTopic + "/" + mqttUsername + "/" + subtopic;
    }

    public static boolean topicEndsWith(String topic, String expectedSuffix) {
        return topic != null && topic.endsWith(expectedSuffix);
    }

    public static boolean hasMinimumParts(String topic, int minParts) {
        if (topic == null) {
            return false;
        }
        return topic.split("/").length >= minParts;
    }
}
