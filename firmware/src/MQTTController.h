#ifndef MQTT_CONTROLLER_H
#define MQTT_CONTROLLER_H

#include <Arduino.h>
#include <PubSubClient.h>
#include "CredentialManager.h"

// Command types received from broker
enum class CommandType {
    NONE,
    ENTRY_OPEN,
    EXIT_OPEN,
    SYSTEM_RESET_CREDENTIALS,
    UNKNOWN
};

// Callback function type for handling commands
typedef void (*CommandCallback)(CommandType cmd);

// Callback function type for provisioning response
typedef void (*ProvisioningCallback)(bool success, const String& message);

class MQTTController {
public:
    MQTTController();
    
    // Initialize MQTT controller
    void begin();
    
    // Connect to MQTT broker
    bool connect(const Credentials& creds);
    
    // Check connection status
    bool isConnected();
    
    // Disconnect from broker
    void disconnect();
    
    // Process MQTT messages (call in loop)
    void loop();
    
    // Set command callback
    void setCommandCallback(CommandCallback callback);
    
    // Set provisioning callback
    void setProvisioningCallback(ProvisioningCallback callback);
    
    // Publish methods
    bool publishEntryRequest(const String& rfidCode);
    bool publishExitRequest(const String& rfidCode);
    bool publishSensorStatus(int sensorId, bool isOccupied);
    bool publishStatus();
    bool publishProvisioningRequest();
    
    // Get base topic
    String getBaseTopic();
    
    // Get parsed component IDs from provisioning response
    bool getProvisionedIds(ComponentIds& ids);
    
    // Check if provisioning is complete
    bool isProvisioningComplete();
    
private:
    void handleCallback(char* topic, byte* payload, unsigned int length);
    void subscribeToTopics();
    String buildTopic(const char* suffix);
    void parseCommand(const String& payload);
    void parseProvisioningResponse(const String& payload);
    
    static MQTTController* _instance;
    static void staticCallback(char* topic, byte* payload, unsigned int length);
    
    Credentials _credentials;
    String _baseTopic;
    bool _connected;
    bool _provisioningComplete;
    unsigned long _lastReconnectAttempt;
    unsigned long _lastStatusPublish;
    unsigned long _startTime;
    
    CommandCallback _commandCallback;
    ProvisioningCallback _provisioningCallback;
    ComponentIds _provisionedIds;
};

// Global instance
extern MQTTController mqttController;

#endif // MQTT_CONTROLLER_H
