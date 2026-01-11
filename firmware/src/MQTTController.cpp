#include "MQTTController.h"
#include "config.h"
#include <WiFi.h>
#include <ArduinoJson.h>

// Global instance
MQTTController mqttController;

// Static instance pointer for callback
MQTTController* MQTTController::_instance = nullptr;

// WiFi client for MQTT
static WiFiClient espClient;
static PubSubClient mqttClient(espClient);

MQTTController::MQTTController() 
    : _connected(false)
    , _provisioningComplete(false)
    , _lastReconnectAttempt(0)
    , _lastStatusPublish(0)
    , _startTime(0)
    , _commandCallback(nullptr)
    , _provisioningCallback(nullptr) {
    _instance = this;
    _provisionedIds = {-1, -1, -1, -1, -1};
}

void MQTTController::begin() {
    _startTime = millis();
    mqttClient.setCallback(staticCallback);
    mqttClient.setBufferSize(1024); // Increase buffer for larger messages
    DEBUG_PRINTLN("[MQTTController] Initialized");
}

void MQTTController::staticCallback(char* topic, byte* payload, unsigned int length) {
    if (_instance) {
        _instance->handleCallback(topic, payload, length);
    }
}

bool MQTTController::connect(const Credentials& creds) {
    _credentials = creds;
    
    // Build base topic: sps/<mqtt_username>/
    _baseTopic = "sps/" + creds.mqttUsername + "/";
    
    DEBUG_PRINTF("[MQTTController] Connecting to MQTT broker: %s:%d\n", 
                 creds.mqttServer.c_str(), creds.mqttPort);
    
    mqttClient.setServer(creds.mqttServer.c_str(), creds.mqttPort);
    
    // Build client ID
    String clientId = "ESP32-Parking-" + String(random(0xffff), HEX);
    
    // Build LWT topic and message
    String lwtTopic = buildTopic(MQTT_TOPIC_STATUS);
    String lwtMessage = "{\"online\":false}";
    
    // Connect with LWT
    bool success = mqttClient.connect(
        clientId.c_str(),
        creds.mqttUsername.c_str(),
        creds.mqttPassword.c_str(),
        lwtTopic.c_str(),
        1,      // QoS
        true,   // Retain
        lwtMessage.c_str()
    );
    
    if (success) {
        _connected = true;
        DEBUG_PRINTLN("[MQTTController] Connected to MQTT broker");
        
        // Subscribe to topics
        subscribeToTopics();
        
        // Publish online status
        publishStatus();
    } else {
        _connected = false;
        DEBUG_PRINTF("[MQTTController] Connection failed, rc=%d\n", mqttClient.state());
    }
    
    return success;
}

void MQTTController::subscribeToTopics() {
    // Subscribe to command topic
    String commandTopic = buildTopic(MQTT_TOPIC_COMMAND);
    mqttClient.subscribe(commandTopic.c_str());
    DEBUG_PRINTF("[MQTTController] Subscribed to: %s\n", commandTopic.c_str());
    
    // Subscribe to provisioning response topic
    String provisionTopic = buildTopic(MQTT_TOPIC_PROVISION_RESP);
    mqttClient.subscribe(provisionTopic.c_str());
    DEBUG_PRINTF("[MQTTController] Subscribed to: %s\n", provisionTopic.c_str());
}

bool MQTTController::isConnected() {
    _connected = mqttClient.connected();
    return _connected;
}

void MQTTController::disconnect() {
    mqttClient.disconnect();
    _connected = false;
    DEBUG_PRINTLN("[MQTTController] Disconnected");
}

void MQTTController::loop() {
    if (!mqttClient.connected()) {
        _connected = false;
        
        unsigned long now = millis();
        if (now - _lastReconnectAttempt > 5000) {
            _lastReconnectAttempt = now;
            DEBUG_PRINTLN("[MQTTController] Attempting reconnection...");
            
            if (connect(_credentials)) {
                _lastReconnectAttempt = 0;
            }
        }
    } else {
        mqttClient.loop();
        
        // Periodic status publish
        unsigned long now = millis();
        if (now - _lastStatusPublish >= STATUS_HEARTBEAT_INTERVAL_MS) {
            publishStatus();
            _lastStatusPublish = now;
        }
    }
}

void MQTTController::setCommandCallback(CommandCallback callback) {
    _commandCallback = callback;
}

void MQTTController::setProvisioningCallback(ProvisioningCallback callback) {
    _provisioningCallback = callback;
}

