#include "LCDController.h"
#include "config.h"
#include <Wire.h>
#include <LiquidCrystal_I2C.h>

// Global instance
LCDController lcdController;

// LCD instance
static LiquidCrystal_I2C lcd(LCD_I2C_ADDR, LCD_COLS, LCD_ROWS);

LCDController::LCDController() 
    : _initialized(false)
    , _lastAvailable(-1)
    , _lastTotal(-1)
    , _backlightOn(false) {
}

void LCDController::begin() {
    // Initialize I2C
    Wire.begin(I2C_SDA, I2C_SCL);
    
    // Initialize LCD
    lcd.init();
    lcd.backlight();  // Turn on backlight
    lcd.clear();
    
    _initialized = true;
    _backlightOn = true;
    
    DEBUG_PRINTLN("[LCDController] Initialized (backlight on)");
    DEBUG_PRINTF("[LCDController] I2C Address: 0x%02X\n", LCD_I2C_ADDR);
}

void LCDController::clear() {
    if (!_initialized) return;
    lcd.clear();
}

void LCDController::showBootMessage() {
    if (!_initialized) return;
    
    lcd.clear();
    printCentered("Parking System", 0);
    printCentered("Booting...", 1);
    
    DEBUG_PRINTLN("[LCDController] Showing boot message");
}

void LCDController::showCaptivePortal(const String& apName) {
    if (!_initialized) return;
    
    lcd.clear();
    printCentered("Connect to WiFi:", 0);
    printCentered(apName, 1);
    
    DEBUG_PRINTF("[LCDController] Showing captive portal: %s\n", apName.c_str());
}

void LCDController::showConnecting() {
    if (!_initialized) return;
    
    lcd.clear();
    printCentered("Connecting to", 0);
    printCentered("WiFi...", 1);
    
    DEBUG_PRINTLN("[LCDController] Showing connecting");
}

void LCDController::showWiFiConnected(const String& ip) {
    if (!_initialized) return;
    
    lcd.clear();
    printCentered("WiFi Connected!", 0);
    printCentered(ip, 1);
    
    DEBUG_PRINTF("[LCDController] Showing WiFi connected: %s\n", ip.c_str());
}

void LCDController::showMQTTConnecting() {
    if (!_initialized) return;
    
    lcd.clear();
    printCentered("Connecting to", 0);
    printCentered("MQTT Server...", 1);
    
    DEBUG_PRINTLN("[LCDController] Showing MQTT connecting");
}

void LCDController::showWaitingForServer() {
    if (!_initialized) return;
    
    lcd.clear();
    printCentered("Waiting for", 0);
    printCentered("Server...", 1);
    
    DEBUG_PRINTLN("[LCDController] Showing waiting for server");
}

void LCDController::showProvisioningComplete() {
    if (!_initialized) return;
    
    lcd.clear();
    printCentered("Provisioning", 0);
    printCentered("Complete!", 1);
    
    DEBUG_PRINTLN("[LCDController] Showing provisioning complete");
}

void LCDController::updateSlots(int available, int total) {
    if (!_initialized) return;
    
    // Only update if values changed (prevent excessive LCD writes)
    if (available == _lastAvailable && total == _lastTotal) {
        return;
    }
    
    _lastAvailable = available;
    _lastTotal = total;
    
    lcd.clear();
    printCentered("Parking Slots", 0);
    
    // Format: "Available: X/Y"
    String slotsText = "Available: " + String(available) + "/" + String(total);
    printCentered(slotsText, 1);
    
    DEBUG_PRINTF("[LCDController] Updated slots: %d/%d\n", available, total);
}

void LCDController::showMessage(const String& line1, const String& line2) {
    if (!_initialized) return;
    
    lcd.clear();
    printCentered(line1, 0);
    if (line2.length() > 0) {
        printCentered(line2, 1);
    }
    
    DEBUG_PRINTF("[LCDController] Message: %s | %s\n", line1.c_str(), line2.c_str());
}

void LCDController::showError(const String& error) {
    if (!_initialized) return;
    
    lcd.clear();
    printCentered("ERROR:", 0);
    printCentered(error, 1);
    
    DEBUG_PRINTF("[LCDController] Error: %s\n", error.c_str());
}

void LCDController::backlightOn() {
    if (!_initialized) return;
    lcd.backlight();
    _backlightOn = true;
    DEBUG_PRINTLN("[LCDController] Backlight ON");
}

void LCDController::backlightOff() {
    if (!_initialized) return;
    lcd.noBacklight();
    _backlightOn = false;
    DEBUG_PRINTLN("[LCDController] Backlight OFF");
}

void LCDController::setBacklight(bool on) {
    if (on) {
        backlightOn();
    } else {
        backlightOff();
    }
}

void LCDController::printCentered(const String& text, int row) {
    if (row < 0 || row >= LCD_ROWS) return;
    
    String displayText = text;
    
    // Truncate if too long
    if (displayText.length() > LCD_COLS) {
        displayText = displayText.substring(0, LCD_COLS);
    }
    
    // Calculate starting position for centering
    int startPos = (LCD_COLS - displayText.length()) / 2;
    if (startPos < 0) startPos = 0;
    
    lcd.setCursor(startPos, row);
    lcd.print(displayText);
}
