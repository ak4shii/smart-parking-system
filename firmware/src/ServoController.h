#ifndef SERVO_CONTROLLER_H
#define SERVO_CONTROLLER_H

#include <Arduino.h>

// Gate identifiers
enum class GateId {
    ENTRY = 0,
    EXIT = 1
};

class ServoController {
public:
    ServoController();
    
    // Initialize servo motors
    void begin();
    
    // Open entry gate (rotates left to 90°)
    void openEntryGate();
    
    // Open exit gate (rotates right to 90°)
    void openExitGate();
    
    // Close a specific gate
    void closeGate(GateId gate);
    
    // Close all gates
    void closeAllGates();
    
    // Check if gate is open
    bool isGateOpen(GateId gate);
    
    // Get time since gate was opened (for timeout)
    unsigned long getGateOpenDuration(GateId gate);
    
    // Process servo updates (call in loop for smooth motion)
    void loop();
    
private:
    void setGatePosition(GateId gate, int angle);
    
    bool _entryOpen;
    bool _exitOpen;
    unsigned long _entryOpenTime;
    unsigned long _exitOpenTime;
};

// Global instance
extern ServoController servoController;

#endif // SERVO_CONTROLLER_H