void MQTTController::handleCallback(char* topic, byte* payload, unsigned int length) {
    // Convert payload to string
    String message;
    message.reserve(length + 1);
    for (unsigned int i = 0; i < length; i++) {
        message += (char)payload[i];
    }
    
    DEBUG_PRINTF("[MQTTController] Received on %s: %s\n", topic, message.c_str());
    
    String topicStr = String(topic);
    String commandTopic = buildTopic(MQTT_TOPIC_COMMAND);
    String provisionTopic = buildTopic(MQTT_TOPIC_PROVISION_RESP);
    
    if (topicStr == commandTopic) {
        parseCommand(message);
    } else if (topicStr == provisionTopic) {
        parseProvisioningResponse(message);
    }
}

void MQTTController::parseCommand(const String& payload) {
    JsonDocument doc;
    DeserializationError error = deserializeJson(doc, payload);
    
    if (error) {
        DEBUG_PRINTF("[MQTTController] JSON parse error: %s\n", error.c_str());
        return;
    }
    
    String commandType = doc["commandType"] | "";
    String command = doc["command"] | "";
    
    CommandType cmd = CommandType::NONE;
    
    if (commandType == "entry" && command == "open") {
        cmd = CommandType::ENTRY_OPEN;
        DEBUG_PRINTLN("[MQTTController] Command: Entry Open");
    } else if (commandType == "exit" && command == "open") {
        cmd = CommandType::EXIT_OPEN;
        DEBUG_PRINTLN("[MQTTController] Command: Exit Open");
    } else if (commandType == "system" && command == "reset_credentials") {
        cmd = CommandType::SYSTEM_RESET_CREDENTIALS;
        DEBUG_PRINTLN("[MQTTController] Command: Reset Credentials");
    } else {
        cmd = CommandType::UNKNOWN;
        DEBUG_PRINTF("[MQTTController] Unknown command: %s/%s\n", 
                     commandType.c_str(), command.c_str());
    }
    
    if (_commandCallback && cmd != CommandType::NONE) {
        _commandCallback(cmd);
    }
}

void MQTTController::parseProvisioningResponse(const String& payload) {
    JsonDocument doc;
    DeserializationError error = deserializeJson(doc, payload);
    
    if (error) {
        DEBUG_PRINTF("[MQTTController] Provisioning JSON parse error: %s\n", error.c_str());
        if (_provisioningCallback) {
            _provisioningCallback(false, "JSON parse error");
        }
        return;
    }
    
    bool success = doc["success"] | false;
    String message = doc["message"] | "Unknown";
    
    if (success) {
        // Parse door IDs
        JsonArray doors = doc["doors"].as<JsonArray>();
        for (JsonObject door : doors) {
            String name = door["name"] | "";
            int id = door["id"] | -1;
            
            if (name == COMPONENT_NAME_ENTRY_DOOR) {
                _provisionedIds.entryDoorId = id;
            } else if (name == COMPONENT_NAME_EXIT_DOOR) {
                _provisionedIds.exitDoorId = id;
            }
        }
        
        // Parse LCD IDs
        JsonArray lcds = doc["lcds"].as<JsonArray>();
        for (JsonObject lcd : lcds) {
            String name = lcd["name"] | "";
            int id = lcd["id"] | -1;
            
            if (name == COMPONENT_NAME_LCD) {
                _provisionedIds.lcdId = id;
            }
        }
        
        // Parse sensor IDs
        JsonArray sensors = doc["sensors"].as<JsonArray>();
        for (JsonObject sensor : sensors) {
            String name = sensor["name"] | "";
            int id = sensor["id"] | -1;
            
            if (name == COMPONENT_NAME_SENSOR1) {
                _provisionedIds.sensor1Id = id;
            } else if (name == COMPONENT_NAME_SENSOR2) {
                _provisionedIds.sensor2Id = id;
            }
        }
        
        _provisioningComplete = true;
        
        DEBUG_PRINTLN("[MQTTController] Provisioning successful:");
        DEBUG_PRINTF("  Entry Door ID: %d\n", _provisionedIds.entryDoorId);
        DEBUG_PRINTF("  Exit Door ID: %d\n", _provisionedIds.exitDoorId);
        DEBUG_PRINTF("  LCD ID: %d\n", _provisionedIds.lcdId);
        DEBUG_PRINTF("  Sensor 1 ID: %d\n", _provisionedIds.sensor1Id);
        DEBUG_PRINTF("  Sensor 2 ID: %d\n", _provisionedIds.sensor2Id);
    } else {
        DEBUG_PRINTF("[MQTTController] Provisioning failed: %s\n", message.c_str());
    }
    
    if (_provisioningCallback) {
        _provisioningCallback(success, message);
    }
}

