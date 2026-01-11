#ifndef ULTRASONIC_CONTROLLER_H
#define ULTRASONIC_CONTROLLER_H

#include <Arduino.h>
#include "config.h"

// Forward declaration
class LCDController;

class UltrasonicController {
public:
    UltrasonicController();
    
    // Initialize ultrasonic sensors
    void begin();
    
    // Check all slots and return true if any state changed
    bool checkSlots();
    
    // Get number of available (unoccupied) slots
    int getAvailableSlots();
    
    // Get total number of slots
    int getTotalSlots();
    
    // Check if specific slot is occupied
    bool isOccupied(int slotIndex);
    
    // Get the last changed slot index (-1 if none)
    int getLastChangedSlot();
    
    // Get the sensor ID for a slot (assigned during provisioning)
    int getSensorId(int slotIndex);
    
    // Set sensor IDs (from provisioning)
    void setSensorIds(int sensor1Id, int sensor2Id);
    
    // Get distance reading for a slot (in cm)
    float getDistance(int slotIndex);
    
private:
    float measureDistance(int trigPin, int echoPin);
    
    bool _slotOccupied[TOTAL_PARKING_SLOTS];
    bool _previousState[TOTAL_PARKING_SLOTS];
    int _sensorIds[TOTAL_PARKING_SLOTS];
    int _lastChangedSlot;
    bool _initialized;
};

// Global instance
extern UltrasonicController ultrasonicController;

#endif // ULTRASONIC_CONTROLLER_H
