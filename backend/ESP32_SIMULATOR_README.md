# ESP32 Provision Simulator

Mô phỏng quá trình provision của ESP32 device sau khi tạo parking space trong UI.

## 📋 Prerequisites

```bash
pip install paho-mqtt
```

## 🚀 Usage

### Step 1: Create Parking Space in UI

1. Mở UI: http://localhost:5173/admin
2. Click "Create New Parking Space"
3. Điền thông tin:
   - **Parking Space Name**: Test Parking
   - **Location**: 123 Test Street
   - **MC Code**: MC-TEST-001
   - **MC Name**: Test Controller
4. Complete wizard
5. **MQTT Credentials Dialog sẽ hiện** → **COPY CREDENTIALS!**

### Step 2: Run Simulator

```bash
cd backend
python esp32_provision_simulator.py
```

### Step 3: Enter Credentials

```
📋 Please enter MQTT credentials from the UI:

MQTT Username: hungtq1234@test.com_MC-TEST-001
MQTT Password: xR9tL2mK8vN4wQ7pC3bF1gH5jS6nM0yD
```

### Step 4: Watch Magic Happen! ✨

```
============================================================
               ESP32 DEVICE INITIALIZED
============================================================

Device Name:  ESP32-MC-TEST-001
Device Code:  MC-TEST-001
MQTT Host:    localhost:1883
MQTT User:    hungtq1234@test.com_MC-TEST-001
Base Topic:   sps/hungtq1234@test.com_MC-TEST-001
Sensors:      3 configured

[STEP 1] Connecting to MQTT Broker...
ℹ️  Connecting to localhost:1883...
✅ Connected to MQTT Broker (RC: 0)
ℹ️  Subscribed to: sps/hungtq1234@test.com_MC-TEST-001/cmd/#
✅ Published birth message

[STEP 2] Starting main loop (duration: 60s, interval: 5s)
ℹ️  Press Ctrl+C to stop

📊 SENSOR DATA PUBLISHED
   slot1: 45cm - 🅿️  EMPTY
   slot2: 12cm - 🚗 OCCUPIED
   slot3: 🅿️  EMPTY

📊 SENSOR DATA PUBLISHED
   slot1: 167cm - 🅿️  EMPTY
   slot2: 89cm - 🅿️  EMPTY
   slot3: 🚗 OCCUPIED
...
```

## 📡 What The Simulator Does

### 1. **Device Initialization**
- ✅ Parse MQTT credentials
- ✅ Configure sensors (3 sensors: 2 ultrasonic + 1 infrared)
- ✅ Setup MQTT client

### 2. **MQTT Connection**
- ✅ Connect to broker with credentials
- ✅ Subscribe to command topics (`sps/{username}/cmd/#`)
- ✅ Set last will message (offline status)

### 3. **Birth Message**
Publish once on connect:
```json
{
  "device": "MC-TEST-001",
  "deviceName": "ESP32-MC-TEST-001",
  "status": "online",
  "timestamp": "2026-01-10T12:30:00Z",
  "ip": "192.168.1.100",
  "mac": "AA:BB:CC:DD:EE:FF",
  "firmwareVersion": "1.0.0",
  "sensors": [...]
}
```
**Topic**: `sps/{username}/birth`

### 4. **Sensor Data Publishing**
Every 5 seconds:
```json
{
  "device": "MC-TEST-001",
  "sensors": [
    {
      "id": "slot1",
      "type": "ultrasonic",
      "distance": 45,
      "occupied": false
    },
    {
      "id": "slot2",
      "type": "ultrasonic",
      "distance": 12,
      "occupied": true
    },
    {
      "id": "slot3",
      "type": "infrared",
      "occupied": false
    }
  ],
  "timestamp": "2026-01-10T12:30:05Z"
}
```
**Topic**: `sps/{username}/sensors`

### 5. **Status Updates**
Every 5 seconds:
```json
{
  "device": "MC-TEST-001",
  "status": "online",
  "uptime": 25,
  "freeHeap": 87432,
  "rssi": -55,
  "timestamp": "2026-01-10T12:30:25Z"
}
```
**Topic**: `sps/{username}/status` (retained)

### 6. **Command Handling**
Listen on: `sps/{username}/cmd/#`

**Supported commands:**

#### PING
```bash
# Publish ping command
docker exec -it sps-mosquitto mosquitto_pub \
  -h localhost -p 1883 \
  -u "hungtq1234@test.com_MC-TEST-001" \
  -P "your_password" \
  -t "sps/hungtq1234@test.com_MC-TEST-001/cmd/ping" \
  -m '{"command":"ping"}'

# Device responds with PONG
```

#### RESTART
```bash
# Restart device
mosquitto_pub ... \
  -t "sps/hungtq1234@test.com_MC-TEST-001/cmd/restart" \
  -m '{"command":"restart"}'
```

#### CALIBRATE
```bash
# Calibrate sensors
mosquitto_pub ... \
  -t "sps/hungtq1234@test.com_MC-TEST-001/cmd/calibrate" \
  -m '{"command":"calibrate"}'
```

#### UPDATE CONFIG
```bash
# Update configuration
mosquitto_pub ... \
  -t "sps/hungtq1234@test.com_MC-TEST-001/cmd/config" \
  -m '{"command":"update_config","config":{"interval":10}}'
```

## 🎯 Testing Scenarios

### Scenario 1: Normal Operation
```bash
# Run simulator for 60 seconds
python esp32_provision_simulator.py

# Let it run and publish sensor data
# Press Ctrl+C after testing
```

