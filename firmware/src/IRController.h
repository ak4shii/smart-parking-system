#ifndef IR_CONTROLLER_H
#define IR_CONTROLLER_H

#include <Arduino.h>

class IRController {
public:
    IRController();
    
    // Initialize IR sensors
    void begin();
    
    // Check if car is detected at entry gate
    bool isCarAtEntry();
    
    // Check if car is detected at exit gate
    bool isCarAtExit();
    
    // Get raw sensor reading (for debugging)
    int getEntryRawValue();
    int getExitRawValue();
    
private:
    bool _initialized;
};

// Global instance
extern IRController irController;

#endif // IR_CONTROLLER_H
