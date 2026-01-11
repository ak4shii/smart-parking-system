#include "CredentialManager.h"
#include "config.h"
#include <Preferences.h>

// Global instance
CredentialManager credentialManager;

// Preferences objects for different namespaces
static Preferences prefsCredentials;
static Preferences prefsComponentIds;

CredentialManager::CredentialManager() : _initialized(false) {
}

void CredentialManager::begin() {
    if (_initialized) return;
    _initialized = true;
    DEBUG_PRINTLN("[CredentialManager] Initialized");
}

bool CredentialManager::saveCredentials(const Credentials& creds) {
    if (!prefsCredentials.begin(NVS_NAMESPACE_CREDENTIALS, false)) {
        DEBUG_PRINTLN("[CredentialManager] Failed to open credentials namespace");
        return false;
    }
    
    prefsCredentials.putString(NVS_KEY_WIFI_SSID, creds.wifiSsid);
    prefsCredentials.putString(NVS_KEY_WIFI_PASS, creds.wifiPassword);
    prefsCredentials.putString(NVS_KEY_MQTT_SERVER, creds.mqttServer);
    prefsCredentials.putInt(NVS_KEY_MQTT_PORT, creds.mqttPort);
    prefsCredentials.putString(NVS_KEY_MQTT_USER, creds.mqttUsername);
    prefsCredentials.putString(NVS_KEY_MQTT_PASS, creds.mqttPassword);
    prefsCredentials.putBool(NVS_KEY_CREDS_VALID, true);
    
    prefsCredentials.end();
    
    DEBUG_PRINTLN("[CredentialManager] Credentials saved successfully");
    return true;
}

bool CredentialManager::loadCredentials(Credentials& creds) {
    if (!prefsCredentials.begin(NVS_NAMESPACE_CREDENTIALS, true)) {
        DEBUG_PRINTLN("[CredentialManager] Failed to open credentials namespace");
        return false;
    }
    
    if (!prefsCredentials.getBool(NVS_KEY_CREDS_VALID, false)) {
        prefsCredentials.end();
        DEBUG_PRINTLN("[CredentialManager] No valid credentials found");
        return false;
    }
    
    creds.wifiSsid = prefsCredentials.getString(NVS_KEY_WIFI_SSID, "");
    creds.wifiPassword = prefsCredentials.getString(NVS_KEY_WIFI_PASS, "");
    creds.mqttServer = prefsCredentials.getString(NVS_KEY_MQTT_SERVER, "");
    creds.mqttPort = prefsCredentials.getInt(NVS_KEY_MQTT_PORT, 1883);
    creds.mqttUsername = prefsCredentials.getString(NVS_KEY_MQTT_USER, "");
    creds.mqttPassword = prefsCredentials.getString(NVS_KEY_MQTT_PASS, "");
    
    prefsCredentials.end();
    
    DEBUG_PRINTLN("[CredentialManager] Credentials loaded successfully");
    return true;
}

bool CredentialManager::hasValidCredentials() {
    if (!prefsCredentials.begin(NVS_NAMESPACE_CREDENTIALS, true)) {
        return false;
    }
    
    bool valid = prefsCredentials.getBool(NVS_KEY_CREDS_VALID, false);
    prefsCredentials.end();
    
    return valid;
}

void CredentialManager::clearCredentials() {
    if (!prefsCredentials.begin(NVS_NAMESPACE_CREDENTIALS, false)) {
        DEBUG_PRINTLN("[CredentialManager] Failed to open credentials namespace for clearing");
        return;
    }
    
    prefsCredentials.clear();
    prefsCredentials.end();
    
    DEBUG_PRINTLN("[CredentialManager] Credentials cleared");
}

bool CredentialManager::saveComponentIds(const ComponentIds& ids) {
    if (!prefsComponentIds.begin(NVS_NAMESPACE_COMPONENT_IDS, false)) {
        DEBUG_PRINTLN("[CredentialManager] Failed to open component IDs namespace");
        return false;
    }
    
    prefsComponentIds.putInt(NVS_KEY_ENTRY_DOOR_ID, ids.entryDoorId);
    prefsComponentIds.putInt(NVS_KEY_EXIT_DOOR_ID, ids.exitDoorId);
    prefsComponentIds.putInt(NVS_KEY_LCD_ID, ids.lcdId);
    prefsComponentIds.putInt(NVS_KEY_SENSOR1_ID, ids.sensor1Id);
    prefsComponentIds.putInt(NVS_KEY_SENSOR2_ID, ids.sensor2Id);
    prefsComponentIds.putBool(NVS_KEY_IDS_VALID, true);
    
    prefsComponentIds.end();
    
    DEBUG_PRINTLN("[CredentialManager] Component IDs saved successfully");
    return true;
}

bool CredentialManager::loadComponentIds(ComponentIds& ids) {
    if (!prefsComponentIds.begin(NVS_NAMESPACE_COMPONENT_IDS, true)) {
        DEBUG_PRINTLN("[CredentialManager] Failed to open component IDs namespace");
        return false;
    }
    
    if (!prefsComponentIds.getBool(NVS_KEY_IDS_VALID, false)) {
        prefsComponentIds.end();
        DEBUG_PRINTLN("[CredentialManager] No valid component IDs found");
        return false;
    }
    
    ids.entryDoorId = prefsComponentIds.getInt(NVS_KEY_ENTRY_DOOR_ID, -1);
    ids.exitDoorId = prefsComponentIds.getInt(NVS_KEY_EXIT_DOOR_ID, -1);
    ids.lcdId = prefsComponentIds.getInt(NVS_KEY_LCD_ID, -1);
    ids.sensor1Id = prefsComponentIds.getInt(NVS_KEY_SENSOR1_ID, -1);
    ids.sensor2Id = prefsComponentIds.getInt(NVS_KEY_SENSOR2_ID, -1);
    
    prefsComponentIds.end();
    
    DEBUG_PRINTLN("[CredentialManager] Component IDs loaded successfully");
    return true;
}

bool CredentialManager::hasValidComponentIds() {
    if (!prefsComponentIds.begin(NVS_NAMESPACE_COMPONENT_IDS, true)) {
        return false;
    }
    
    bool valid = prefsComponentIds.getBool(NVS_KEY_IDS_VALID, false);
    prefsComponentIds.end();
    
    return valid;
}

void CredentialManager::clearComponentIds() {
    if (!prefsComponentIds.begin(NVS_NAMESPACE_COMPONENT_IDS, false)) {
        DEBUG_PRINTLN("[CredentialManager] Failed to open component IDs namespace for clearing");
        return;
    }
    
    prefsComponentIds.clear();
    prefsComponentIds.end();
    
    DEBUG_PRINTLN("[CredentialManager] Component IDs cleared");
}

void CredentialManager::clearAll() {
    clearCredentials();
    clearComponentIds();
    DEBUG_PRINTLN("[CredentialManager] All data cleared");
}
