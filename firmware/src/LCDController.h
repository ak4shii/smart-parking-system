#ifndef LCD_CONTROLLER_H
#define LCD_CONTROLLER_H

#include <Arduino.h>

class LCDController {
public:
    LCDController();
    
    // Initialize LCD display
    void begin();
    
    // Clear display
    void clear();
    
    // Show boot message
    void showBootMessage();
    
    // Show captive portal message
    void showCaptivePortal(const String& apName);
    
    // Show connecting message
    void showConnecting();
    
    // Show WiFi connected message
    void showWiFiConnected(const String& ip);
    
    // Show MQTT connecting message
    void showMQTTConnecting();
    
    // Show waiting for server message (provisioning)
    void showWaitingForServer();
    
    // Show provisioning complete message
    void showProvisioningComplete();
    
    // Update slots display (main operating screen)
    void updateSlots(int available, int total);
    
    // Show custom message
    void showMessage(const String& line1, const String& line2 = "");
    
    // Show error message
    void showError(const String& error);
    
    // Backlight control
    void backlightOn();
    void backlightOff();
    void setBacklight(bool on);
    
private:
    void printCentered(const String& text, int row);
    bool _initialized;
    int _lastAvailable;
    int _lastTotal;
    bool _backlightOn;
};

// Global instance
extern LCDController lcdController;

#endif // LCD_CONTROLLER_H