String MQTTController::buildTopic(const char* suffix) {
    return _baseTopic + suffix;
}

String MQTTController::getBaseTopic() {
    return _baseTopic;
}

bool MQTTController::publishEntryRequest(const String& rfidCode) {
    if (!isConnected()) return false;
    
    JsonDocument doc;
    doc["rfidCode"] = rfidCode;
    
    String payload;
    serializeJson(doc, payload);
    
    String topic = buildTopic(MQTT_TOPIC_ENTRY_REQUEST);
    bool success = mqttClient.publish(topic.c_str(), payload.c_str());
    
    DEBUG_PRINTF("[MQTTController] Published entry request: %s\n", payload.c_str());
    return success;
}

bool MQTTController::publishExitRequest(const String& rfidCode) {
    if (!isConnected()) return false;
    
    JsonDocument doc;
    doc["rfidCode"] = rfidCode;
    
    String payload;
    serializeJson(doc, payload);
    
    String topic = buildTopic(MQTT_TOPIC_EXIT_REQUEST);
    bool success = mqttClient.publish(topic.c_str(), payload.c_str());
    
    DEBUG_PRINTF("[MQTTController] Published exit request: %s\n", payload.c_str());
    return success;
}

bool MQTTController::publishSensorStatus(int sensorId, bool isOccupied) {
    if (!isConnected()) return false;
    
    JsonDocument doc;
    doc["sensorId"] = sensorId;
    doc["isOccupied"] = isOccupied;
    
    String payload;
    serializeJson(doc, payload);
    
    String topic = buildTopic(MQTT_TOPIC_SENSOR_STATUS);
    bool success = mqttClient.publish(topic.c_str(), payload.c_str());
    
    DEBUG_PRINTF("[MQTTController] Published sensor status: %s\n", payload.c_str());
    return success;
}

bool MQTTController::publishStatus() {
    if (!isConnected()) return false;
    
    unsigned long uptimeSec = (millis() - _startTime) / 1000;
    
    JsonDocument doc;
    doc["online"] = true;
    doc["uptimeSec"] = uptimeSec;
    
    String payload;
    serializeJson(doc, payload);
    
    String topic = buildTopic(MQTT_TOPIC_STATUS);
    bool success = mqttClient.publish(topic.c_str(), payload.c_str(), true); // Retained
    
    DEBUG_PRINTF("[MQTTController] Published status: %s\n", payload.c_str());
    return success;
}

bool MQTTController::publishProvisioningRequest() {
    if (!isConnected()) return false;
    
    JsonDocument doc;
    
    // Add doors
    JsonArray doors = doc["doors"].to<JsonArray>();
    JsonObject entryDoor = doors.add<JsonObject>();
    entryDoor["name"] = COMPONENT_NAME_ENTRY_DOOR;
    JsonObject exitDoor = doors.add<JsonObject>();
    exitDoor["name"] = COMPONENT_NAME_EXIT_DOOR;
    
    // Add LCDs
    JsonArray lcds = doc["lcds"].to<JsonArray>();
    JsonObject lcd = lcds.add<JsonObject>();
    lcd["name"] = COMPONENT_NAME_LCD;
    
    // Add sensors
    JsonArray sensors = doc["sensors"].to<JsonArray>();
    JsonObject sensor1 = sensors.add<JsonObject>();
    sensor1["name"] = COMPONENT_NAME_SENSOR1;
    sensor1["type"] = "ultrasonic";
    sensor1["slotName"] = COMPONENT_SLOT_NAME_1;
    
    JsonObject sensor2 = sensors.add<JsonObject>();
    sensor2["name"] = COMPONENT_NAME_SENSOR2;
    sensor2["type"] = "ultrasonic";
    sensor2["slotName"] = COMPONENT_SLOT_NAME_2;
    
    String payload;
    serializeJson(doc, payload);
    
    String topic = buildTopic(MQTT_TOPIC_PROVISION_REQ);
    bool success = mqttClient.publish(topic.c_str(), payload.c_str());
    
    DEBUG_PRINTF("[MQTTController] Published provisioning request: %s\n", payload.c_str());
    return success;
}

bool MQTTController::getProvisionedIds(ComponentIds& ids) {
    if (!_provisioningComplete) {
        return false;
    }
    ids = _provisionedIds;
    return true;
}

bool MQTTController::isProvisioningComplete() {
    return _provisioningComplete;
}