### Scenario 2: Monitor with mosquitto_sub
```bash
# Terminal 1: Run simulator
python esp32_provision_simulator.py

# Terminal 2: Subscribe to all topics
docker exec -it sps-mosquitto mosquitto_sub \
  -h localhost -p 1883 \
  -u "hungtq1234@test.com_MC-TEST-001" \
  -P "your_password" \
  -t "sps/hungtq1234@test.com_MC-TEST-001/#" \
  -v
```

### Scenario 3: Send Commands
```bash
# Terminal 1: Run simulator
python esp32_provision_simulator.py

# Terminal 2: Send ping
docker exec -it sps-mosquitto mosquitto_pub \
  -h localhost -p 1883 \
  -u "hungtq1234@test.com_MC-TEST-001" \
  -P "your_password" \
  -t "sps/hungtq1234@test.com_MC-TEST-001/cmd/ping" \
  -m '{"command":"ping"}'

# Simulator will respond with PONG!
```

### Scenario 4: Test Regenerate Credentials
```bash
# 1. Start simulator with old credentials
python esp32_provision_simulator.py
# (connected and running)

# 2. In UI: Click "Regenerate" on DevicesPage

# 3. Simulator will:
#    - Stay connected (existing connection)
#    - Try to reconnect if disconnected → FAIL (old creds)

# 4. Stop simulator (Ctrl+C)

# 5. Restart with NEW credentials
python esp32_provision_simulator.py
# (enter new password)
# → Should connect successfully!
```

## 📊 Output Example

```
============================================================
               ESP32 DEVICE INITIALIZED
============================================================

Device Name:  ESP32-MC-TEST-001
Device Code:  MC-TEST-001
MQTT Host:    localhost:1883
MQTT User:    hungtq1234@test.com_MC-TEST-001
Base Topic:   sps/hungtq1234@test.com_MC-TEST-001
Sensors:      3 configured

[STEP 1] Connecting to MQTT Broker...
ℹ️  Connecting to localhost:1883...
✅ Connected to MQTT Broker (RC: 0)
ℹ️  Subscribed to: sps/hungtq1234@test.com_MC-TEST-001/cmd/#
✅ Published birth message

[STEP 2] Starting main loop (duration: 60s, interval: 5s)
ℹ️  Press Ctrl+C to stop

📊 SENSOR DATA PUBLISHED
   slot1: 156cm - 🅿️  EMPTY
   slot2: 23cm - 🚗 OCCUPIED
   slot3: 🅿️  EMPTY

📨 COMMAND RECEIVED
   Topic:   sps/hungtq1234@test.com_MC-TEST-001/cmd/ping
   Payload: {"command":"ping"}
ℹ️     → Responding to PING

📊 SENSOR DATA PUBLISHED
   slot1: 89cm - 🅿️  EMPTY
   slot2: 45cm - 🚗 OCCUPIED
   slot3: 🚗 OCCUPIED

✅ Loop completed (60s)

[STEP 3] Disconnecting from MQTT Broker...
✅ Disconnected successfully

============================================================
               SIMULATION COMPLETED
============================================================
✅ ESP32 provision flow simulated successfully!
```

## 🔍 Troubleshooting

### ❌ Connection Failed (RC: 5)
```
❌ Connection failed (RC: 5)
❌    → Not authorized - Check credentials!
```

**Solution:**
- Verify username and password are correct
- Check `mqtt_enabled = true` in database
- Try regenerating credentials

### ❌ Connection Timeout
```
❌ Connection timeout!
```

**Solution:**
- Check Mosquitto is running: `docker ps | grep mosquitto`
- Check port 1883 is accessible
- Verify Docker network

### ⚠️ Unexpected Disconnection
```
⚠️ Unexpected disconnection (RC: 7)
```

**Solution:**
- Mosquitto might have restarted
- Network interruption
- Check Mosquitto logs: `docker logs sps-mosquitto`

## 📝 Customization

### Change Publish Interval
```python
# In main()
device.run_loop(duration=120, interval=10)
#                         ↑          ↑
#                    2 minutes   every 10s
```

### Add More Sensors
```python
# In ESP32Device.__init__()
self.sensors = [
    {"id": "slot1", "type": "ultrasonic", "pin": 12},
    {"id": "slot2", "type": "ultrasonic", "pin": 14},
    {"id": "slot3", "type": "infrared", "pin": 27},
    {"id": "slot4", "type": "ultrasonic", "pin": 26},  # NEW
]
```

### Change MQTT Host
```python
# When creating credentials dict
mqtt_credentials = {
    'mqttHost': '192.168.1.100',  # Change to your host
    'mqttPort': 1883,
    # ...
}
```

## 🎓 Learning Points

This simulator demonstrates:

1. ✅ **MQTT Connection Flow** - How ESP32 connects with credentials
2. ✅ **Topic Naming** - Proper topic structure (`sps/{username}/...`)
3. ✅ **QoS & Retain** - Birth/status messages with retain flag
4. ✅ **Last Will** - Offline message on unexpected disconnect
5. ✅ **Command Pattern** - Subscribe to commands, publish responses
6. ✅ **Sensor Simulation** - Realistic sensor data generation
7. ✅ **Error Handling** - Connection failures, timeouts, auth errors

## 🚀 Next Steps

After testing with simulator:
1. Flash real ESP32 with actual code
2. Configure WiFi credentials
3. Update MQTT credentials in ESP32
4. Deploy to parking lot  
5. Monitor via frontend UI

---

Happy Testing! 🎉
