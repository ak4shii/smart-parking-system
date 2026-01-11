#include "ServoController.h"
#include "config.h"
#include <ESP32Servo.h>

// Global instance
ServoController servoController;

// Servo instances
static Servo servoEntry;
static Servo servoExit;

ServoController::ServoController()
    : _entryOpen(false)
    , _exitOpen(false)
    , _entryOpenTime(0)
    , _exitOpenTime(0) {
}

void ServoController::begin() {
    // Allow allocation of all timers for servos
    ESP32PWM::allocateTimer(0);
    ESP32PWM::allocateTimer(1);
    ESP32PWM::allocateTimer(2);
    ESP32PWM::allocateTimer(3);
    
    // Attach servos with standard pulse width range
    servoEntry.setPeriodHertz(50);
    servoEntry.attach(SERVO_ENTRY_PIN, 500, 2400);
    
    servoExit.setPeriodHertz(50);
    servoExit.attach(SERVO_EXIT_PIN, 500, 2400);
    
    // Initialize to closed position
    closeAllGates();
    
    DEBUG_PRINTLN("[ServoController] Initialized");
}

void ServoController::openEntryGate() {
    if (_entryOpen) return;
    
    // Entry gate rotates left (0 to 90)
    setGatePosition(GateId::ENTRY, SERVO_ENTRY_OPEN_POS);
    _entryOpen = true;
    _entryOpenTime = millis();
    
    DEBUG_PRINTLN("[ServoController] Entry gate opened");
}

void ServoController::openExitGate() {
    if (_exitOpen) return;
    
    // Exit gate rotates right (0 to -90, represented as 180-90=90 in opposite direction)
    // For opposite rotation, we use 180 - angle
    setGatePosition(GateId::EXIT, 180 - SERVO_EXIT_OPEN_POS);
    _exitOpen = true;
    _exitOpenTime = millis();
    
    DEBUG_PRINTLN("[ServoController] Exit gate opened");
}

void ServoController::closeGate(GateId gate) {
    if (gate == GateId::ENTRY) {
        setGatePosition(GateId::ENTRY, SERVO_CLOSED_POS);
        _entryOpen = false;
        _entryOpenTime = 0;
        DEBUG_PRINTLN("[ServoController] Entry gate closed");
    } else {
        // Exit gate closes to 180 (opposite of entry)
        setGatePosition(GateId::EXIT, 180);
        _exitOpen = false;
        _exitOpenTime = 0;
        DEBUG_PRINTLN("[ServoController] Exit gate closed");
    }
}

void ServoController::closeAllGates() {
    closeGate(GateId::ENTRY);
    closeGate(GateId::EXIT);
}

bool ServoController::isGateOpen(GateId gate) {
    return (gate == GateId::ENTRY) ? _entryOpen : _exitOpen;
}

unsigned long ServoController::getGateOpenDuration(GateId gate) {
    if (gate == GateId::ENTRY) {
        return _entryOpen ? (millis() - _entryOpenTime) : 0;
    } else {
        return _exitOpen ? (millis() - _exitOpenTime) : 0;
    }
}

void ServoController::setGatePosition(GateId gate, int angle) {
    // Clamp angle to valid range
    angle = constrain(angle, 0, 180);
    
    if (gate == GateId::ENTRY) {
        servoEntry.write(angle);
    } else {
        servoExit.write(angle);
    }
}

void ServoController::loop() {
    // Check for gate timeout
    if (_entryOpen && getGateOpenDuration(GateId::ENTRY) > GATE_MAX_OPEN_TIME_MS) {
        DEBUG_PRINTLN("[ServoController] Entry gate timeout - forcing close");
        closeGate(GateId::ENTRY);
    }
    
    if (_exitOpen && getGateOpenDuration(GateId::EXIT) > GATE_MAX_OPEN_TIME_MS) {
        DEBUG_PRINTLN("[ServoController] Exit gate timeout - forcing close");
        closeGate(GateId::EXIT);
    }
}
