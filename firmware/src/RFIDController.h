#ifndef RFID_CONTROLLER_H
#define RFID_CONTROLLER_H

#include <Arduino.h>

class RFIDController {
public:
    RFIDController();
    
    // Initialize both RFID readers
    void begin();
    
    // Scan entry gate RFID reader
    // Returns UID as hex string, or empty string if no card
    String scanEntry();
    
    // Scan exit gate RFID reader
    // Returns UID as hex string, or empty string if no card
    String scanExit();
    
    // Check if entry reader is available
    bool isEntryReaderAvailable();
    
    // Check if exit reader is available
    bool isExitReaderAvailable();
    
private:
    String readCard(int readerIndex);
    String uidToString(byte* uid, byte size);
    
    bool _entryAvailable;
    bool _exitAvailable;
};

// Global instance
extern RFIDController rfidController;

#endif // RFID_CONTROLLER_H
