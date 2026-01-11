#include "WiFiController.h"
#include "config.h"
#include <WiFi.h>
#include <WiFiManager.h>

// Global instance
WiFiController wifiController;

// Custom parameters for WiFiManager
static WiFiManagerParameter* paramMqttServer;
static WiFiManagerParameter* paramMqttPort;
static WiFiManagerParameter* paramMqttUser;
static WiFiManagerParameter* paramMqttPass;

WiFiController::WiFiController() 
    : _connected(false), _lastReconnectAttempt(0) {
}

void WiFiController::begin() {
    WiFi.mode(WIFI_STA);
    DEBUG_PRINTLN("[WiFiController] Initialized");
}

bool WiFiController::startCaptivePortal(Credentials& outCredentials) {
    DEBUG_PRINTLN("[WiFiController] Starting captive portal...");
    
    WiFiManager wifiManager;
    
    // Set timeout
    wifiManager.setConfigPortalTimeout(WIFI_CONFIG_TIMEOUT_SEC);
    
    // Create custom parameters for MQTT
    paramMqttServer = new WiFiManagerParameter("mqtt_server", "MQTT Server", "", 64);
    paramMqttPort = new WiFiManagerParameter("mqtt_port", "MQTT Port", "1883", 6);
    paramMqttUser = new WiFiManagerParameter("mqtt_user", "MQTT Username", "", 32);
    paramMqttPass = new WiFiManagerParameter("mqtt_pass", "MQTT Password", "", 64);
    
    // Add custom parameters
    wifiManager.addParameter(paramMqttServer);
    wifiManager.addParameter(paramMqttPort);
    wifiManager.addParameter(paramMqttUser);
    wifiManager.addParameter(paramMqttPass);
    
    // Start configuration portal
    bool success = wifiManager.startConfigPortal(WIFI_AP_NAME, WIFI_AP_PASSWORD);
    
    if (success) {
        // Get WiFi credentials
        outCredentials.wifiSsid = WiFi.SSID();
        outCredentials.wifiPassword = WiFi.psk();
        
        // Get MQTT credentials from custom parameters
        outCredentials.mqttServer = String(paramMqttServer->getValue());
        outCredentials.mqttPort = String(paramMqttPort->getValue()).toInt();
        outCredentials.mqttUsername = String(paramMqttUser->getValue());
        outCredentials.mqttPassword = String(paramMqttPass->getValue());
        
        // Validate MQTT port
        if (outCredentials.mqttPort <= 0 || outCredentials.mqttPort > 65535) {
            outCredentials.mqttPort = 1883;
        }
        
        _connected = true;
        _currentCredentials = outCredentials;
        
        DEBUG_PRINTLN("[WiFiController] Configuration successful");
        DEBUG_PRINTF("[WiFiController] Connected to: %s\n", outCredentials.wifiSsid.c_str());
        DEBUG_PRINTF("[WiFiController] MQTT Server: %s:%d\n", 
                     outCredentials.mqttServer.c_str(), outCredentials.mqttPort);
    } else {
        DEBUG_PRINTLN("[WiFiController] Configuration failed or timed out");
    }
    
    // Clean up
    delete paramMqttServer;
    delete paramMqttPort;
    delete paramMqttUser;
    delete paramMqttPass;
    
    return success;
}

bool WiFiController::connect(const Credentials& creds) {
    DEBUG_PRINTF("[WiFiController] Connecting to WiFi: %s\n", creds.wifiSsid.c_str());
    
    WiFi.begin(creds.wifiSsid.c_str(), creds.wifiPassword.c_str());
    
    // Wait for connection with timeout
    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 30) {
        delay(500);
        DEBUG_PRINT(".");
        attempts++;
    }
    DEBUG_PRINTLN();
    
    if (WiFi.status() == WL_CONNECTED) {
        _connected = true;
        _currentCredentials = creds;
        DEBUG_PRINTF("[WiFiController] Connected! IP: %s\n", WiFi.localIP().toString().c_str());
        return true;
    }
    
    DEBUG_PRINTLN("[WiFiController] Connection failed");
    _connected = false;
    return false;
}

bool WiFiController::connectWithStoredCredentials() {
    Credentials creds;
    if (!credentialManager.loadCredentials(creds)) {
        DEBUG_PRINTLN("[WiFiController] No stored credentials found");
        return false;
    }
    
    return connect(creds);
}

bool WiFiController::isConnected() {
    _connected = (WiFi.status() == WL_CONNECTED);
    return _connected;
}

void WiFiController::disconnect() {
    WiFi.disconnect();
    _connected = false;
    DEBUG_PRINTLN("[WiFiController] Disconnected");
}

String WiFiController::getSSID() {
    return WiFi.SSID();
}

String WiFiController::getIPAddress() {
    if (isConnected()) {
        return WiFi.localIP().toString();
    }
    return "0.0.0.0";
}

void WiFiController::loop() {
    // Check for disconnection and attempt reconnection
    if (!isConnected()) {
        unsigned long now = millis();
        if (now - _lastReconnectAttempt > 10000) { // Try every 10 seconds
            _lastReconnectAttempt = now;
            DEBUG_PRINTLN("[WiFiController] Attempting reconnection...");
            
            if (_currentCredentials.wifiSsid.length() > 0) {
                connect(_currentCredentials);
            }
        }
    }
}
