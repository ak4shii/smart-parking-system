/**
 * ESP32 Car Parking System Firmware
 * 
 * Features:
 * - 2 RFID scanners for entry/exit gates
 * - 2 IR sensors for vehicle detection at gates
 * - 2 Ultrasonic sensors for parking slot detection
 * - 2 Servos for gate control
 * - 1 LCD (WC1602 I2C) for displaying available slots
 * - Captive portal for WiFi/MQTT configuration
 * - MQTT communication with server
 * - Component provisioning system
 */

#include <Arduino.h>
#include "config.h"
#include "CredentialManager.h"
#include "WiFiController.h"
#include "MQTTController.h"
#include "RFIDController.h"
#include "ServoController.h"
#include "IRController.h"
#include "UltrasonicController.h"
#include "LCDController.h"

// ============================================================================
// STATE VARIABLES
// ============================================================================

// System state
enum class SystemState {
    BOOT,
    CAPTIVE_PORTAL,
    CONNECTING_WIFI,
    CONNECTING_MQTT,
    PROVISIONING,
    RUNNING,
    ERROR
};

static SystemState currentState = SystemState::BOOT;

// Gate state tracking
static bool entryGateWaitingForCommand = false;
static bool exitGateWaitingForCommand = false;
static String pendingEntryRfid = "";
static String pendingExitRfid = "";

// Timing variables
static unsigned long lastRfidScan = 0;
static unsigned long lastUltrasonicScan = 0;
static unsigned long provisioningStartTime = 0;

// Provisioning timeout (30 seconds)
static const unsigned long PROVISIONING_TIMEOUT_MS = 30000;

// ============================================================================
// CALLBACKS
// ============================================================================

void onMQTTCommand(CommandType cmd) {
    switch (cmd) {
        case CommandType::ENTRY_OPEN:
            DEBUG_PRINTLN("[Main] Received entry open command");
            if (entryGateWaitingForCommand) {
                servoController.openEntryGate();
                entryGateWaitingForCommand = false;
                pendingEntryRfid = "";
            }
            break;
            
        case CommandType::EXIT_OPEN:
            DEBUG_PRINTLN("[Main] Received exit open command");
            if (exitGateWaitingForCommand) {
                servoController.openExitGate();
                exitGateWaitingForCommand = false;
                pendingExitRfid = "";
            }
            break;
            
        case CommandType::SYSTEM_RESET_CREDENTIALS:
            DEBUG_PRINTLN("[Main] Received reset credentials command");
            lcdController.showMessage("Resetting...", "Please wait");
            delay(1000);
            
            // Clear all stored data
            credentialManager.clearAll();
            
            // Restart ESP32
            DEBUG_PRINTLN("[Main] Restarting...");
            ESP.restart();
            break;
            
        default:
            break;
    }
}

void onProvisioningResponse(bool success, const String& message) {
    DEBUG_PRINTF("[Main] Provisioning response: %s - %s\n", 
                 success ? "SUCCESS" : "FAILED", message.c_str());
    
    if (success) {
        // Get provisioned IDs and save them
        ComponentIds ids;
        if (mqttController.getProvisionedIds(ids)) {
            credentialManager.saveComponentIds(ids);
            ultrasonicController.setSensorIds(ids.sensor1Id, ids.sensor2Id);
            
            lcdController.showProvisioningComplete();
            delay(1500);
            
            currentState = SystemState::RUNNING;
        }
    } else {
        lcdController.showError("Prov. Failed");
        delay(2000);
        // Will retry in loop
    }
}

// ============================================================================
// SETUP
// ============================================================================

void setup() {
    // Initialize serial
    Serial.begin(DEBUG_BAUD_RATE);
    delay(100);
    
    DEBUG_PRINTLN("\n\n========================================");
    DEBUG_PRINTLN("ESP32 Car Parking System");
    DEBUG_PRINTLN("========================================\n");
    
    // Initialize credential manager
    credentialManager.begin();
    
    // Initialize LCD first for user feedback
    lcdController.begin();
    lcdController.showBootMessage();
    delay(1000);
    
    // Initialize all controllers
    rfidController.begin();
    servoController.begin();
    irController.begin();
    ultrasonicController.begin();
    wifiController.begin();
    mqttController.begin();
    
    // Set MQTT callbacks
    mqttController.setCommandCallback(onMQTTCommand);
    mqttController.setProvisioningCallback(onProvisioningResponse);
    
    // Check if we have stored credentials
    if (credentialManager.hasValidCredentials()) {
        DEBUG_PRINTLN("[Main] Found stored credentials, attempting connection...");
        currentState = SystemState::CONNECTING_WIFI;
    } else {
        DEBUG_PRINTLN("[Main] No stored credentials, starting captive portal...");
        currentState = SystemState::CAPTIVE_PORTAL;
    }
}

