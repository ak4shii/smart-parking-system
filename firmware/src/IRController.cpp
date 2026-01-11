#include "IRController.h"
#include "config.h"

// Global instance
IRController irController;

IRController::IRController() : _initialized(false) {
}

void IRController::begin() {
    // Configure IR sensor pins as inputs
    // Note: GPIO 34 and 35 are input-only pins on ESP32
    pinMode(IR_ENTRY_PIN, INPUT);
    pinMode(IR_EXIT_PIN, INPUT);
    
    _initialized = true;
    
    DEBUG_PRINTLN("[IRController] Initialized");
    DEBUG_PRINTF("[IRController] Entry IR on GPIO %d\n", IR_ENTRY_PIN);
    DEBUG_PRINTF("[IRController] Exit IR on GPIO %d\n", IR_EXIT_PIN);
}

bool IRController::isCarAtEntry() {
    if (!_initialized) return false;
    
    // IR obstacle sensors typically output LOW when object is detected
    // and HIGH when no object (inverted logic)
    bool detected = (digitalRead(IR_ENTRY_PIN) == LOW);
    
    return detected;
}

bool IRController::isCarAtExit() {
    if (!_initialized) return false;
    
    // IR obstacle sensors typically output LOW when object is detected
    bool detected = (digitalRead(IR_EXIT_PIN) == LOW);
    
    return detected;
}

int IRController::getEntryRawValue() {
    return digitalRead(IR_ENTRY_PIN);
}

int IRController::getExitRawValue() {
    return digitalRead(IR_EXIT_PIN);
}
