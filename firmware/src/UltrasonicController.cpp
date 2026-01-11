#include "UltrasonicController.h"
#include "config.h"

// Global instance
UltrasonicController ultrasonicController;

// Pin definitions for each sensor
static const int trigPins[TOTAL_PARKING_SLOTS] = {US_SLOT1_TRIG, US_SLOT2_TRIG};
static const int echoPins[TOTAL_PARKING_SLOTS] = {US_SLOT1_ECHO, US_SLOT2_ECHO};

UltrasonicController::UltrasonicController() 
    : _lastChangedSlot(-1)
    , _initialized(false) {
    for (int i = 0; i < TOTAL_PARKING_SLOTS; i++) {
        _slotOccupied[i] = false;
        _previousState[i] = false;
        _sensorIds[i] = -1;
    }
}

void UltrasonicController::begin() {
    // Configure pins for each ultrasonic sensor
    for (int i = 0; i < TOTAL_PARKING_SLOTS; i++) {
        pinMode(trigPins[i], OUTPUT);
        pinMode(echoPins[i], INPUT);
        digitalWrite(trigPins[i], LOW);
    }
    
    _initialized = true;
    
    DEBUG_PRINTLN("[UltrasonicController] Initialized");
    DEBUG_PRINTF("[UltrasonicController] Total slots: %d\n", TOTAL_PARKING_SLOTS);
    DEBUG_PRINTF("[UltrasonicController] Detection threshold: %d cm\n", ULTRASONIC_THRESHOLD_CM);
}

bool UltrasonicController::checkSlots() {
    if (!_initialized) return false;
    
    bool anyStateChanged = false;
    _lastChangedSlot = -1;
    
    for (int i = 0; i < TOTAL_PARKING_SLOTS; i++) {
        // Save previous state
        _previousState[i] = _slotOccupied[i];
        
        // Measure distance
        float distance = measureDistance(trigPins[i], echoPins[i]);
        
        // Determine if slot is occupied
        // Occupied if distance is less than threshold (and valid reading)
        if (distance > 0 && distance < ULTRASONIC_THRESHOLD_CM) {
            _slotOccupied[i] = true;
        } else if (distance >= ULTRASONIC_THRESHOLD_CM) {
            _slotOccupied[i] = false;
        }
        // If distance is invalid (0 or very large), keep previous state
        
        // Check if state changed
        if (_slotOccupied[i] != _previousState[i]) {
            anyStateChanged = true;
            _lastChangedSlot = i;
            
            DEBUG_PRINTF("[UltrasonicController] Slot %d state changed: %s (distance: %.1f cm)\n",
                         i,
                         _slotOccupied[i] ? "OCCUPIED" : "AVAILABLE",
                         distance);
        }
    }
    
    return anyStateChanged;
}

float UltrasonicController::measureDistance(int trigPin, int echoPin) {
    // Ensure trigger is low
    digitalWrite(trigPin, LOW);
    delayMicroseconds(2);
    
    // Send 10us pulse
    digitalWrite(trigPin, HIGH);
    delayMicroseconds(10);
    digitalWrite(trigPin, LOW);
    
    // Read echo pulse duration (timeout after 30ms = ~5m max distance)
    unsigned long duration = pulseIn(echoPin, HIGH, 30000);
    
    // Calculate distance in cm
    // Speed of sound = 343 m/s = 0.0343 cm/us
    // Distance = (duration * 0.0343) / 2
    if (duration == 0) {
        return -1; // No echo received (timeout or error)
    }
    
    float distance = (duration * 0.0343) / 2.0;
    
    return distance;
}

int UltrasonicController::getAvailableSlots() {
    int available = 0;
    for (int i = 0; i < TOTAL_PARKING_SLOTS; i++) {
        if (!_slotOccupied[i]) {
            available++;
        }
    }
    return available;
}

int UltrasonicController::getTotalSlots() {
    return TOTAL_PARKING_SLOTS;
}

bool UltrasonicController::isOccupied(int slotIndex) {
    if (slotIndex < 0 || slotIndex >= TOTAL_PARKING_SLOTS) {
        return false;
    }
    return _slotOccupied[slotIndex];
}

int UltrasonicController::getLastChangedSlot() {
    return _lastChangedSlot;
}

int UltrasonicController::getSensorId(int slotIndex) {
    if (slotIndex < 0 || slotIndex >= TOTAL_PARKING_SLOTS) {
        return -1;
    }
    return _sensorIds[slotIndex];
}

void UltrasonicController::setSensorIds(int sensor1Id, int sensor2Id) {
    _sensorIds[0] = sensor1Id;
    _sensorIds[1] = sensor2Id;
    
    DEBUG_PRINTF("[UltrasonicController] Sensor IDs set: Slot 0 = %d, Slot 1 = %d\n",
                 sensor1Id, sensor2Id);
}

float UltrasonicController::getDistance(int slotIndex) {
    if (slotIndex < 0 || slotIndex >= TOTAL_PARKING_SLOTS) {
        return -1;
    }
    return measureDistance(trigPins[slotIndex], echoPins[slotIndex]);
}
