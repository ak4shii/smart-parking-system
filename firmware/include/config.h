#ifndef CONFIG_H
#define CONFIG_H

// ============================================================================
// PIN DEFINITIONS
// ============================================================================

// ===== SPI Bus (Shared by RFID readers) =====
#define SPI_SCK     18
#define SPI_MISO    19
#define SPI_MOSI    23

// ===== RFID Entry Gate =====
#define RFID_ENTRY_SS   5
#define RFID_ENTRY_RST  17

// ===== RFID Exit Gate =====
#define RFID_EXIT_SS    14
#define RFID_EXIT_RST   27

// ===== I2C Bus (LCD) =====
#define I2C_SDA     21
#define I2C_SCL     22
#define LCD_I2C_ADDR    0x27    // Common address for WC1602 (try 0x3F if not working)
#define LCD_COLS    16
#define LCD_ROWS    2

// ===== Servo Motors =====
#define SERVO_ENTRY_PIN  15
#define SERVO_EXIT_PIN   13

// ===== IR Sensors (Vehicle Detection at Gates) =====
#define IR_ENTRY_PIN    35      // Input-only pin
#define IR_EXIT_PIN     34     // Input-only pin

// ===== Ultrasonic Sensors (Parking Slot Detection) =====
#define US_SLOT1_TRIG   32
#define US_SLOT1_ECHO   33
#define US_SLOT2_TRIG   25
#define US_SLOT2_ECHO   26

// ============================================================================
// SYSTEM CONFIGURATION
// ============================================================================

// Ultrasonic sensor threshold for occupied detection (in cm)
#define ULTRASONIC_THRESHOLD_CM     10

// Maximum time gate stays open (in milliseconds)
#define GATE_MAX_OPEN_TIME_MS       30000

// Servo positions (degrees)
#define SERVO_CLOSED_POS            0
#define SERVO_ENTRY_OPEN_POS        90      // Rotates left for entry
#define SERVO_EXIT_OPEN_POS         90      // Rotates right for exit

// Status heartbeat interval (in milliseconds)
#define STATUS_HEARTBEAT_INTERVAL_MS    5000

// RFID scan interval (in milliseconds)
#define RFID_SCAN_INTERVAL_MS       200

// Ultrasonic scan interval (in milliseconds)
#define ULTRASONIC_SCAN_INTERVAL_MS 500

// Total parking slots (number of ultrasonic sensors)
#define TOTAL_PARKING_SLOTS         2

// ============================================================================
// NVS STORAGE NAMESPACES AND KEYS
// ============================================================================

// Namespaces (max 15 chars)
#define NVS_NAMESPACE_CREDENTIALS   "credentials"
#define NVS_NAMESPACE_COMPONENT_IDS "component_ids"

// Credential keys
#define NVS_KEY_WIFI_SSID           "wifi_ssid"
#define NVS_KEY_WIFI_PASS           "wifi_pass"
#define NVS_KEY_MQTT_SERVER         "mqtt_server"
#define NVS_KEY_MQTT_PORT           "mqtt_port"
#define NVS_KEY_MQTT_USER           "mqtt_user"
#define NVS_KEY_MQTT_PASS           "mqtt_pass"
#define NVS_KEY_CREDS_VALID         "creds_valid"

// Component ID keys
#define NVS_KEY_ENTRY_DOOR_ID       "entry_door_id"
#define NVS_KEY_EXIT_DOOR_ID        "exit_door_id"
#define NVS_KEY_LCD_ID              "lcd_id"
#define NVS_KEY_SENSOR1_ID          "sensor1_id"
#define NVS_KEY_SENSOR2_ID          "sensor2_id"
#define NVS_KEY_IDS_VALID           "ids_valid"

// ============================================================================
// COMPONENT NAMES (for provisioning)
// ============================================================================

#define COMPONENT_NAME_ENTRY_DOOR   "Entry Door"
#define COMPONENT_NAME_EXIT_DOOR    "Exit Door"
#define COMPONENT_NAME_LCD          "Display 1"
#define COMPONENT_NAME_SENSOR1      "Slot Sensor A1"
#define COMPONENT_NAME_SENSOR2      "Slot Sensor A2"

#define COMPONENT_SLOT_NAME_1       "A1"
#define COMPONENT_SLOT_NAME_2       "A2"

// ============================================================================
// MQTT TOPICS (relative to base topic sps/<mqtt_username>/)
// ============================================================================

#define MQTT_TOPIC_STATUS           "status"
#define MQTT_TOPIC_COMMAND          "command"
#define MQTT_TOPIC_ENTRY_REQUEST    "entry/request"
#define MQTT_TOPIC_EXIT_REQUEST     "exit/request"
#define MQTT_TOPIC_SENSOR_STATUS    "sensor/status"
#define MQTT_TOPIC_PROVISION_REQ    "provision/request"
#define MQTT_TOPIC_PROVISION_RESP   "provision/response"

// ============================================================================
// WIFI MANAGER CONFIGURATION
// ============================================================================

#define WIFI_AP_NAME                "ParkingSystem-Setup"
#define WIFI_AP_PASSWORD            "parking123"
#define WIFI_CONFIG_TIMEOUT_SEC     180

// ============================================================================
// DEBUG CONFIGURATION
// ============================================================================

#define DEBUG_SERIAL                Serial
#define DEBUG_BAUD_RATE             115200

#ifdef CORE_DEBUG_LEVEL
    #if CORE_DEBUG_LEVEL > 0
        #define DEBUG_PRINT(x)      DEBUG_SERIAL.print(x)
        #define DEBUG_PRINTLN(x)    DEBUG_SERIAL.println(x)
        #define DEBUG_PRINTF(...)   DEBUG_SERIAL.printf(__VA_ARGS__)
    #else
        #define DEBUG_PRINT(x)
        #define DEBUG_PRINTLN(x)
        #define DEBUG_PRINTF(...)
    #endif
#else
    #define DEBUG_PRINT(x)          DEBUG_SERIAL.print(x)
    #define DEBUG_PRINTLN(x)        DEBUG_SERIAL.println(x)
    #define DEBUG_PRINTF(...)       DEBUG_SERIAL.printf(__VA_ARGS__)
#endif

#endif // CONFIG_H