// ============================================================================
// MAIN LOOP
// ============================================================================

void loop() {
    unsigned long currentMillis = millis();
    
    switch (currentState) {
        // ====================================================================
        // CAPTIVE PORTAL STATE
        // ====================================================================
        case SystemState::CAPTIVE_PORTAL: {
            lcdController.showCaptivePortal(WIFI_AP_NAME);
            
            Credentials creds;
            if (wifiController.startCaptivePortal(creds)) {
                // Save credentials
                credentialManager.saveCredentials(creds);
                
                lcdController.showWiFiConnected(wifiController.getIPAddress());
                delay(1500);
                
                currentState = SystemState::CONNECTING_MQTT;
            } else {
                // Portal timed out, restart
                lcdController.showError("Portal Timeout");
                delay(2000);
                ESP.restart();
            }
            break;
        }
        
        // ====================================================================
        // CONNECTING WIFI STATE
        // ====================================================================
        case SystemState::CONNECTING_WIFI: {
            lcdController.showConnecting();
            
            Credentials creds;
            if (credentialManager.loadCredentials(creds)) {
                if (wifiController.connect(creds)) {
                    lcdController.showWiFiConnected(wifiController.getIPAddress());
                    delay(1500);
                    currentState = SystemState::CONNECTING_MQTT;
                } else {
                    // WiFi connection failed, clear credentials and restart portal
                    lcdController.showError("WiFi Failed");
                    delay(2000);
                    credentialManager.clearCredentials();
                    currentState = SystemState::CAPTIVE_PORTAL;
                }
            } else {
                currentState = SystemState::CAPTIVE_PORTAL;
            }
            break;
        }
        
        // ====================================================================
        // CONNECTING MQTT STATE
        // ====================================================================
        case SystemState::CONNECTING_MQTT: {
            lcdController.showMQTTConnecting();
            
            Credentials creds;
            if (credentialManager.loadCredentials(creds)) {
                if (mqttController.connect(creds)) {
                    // Check if we already have component IDs
                    if (credentialManager.hasValidComponentIds()) {
                        DEBUG_PRINTLN("[Main] Found stored component IDs");
                        
                        ComponentIds ids;
                        credentialManager.loadComponentIds(ids);
                        ultrasonicController.setSensorIds(ids.sensor1Id, ids.sensor2Id);
                        
                        currentState = SystemState::RUNNING;
                    } else {
                        DEBUG_PRINTLN("[Main] No component IDs, starting provisioning...");
                        currentState = SystemState::PROVISIONING;
                        provisioningStartTime = currentMillis;
                    }
                } else {
                    lcdController.showError("MQTT Failed");
                    delay(2000);
                    // Retry connection
                }
            }
            break;
        }
        
        // ====================================================================
        // PROVISIONING STATE
        // ====================================================================
        case SystemState::PROVISIONING: {
            lcdController.showWaitingForServer();
            
            // Send provisioning request
            static unsigned long lastProvisioningRequest = 0;
            if (currentMillis - lastProvisioningRequest > 5000 || lastProvisioningRequest == 0) {
                mqttController.publishProvisioningRequest();
                lastProvisioningRequest = currentMillis;
            }
            
            // Process MQTT messages
            mqttController.loop();
            
            // Check if provisioning is complete (callback will update state)
            if (mqttController.isProvisioningComplete()) {
                // State will be updated by callback
                break;
            }
            
            // Check for timeout
            if (currentMillis - provisioningStartTime > PROVISIONING_TIMEOUT_MS) {
                lcdController.showError("Prov. Timeout");
                delay(2000);
                // Retry provisioning
                provisioningStartTime = currentMillis;
            }
            break;
        }
        
        // ====================================================================
        // RUNNING STATE (Normal Operation)
        // ====================================================================
        case SystemState::RUNNING: {
            // Process WiFi and MQTT
            wifiController.loop();
            mqttController.loop();
            
            // Check WiFi connection
            if (!wifiController.isConnected()) {
                DEBUG_PRINTLN("[Main] WiFi disconnected!");
                // WiFiController will attempt reconnection in its loop
            }
            
            // Check MQTT connection
            if (!mqttController.isConnected()) {
                DEBUG_PRINTLN("[Main] MQTT disconnected!");
                // MQTTController will attempt reconnection in its loop
            }
            
            // Process servo timeouts
            servoController.loop();
            
            // ------------------------------------------------------------
            // RFID Scanning
            // ------------------------------------------------------------
            if (currentMillis - lastRfidScan >= RFID_SCAN_INTERVAL_MS) {
                lastRfidScan = currentMillis;
                
                // Scan entry RFID
                if (!entryGateWaitingForCommand && !servoController.isGateOpen(GateId::ENTRY)) {
                    String entryCard = rfidController.scanEntry();
                    if (entryCard.length() > 0) {
                        DEBUG_PRINTF("[Main] Entry card scanned: %s\n", entryCard.c_str());
                        
                        // Send entry request to server
                        mqttController.publishEntryRequest(entryCard);
                        entryGateWaitingForCommand = true;
                        pendingEntryRfid = entryCard;
                    }
                }
                
                // Scan exit RFID
                if (!exitGateWaitingForCommand && !servoController.isGateOpen(GateId::EXIT)) {
                    String exitCard = rfidController.scanExit();
                    if (exitCard.length() > 0) {
                        DEBUG_PRINTF("[Main] Exit card scanned: %s\n", exitCard.c_str());
                        
                        // Send exit request to server
                        mqttController.publishExitRequest(exitCard);
                        exitGateWaitingForCommand = true;
                        pendingExitRfid = exitCard;
                    }
                }
            }
            
            // ------------------------------------------------------------
            // Gate Control with IR Sensors
            // ------------------------------------------------------------
            // Entry gate: keep open while car is passing
            if (servoController.isGateOpen(GateId::ENTRY)) {
                bool carAtEntry = irController.isCarAtEntry();
                unsigned long openDuration = servoController.getGateOpenDuration(GateId::ENTRY);
                
                // Close gate if no car detected and some time has passed
                if (!carAtEntry && openDuration > 1000) {
                    DEBUG_PRINTLN("[Main] Entry gate: car passed, closing");
                    servoController.closeGate(GateId::ENTRY);
                }
                // Note: 30s timeout is handled by ServoController
            }
            
            // Exit gate: keep open while car is passing
            if (servoController.isGateOpen(GateId::EXIT)) {
                bool carAtExit = irController.isCarAtExit();
                unsigned long openDuration = servoController.getGateOpenDuration(GateId::EXIT);
                
                // Close gate if no car detected and some time has passed
                if (!carAtExit && openDuration > 1000) {
                    DEBUG_PRINTLN("[Main] Exit gate: car passed, closing");
                    servoController.closeGate(GateId::EXIT);
                }
                // Note: 30s timeout is handled by ServoController
            }
            
            // ------------------------------------------------------------
            // Ultrasonic Sensors (Parking Slots)
            // ------------------------------------------------------------
            if (currentMillis - lastUltrasonicScan >= ULTRASONIC_SCAN_INTERVAL_MS) {
                lastUltrasonicScan = currentMillis;
                
                // Check slots and see if any state changed
                bool stateChanged = ultrasonicController.checkSlots();
                
                if (stateChanged) {
                    // Update LCD with new slot count
                    int available = ultrasonicController.getAvailableSlots();
                    int total = ultrasonicController.getTotalSlots();
                    lcdController.updateSlots(available, total);
                    
                    // Publish sensor status for changed slot
                    int changedSlot = ultrasonicController.getLastChangedSlot();
                    if (changedSlot >= 0) {
                        int sensorId = ultrasonicController.getSensorId(changedSlot);
                        bool isOccupied = ultrasonicController.isOccupied(changedSlot);
                        
                        if (sensorId > 0) {
                            mqttController.publishSensorStatus(sensorId, isOccupied);
                        }
                    }
                }
            }
            
            // ------------------------------------------------------------
            // Initial LCD Update (on first run)
            // ------------------------------------------------------------
            static bool initialLcdUpdate = false;
            if (!initialLcdUpdate) {
                initialLcdUpdate = true;
                ultrasonicController.checkSlots(); // Initial scan
                int available = ultrasonicController.getAvailableSlots();
                int total = ultrasonicController.getTotalSlots();
                lcdController.updateSlots(available, total);
            }
            
            break;
        }
        
        // ====================================================================
        // ERROR STATE
        // ====================================================================
        case SystemState::ERROR: {
            lcdController.showError("System Error");
            delay(5000);
            ESP.restart();
            break;
        }
        
        default:
            currentState = SystemState::ERROR;
            break;
    }
}