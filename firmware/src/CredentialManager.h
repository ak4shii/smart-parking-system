#ifndef CREDENTIAL_MANAGER_H
#define CREDENTIAL_MANAGER_H

#include <Arduino.h>

// Structure to hold WiFi and MQTT credentials
struct Credentials {
    String wifiSsid;
    String wifiPassword;
    String mqttServer;
    int mqttPort;
    String mqttUsername;
    String mqttPassword;
};

// Structure to hold component IDs assigned by server
struct ComponentIds {
    int entryDoorId;
    int exitDoorId;
    int lcdId;
    int sensor1Id;
    int sensor2Id;
};

class CredentialManager {
public:
    CredentialManager();
    
    // Initialize the credential manager
    void begin();
    
    // Credential management
    bool saveCredentials(const Credentials& creds);
    bool loadCredentials(Credentials& creds);
    bool hasValidCredentials();
    void clearCredentials();
    
    // Component ID management
    bool saveComponentIds(const ComponentIds& ids);
    bool loadComponentIds(ComponentIds& ids);
    bool hasValidComponentIds();
    void clearComponentIds();
    
    // Clear all stored data
    void clearAll();
    
private:
    bool _initialized;
};

// Global instance
extern CredentialManager credentialManager;

#endif // CREDENTIAL_MANAGER_H
