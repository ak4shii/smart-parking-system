#include "RFIDController.h"
#include "config.h"
#include <SPI.h>
#include <MFRC522.h>

// Global instance
RFIDController rfidController;

// RFID reader instances
static MFRC522 rfidEntry(RFID_ENTRY_SS, RFID_ENTRY_RST);
static MFRC522 rfidExit(RFID_EXIT_SS, RFID_EXIT_RST);

// Mutex for SPI bus access
static SemaphoreHandle_t spiMutex = NULL;

// Reader indices
#define READER_ENTRY    0
#define READER_EXIT     1

RFIDController::RFIDController() 
    : _entryAvailable(false)
    , _exitAvailable(false) {
}

void RFIDController::begin() {
    // Create SPI mutex
    if (spiMutex == NULL) {
        spiMutex = xSemaphoreCreateMutex();
    }
    
    // Initialize SPI bus
    SPI.begin(SPI_SCK, SPI_MISO, SPI_MOSI);
    
    // Initialize entry RFID reader
    rfidEntry.PCD_Init();
    delay(10);
    
    // Check if entry reader is responding
    byte version = rfidEntry.PCD_ReadRegister(MFRC522::VersionReg);
    if (version == 0x00 || version == 0xFF) {
        DEBUG_PRINTLN("[RFIDController] Entry reader not found or not responding");
        _entryAvailable = false;
    } else {
        _entryAvailable = true;
        DEBUG_PRINTF("[RFIDController] Entry reader initialized (version: 0x%02X)\n", version);
    }
    
    // Initialize exit RFID reader
    rfidExit.PCD_Init();
    delay(10);
    
    // Check if exit reader is responding
    version = rfidExit.PCD_ReadRegister(MFRC522::VersionReg);
    if (version == 0x00 || version == 0xFF) {
        DEBUG_PRINTLN("[RFIDController] Exit reader not found or not responding");
        _exitAvailable = false;
    } else {
        _exitAvailable = true;
        DEBUG_PRINTF("[RFIDController] Exit reader initialized (version: 0x%02X)\n", version);
    }
    
    DEBUG_PRINTLN("[RFIDController] Initialized");
}

String RFIDController::scanEntry() {
    return readCard(READER_ENTRY);
}

String RFIDController::scanExit() {
    return readCard(READER_EXIT);
}

String RFIDController::readCard(int readerIndex) {
    String result = "";
    
    // Acquire SPI mutex
    if (xSemaphoreTake(spiMutex, pdMS_TO_TICKS(100)) != pdTRUE) {
        DEBUG_PRINTLN("[RFIDController] Failed to acquire SPI mutex");
        return result;
    }
    
    MFRC522* reader = (readerIndex == READER_ENTRY) ? &rfidEntry : &rfidExit;
    bool available = (readerIndex == READER_ENTRY) ? _entryAvailable : _exitAvailable;
    
    if (!available) {
        xSemaphoreGive(spiMutex);
        return result;
    }
    
    // Check for new card
    if (!reader->PICC_IsNewCardPresent()) {
        xSemaphoreGive(spiMutex);
        return result;
    }
    
    // Read card serial
    if (!reader->PICC_ReadCardSerial()) {
        xSemaphoreGive(spiMutex);
        return result;
    }
    
    // Convert UID to string
    result = uidToString(reader->uid.uidByte, reader->uid.size);
    
    DEBUG_PRINTF("[RFIDController] Card detected on %s reader: %s\n", 
                 (readerIndex == READER_ENTRY) ? "entry" : "exit",
                 result.c_str());
    
    // Halt PICC
    reader->PICC_HaltA();
    reader->PCD_StopCrypto1();
    
    // Release mutex
    xSemaphoreGive(spiMutex);
    
    return result;
}

String RFIDController::uidToString(byte* uid, byte size) {
    String result = "";
    for (byte i = 0; i < size; i++) {
        if (uid[i] < 0x10) {
            result += "0";
        }
        result += String(uid[i], HEX);
    }
    result.toUpperCase();
    return result;
}

bool RFIDController::isEntryReaderAvailable() {
    return _entryAvailable;
}

bool RFIDController::isExitReaderAvailable() {
    return _exitAvailable;
}
