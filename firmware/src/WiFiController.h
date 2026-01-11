#ifndef WIFI_CONTROLLER_H
#define WIFI_CONTROLLER_H

#include <Arduino.h>
#include "CredentialManager.h"

class WiFiController {
public:
    WiFiController();
    
    // Initialize WiFi controller
    void begin();
    
    // Start captive portal for configuration
    // Returns true if configuration was successful
    bool startCaptivePortal(Credentials& outCredentials);
    
    // Connect to WiFi using provided credentials
    bool connect(const Credentials& creds);
    
    // Connect using stored credentials
    bool connectWithStoredCredentials();
    
    // Check connection status
    bool isConnected();
    
    // Disconnect from WiFi
    void disconnect();
    
    // Get current WiFi SSID
    String getSSID();
    
    // Get current IP address
    String getIPAddress();
    
    // Handle WiFi events in loop
    void loop();
    
private:
    bool _connected;
    unsigned long _lastReconnectAttempt;
    Credentials _currentCredentials;
};

// Global instance
extern WiFiController wifiController;

#endif // WIFI_CONTROLLER_H
