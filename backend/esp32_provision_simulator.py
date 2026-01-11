#!/usr/bin/env python3
"""
ESP32 Provision Simulator for Smart Parking System
Simulates the complete provision flow when a parking space is created via UI.

Usage:
    python esp32_provision_simulator.py

Author: Smart Parking System
"""

import paho.mqtt.client as mqtt
import json
import time
import random
import sys
from datetime import datetime, timezone
from typing import Dict, List

# ANSI Colors for beautiful output
class Colors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'

def print_header(text):
    print(f"\n{Colors.HEADER}{Colors.BOLD}{'='*60}{Colors.ENDC}")
    print(f"{Colors.HEADER}{Colors.BOLD}{text:^60}{Colors.ENDC}")
    print(f"{Colors.HEADER}{Colors.BOLD}{'='*60}{Colors.ENDC}\n")

def print_step(step_num, text):
    print(f"{Colors.OKCYAN}[STEP {step_num}]{Colors.ENDC} {text}")

def print_success(text):
    print(f"{Colors.OKGREEN}✅ {text}{Colors.ENDC}")

def print_error(text):
    print(f"{Colors.FAIL}❌ {text}{Colors.ENDC}")

def print_info(text):
    print(f"{Colors.OKBLUE}ℹ️  {text}{Colors.ENDC}")

def print_warning(text):
    print(f"{Colors.WARNING}⚠️  {text}{Colors.ENDC}")


class ESP32Device:
    """Simulates an ESP32 microcontroller for Smart Parking System"""
    
    def __init__(self, mqtt_credentials: Dict):
        """
        Initialize ESP32 device with MQTT credentials
        
        Args:
            mqtt_credentials: Dict containing:
                - mqttHost
                - mqttPort
                - mqttUsername
                - mqttPassword
                - baseTopic
                - mcCode
                - deviceName
        """
        self.mqtt_host = mqtt_credentials.get('mqttHost', 'localhost')
        self.mqtt_port = mqtt_credentials.get('mqttPort', 1883)
        self.mqtt_username = mqtt_credentials['mqttUsername']
        self.mqtt_password = mqtt_credentials['mqttPassword']
        self.base_topic = mqtt_credentials['baseTopic']
        self.mc_code = mqtt_credentials['mcCode']
        self.device_name = mqtt_credentials['deviceName']
        
        self.client = None
        self.connected = False
        self.uptime = 0
        self.message_count = 0
        
        # Simulate sensor configuration
        self.sensors = [
            {"id": "slot1", "type": "ultrasonic", "pin": 12},
            {"id": "slot2", "type": "ultrasonic", "pin": 14},
            {"id": "slot3", "type": "infrared", "pin": 27},
        ]
        
        # Simulate RFID cards
        self.rfid_cards = [
            {"code": "RFID001", "owner": "John Doe"},
            {"code": "RFID002", "owner": "Jane Smith"},
            {"code": "RFID003", "owner": "Bob Johnson"},
        ]
        
        # Track door states 
        self.door_states = {"entry": False, "exit": False}  # False=closed, True=open
        
        # LCD display state
        self.lcd_display = "Welcome!"
        
        print_header("ESP32 DEVICE INITIALIZED")
        print(f"Device Name:  {Colors.BOLD}{self.device_name}{Colors.ENDC}")
        print(f"Device Code:  {Colors.BOLD}{self.mc_code}{Colors.ENDC}")
        print(f"MQTT Host:    {self.mqtt_host}:{self.mqtt_port}")
        print(f"MQTT User:    {self.mqtt_username}")
        print(f"Base Topic:   {self.base_topic}")
        print(f"Sensors:      {len(self.sensors)} configured")
        print(f"RFID Cards:   {len(self.rfid_cards)} registered")
    
    def on_connect(self, client, userdata, flags, rc):
        """Callback when connected to MQTT broker"""
        if rc == 0:
            self.connected = True
            print_success(f"Connected to MQTT Broker (RC: {rc})")
            
            # Subscribe to command topics
            cmd_topic = f"{self.base_topic}/cmd/#"
            client.subscribe(cmd_topic)
            print_info(f"Subscribed to: {cmd_topic}")
            
            # Subscribe to door/camera/lcd commands from backend
            command_topic = f"{self.base_topic}/command"
            camera_topic = f"{self.base_topic}/camera"
            lcd_topic = f"{self.base_topic}/lcd"
            client.subscribe(command_topic)
            client.subscribe(camera_topic)
            client.subscribe(lcd_topic)
            print_info(f"Subscribed to: {command_topic}")
            print_info(f"Subscribed to: {camera_topic}")
            print_info(f"Subscribed to: {lcd_topic}")
            
            # Subscribe to provision response
            provision_response_topic = f"{self.base_topic}/provision/response"
            client.subscribe(provision_response_topic)
            
            # Publish birth message
            self._publish_birth_message()
            
            # Send provision request
            self._publish_provision_request()
            
            # Publish initial status
            self._publish_status()
            
        else:
            print_error(f"Connection failed (RC: {rc})")
            if rc == 1:
                print_error("   → Incorrect protocol version")
            elif rc == 2:
                print_error("   → Invalid client identifier")
            elif rc == 3:
                print_error("   → Server unavailable")
            elif rc == 4:
                print_error("   → Bad username or password")
            elif rc == 5:
                print_error("   → Not authorized - Check credentials!")
    
    def on_disconnect(self, client, userdata, rc):
        """Callback when disconnected from MQTT broker"""
        self.connected = False
        if rc != 0:
            print_warning(f"Unexpected disconnection (RC: {rc})")
        else:
            print_info("Disconnected gracefully")
    
    def on_message(self, client, userdata, msg):
        """Callback when message received"""
        self.message_count += 1
        
        # Check if it's a provision response
        if "/provision/response" in msg.topic:
            print(f"\n{Colors.OKGREEN}📦 PROVISION RESPONSE RECEIVED{Colors.ENDC}")
            print(f"   Topic: {msg.topic}")
            try:
                response = json.loads(msg.payload.decode())
                if response.get('success'):
                    print_success("Provision completed successfully!")
                    doors = response.get('doors', [])
                    lcds = response.get('lcds', [])
                    sensors = response.get('sensors', [])
                    print_info(f"   Created: {len(sensors)} sensors, {len(doors)} doors, {len(lcds)} LCDs")
                    
                    # Update local sensors with DB IDs
                    for db_sensor in sensors:
                        name = db_sensor.get('name', '') # e.g. Sensor-SLOT1
                        # Extract slot name (SLOT1)
                        if name.startswith('Sensor-'):
                            slot_name = name.replace('Sensor-', '').lower() # slot1
                            # Find local sensor
                            for local_sensor in self.sensors:
                                if local_sensor['id'] == slot_name:
                                    local_sensor['db_id'] = db_sensor['id']
                                    print_info(f"   Mapped {local_sensor['id']} -> DB ID: {local_sensor['db_id']}")

                else:
                    print_error(f"Provision failed: {response.get('message', 'Unknown error')}")
            except json.JSONDecodeError:
                print_error("   Invalid JSON in provision response")
            return
        
        # Handle door command from backend
        if msg.topic.endswith("/command"):
            try:
                cmd = json.loads(msg.payload.decode())
                self._handle_door_command(cmd)
            except json.JSONDecodeError:
                print_error("Invalid JSON in door command")
            return
        
        # Handle camera command from backend
        if msg.topic.endswith("/camera"):
            try:
                cmd = json.loads(msg.payload.decode())
                self._handle_camera_command(cmd)
            except json.JSONDecodeError:
                print_error("Invalid JSON in camera command")
            return
        
        # Handle LCD command from backend
        if msg.topic.endswith("/lcd"):
            try:
                cmd = json.loads(msg.payload.decode())
                self._handle_lcd_command(cmd)
            except json.JSONDecodeError:
                print_error("Invalid JSON in LCD command")
            return
        
        # Handle other commands (cmd/#)
        print(f"\n{Colors.OKCYAN}📨 COMMAND RECEIVED{Colors.ENDC}")
        print(f"   Topic:   {msg.topic}")
        print(f"   Payload: {msg.payload.decode()}")
        
        try:
            cmd = json.loads(msg.payload.decode())
            self._handle_command(cmd)
        except json.JSONDecodeError:
            print_error("   Invalid JSON payload")
    
    def on_publish(self, client, userdata, mid):
        """Callback when message published"""
        # Silent by default, can enable for debugging
        pass
    
    def _publish_birth_message(self):
        """Publish device birth message (first message on connect)"""
        topic = f"{self.base_topic}/birth"
        payload = {
            "device": self.mc_code,
            "deviceName": self.device_name,
            "status": "online",
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "ip": "192.168.1.100",
            "mac": "AA:BB:CC:DD:EE:FF",
            "firmwareVersion": "1.0.0",
            "sensors": self.sensors
        }
        self.client.publish(topic, json.dumps(payload), retain=True)
        print_success("Published birth message")
    
    def _publish_provision_request(self):
        """Publish provision request to auto-create slots, sensors, doors, LCDs"""
        topic = f"{self.base_topic}/provision/request"
        
        # Convert sensor configs to provision format  
        sensors_provision = []
        for sensor in self.sensors:
            slot_name = sensor['id'].upper()  # slot1 -> SLOT1
            sensors_provision.append({
                "name": f"Sensor-{slot_name}",
                "slotName": slot_name,
                "type": sensor['type']
            })
        
        payload = {
            "doors": [
                {"name": "Entry_Gate"},
                {"name": "Exit_Gate"}
            ],
            "lcds": [
                {"name": "Main_Display"}
            ],
            "sensors": sensors_provision
        }
        
        self.client.publish(topic, json.dumps(payload))
        print_success("Published provision request")
        print_info(f"   → Requesting {len(sensors_provision)} sensors, 2 doors, 1 LCD")
    
    def _publish_status(self):
        """Publish device status"""
        topic = f"{self.base_topic}/status"
        payload = {
            "device": self.mc_code,
            "status": "online",
            "uptime": self.uptime,
            "freeHeap": random.randint(50000, 100000),
            "rssi": random.randint(-70, -40),
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
        self.client.publish(topic, json.dumps(payload), retain=True)
    
    def _publish_sensor_data(self):
        """Publish sensor readings"""
        topic = f"{self.base_topic}/sensor/status" # Fixed topic from /sensors to /sensor/status
        
        readings = []
        for sensor in self.sensors:
            # Only publish if we have a DB ID
            if 'db_id' not in sensor:
                continue

            if sensor['type'] == 'ultrasonic':
                # Distance in cm (10-200cm)
                distance = random.randint(10, 200)
                occupied = distance < 50  # Car detected if < 50cm
                
                # Payload matching MqttSensorStatusDto
                payload = {
                    "sensorId": sensor['db_id'],
                    "isOccupied": occupied
                }
                
                # Publish individual message per sensor as per Handler expectation?
                # Handler expects topic: sps/{mqttUsername}/sensor/status
                # And payload MqttSensorStatusDto (single object)
                # Simulator loop was aggregating.
                # If Handler expects single object, we must publish individually.
                
                self.client.publish(topic, json.dumps(payload))
                
                readings.append({
                    "id": sensor['id'],
                    "distance": distance,
                    "occupied": occupied
                })

            elif sensor['type'] == 'infrared':
                # Binary detection (0 or 1)
                occupied = random.choice([0, 1])
                
                payload = {
                    "sensorId": sensor['db_id'],
                    "isOccupied": bool(occupied)
                }
                self.client.publish(topic, json.dumps(payload))
                
                readings.append({
                    "id": sensor['id'],
                    "occupied": bool(occupied)
                })
        
        if readings:
            # Pretty print sensor data
            print(f"\n{Colors.OKGREEN}📊 SENSOR DATA PUBLISHED{Colors.ENDC}")
            for reading in readings:
                status = "🚗 OCCUPIED" if reading['occupied'] else "🅿️  EMPTY"
                if 'distance' in reading:
                    print(f"   {reading['id']}: {reading['distance']}cm - {status}")
                else:
                    print(f"   {reading['id']}: {status}")
    
    def _handle_command(self, cmd: Dict):
        """Handle incoming commands from backend"""
        cmd_type = cmd.get('command', 'unknown')
        
        if cmd_type == 'ping':
            print_info("   → Responding to PING")
            self._respond_pong()
        
        elif cmd_type == 'restart':
            print_warning("   → Restart command received")
            self._simulate_restart()
        
        elif cmd_type == 'calibrate':
            print_info("   → Calibrating sensors")
            self._calibrate_sensors()
        
        elif cmd_type == 'update_config':
            print_info("   → Updating configuration")
            config = cmd.get('config', {})
            print(f"      New config: {json.dumps(config, indent=2)}")
        
        else:
            print_warning(f"   → Unknown command: {cmd_type}")
    
    def _handle_door_command(self, cmd: Dict):
        """Handle door control command from backend"""
        command_type = cmd.get('commandType', '')  # 'entry' or 'exit'
        command = cmd.get('command', '')  # 'open' or 'close'
        
        print(f"\n{Colors.OKGREEN}🚪 DOOR COMMAND RECEIVED{Colors.ENDC}")
        print(f"   Type: {command_type}")
        print(f"   Command: {command}")
        
        if command == 'open':
            self.door_states[command_type] = True
            print_success(f"   → Opening {command_type} door...")
            # Simulate door motor
            time.sleep(0.5)
            print_success(f"   → {command_type.upper()} door is now OPEN")
            
            # Auto-close after 5 seconds (simulated)
            print_info(f"   → Door will auto-close in 5s")
        elif command == 'close':
            self.door_states[command_type] = False
            print_info(f"   → Closing {command_type} door...")
    
    def _handle_camera_command(self, cmd: Dict):
        """Handle camera command from backend (for license plate capture)"""
        command_type = cmd.get('commandType', '')
        command = cmd.get('command', '')
        rfid_code = cmd.get('rfidCode', '')
        
        print(f"\n{Colors.OKGREEN}📷 CAMERA COMMAND RECEIVED{Colors.ENDC}")
        print(f"   Type: {command_type}")
        print(f"   Command: {command}")
        print(f"   RFID: {rfid_code}")
        
        if command == 'snap':
            print_info("   → Capturing license plate image...")
            time.sleep(0.3)
            # Simulate captured plate
            plate = f"51A-{random.randint(100,999)}.{random.randint(10,99)}"
            print_success(f"   → Captured: {plate}")
            
            # Note: In real implementation, ESP32 would use camera module
            # and send image to backend for OCR processing
    
    def _handle_lcd_command(self, cmd: Dict):
        """Handle LCD display command from backend"""
        lcd_id = cmd.get('lcdId', 1)
        display_text = cmd.get('displayText', '')
        
        print(f"\n{Colors.OKGREEN}📺 LCD COMMAND RECEIVED{Colors.ENDC}")
        print(f"   LCD ID: {lcd_id}")
        print(f"   Display: {display_text}")
        
        self.lcd_display = display_text
        print_success(f"   → LCD updated: [{display_text}]")
    
    def _simulate_rfid_entry(self):
        """Simulate RFID card scan at entry gate"""
        # Pick random RFID card
        card = random.choice(self.rfid_cards)
        
        print(f"\n{Colors.WARNING}🔖 RFID CARD SCANNED (Entry){Colors.ENDC}")
        print(f"   Card: {card['code']}")
        print(f"   Owner: {card['owner']}")
        
        # Publish entry request to backend
        topic = f"{self.base_topic}/entry/request"
        payload = {
            "rfidCode": card['code']
        }
        self.client.publish(topic, json.dumps(payload))
        print_success(f"   → Published entry request")
        print_info("   → Waiting for backend response (door command)...")
    
    def _simulate_rfid_exit(self):
        """Simulate RFID card scan at exit gate"""
        # Pick random RFID card
        card = random.choice(self.rfid_cards)
        
        print(f"\n{Colors.WARNING}🔖 RFID CARD SCANNED (Exit){Colors.ENDC}")
        print(f"   Card: {card['code']}")
        print(f"   Owner: {card['owner']}")
        
        # Publish exit request to backend
        topic = f"{self.base_topic}/exit/request"
        payload = {
            "rfidCode": card['code']
        }
        self.client.publish(topic, json.dumps(payload))
        print_success(f"   → Published exit request")
        print_info("   → Waiting for backend response (door command)...")
    
    def _respond_pong(self):
        """Respond to ping command"""
        topic = f"{self.base_topic}/pong"
        payload = {
            "device": self.mc_code,
            "response": "pong",
            "uptime": self.uptime,
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
        self.client.publish(topic, json.dumps(payload))
    
    def _simulate_restart(self):
        """Simulate device restart"""
        print_warning("   → Simulating restart in 3 seconds...")
        time.sleep(1)
        print_warning("   → 2...")
        time.sleep(1)
        print_warning("   → 1...")
        time.sleep(1)
        
        # Disconnect and reconnect
        self.client.disconnect()
        time.sleep(2)
        self.uptime = 0
        self.connect()
    
    def _calibrate_sensors(self):
        """Simulate sensor calibration"""
        for i, sensor in enumerate(self.sensors):
            print(f"      Calibrating {sensor['id']}... ", end='')
            time.sleep(0.5)
            print("✅")
        
        # Publish calibration result
        topic = f"{self.base_topic}/calibration"
        payload = {
            "device": self.mc_code,
            "status": "completed",
            "sensors": self.sensors,
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
        self.client.publish(topic, json.dumps(payload))
    
    def connect(self):
        """Connect to MQTT broker"""
        print_step(1, "Connecting to MQTT Broker...")
        
        # Create MQTT client
        client_id = f"esp32-{self.mc_code}-{int(time.time())}"
        self.client = mqtt.Client(client_id=client_id)
        
        # Set credentials
        self.client.username_pw_set(self.mqtt_username, self.mqtt_password)
        
        # Set callbacks
        self.client.on_connect = self.on_connect
        self.client.on_disconnect = self.on_disconnect
        self.client.on_message = self.on_message
        self.client.on_publish = self.on_publish
        
        # Set last will (offline message)
        will_topic = f"{self.base_topic}/status"
        will_payload = json.dumps({
            "device": self.mc_code,
            "status": "offline",
            "timestamp": datetime.now(timezone.utc).isoformat()
        })
        self.client.will_set(will_topic, will_payload, retain=True)
        
        try:
            # Connect to broker
            print_info(f"Connecting to {self.mqtt_host}:{self.mqtt_port}...")
            self.client.connect(self.mqtt_host, self.mqtt_port, 60)
            
            # Start network loop
            self.client.loop_start()
            
            # Wait for connection
            timeout = 5
            start_time = time.time()
            while not self.connected and (time.time() - start_time) < timeout:
                time.sleep(0.1)
            
            if not self.connected:
                print_error("Connection timeout!")
                return False
            
            return True
            
        except Exception as e:
            print_error(f"Connection error: {str(e)}")
            return False
    
    def run_loop(self, duration=60, interval=5):
        """
        Main loop - simulate ESP32 behavior
        
        Args:
            duration: Total duration in seconds (default: 60)
            interval: Publish interval in seconds (default: 5)
        """
        if not self.connected:
            print_error("Not connected to MQTT broker!")
            return
        
        print_step(2, f"Starting main loop (duration: {duration}s, interval: {interval}s)")
        print_info("Press Ctrl+C to stop")
        print_info("RFID simulation will trigger every 15 seconds\n")
        
        start_time = time.time()
        last_publish = 0
        last_rfid = 0
        rfid_interval = 15  # Simulate RFID scan every 15 seconds
        rfid_type = "entry"  # Alternate between entry and exit
        
        try:
            while (time.time() - start_time) < duration:
                current_time = time.time() - start_time
                self.uptime = int(current_time)
                
                # Publish sensor data at interval
                if current_time - last_publish >= interval:
                    self._publish_sensor_data()
                    self._publish_status()
                    last_publish = current_time
                
                # Simulate RFID scan at interval
                if current_time - last_rfid >= rfid_interval:
                    if rfid_type == "entry":
                        self._simulate_rfid_entry()
                        rfid_type = "exit"
                    else:
                        self._simulate_rfid_exit()
                        rfid_type = "entry"
                    last_rfid = current_time
                
                time.sleep(0.5)
            
            print_success(f"\nLoop completed ({duration}s)")
            
        except KeyboardInterrupt:
            print_warning("\n\nStopping device...")
    
    def disconnect(self):
        """Disconnect from MQTT broker"""
        print_step(3, "Disconnecting from MQTT Broker...")
        
        if self.client:
            # Publish goodbye message
            topic = f"{self.base_topic}/status"
            payload = json.dumps({
                "device": self.mc_code,
                "status": "offline",
                "reason": "shutdown",
                "timestamp": datetime.now(timezone.utc).isoformat()
            })
            self.client.publish(topic, payload, retain=True)
            
            time.sleep(0.5)
            
            # Stop loop and disconnect
            self.client.loop_stop()
            self.client.disconnect()
        
        print_success("Disconnected successfully")


def main():
    """Main entry point"""
    print_header("ESP32 PROVISION SIMULATOR")
    print(f"{Colors.BOLD}Smart Parking System - Device Simulator{Colors.ENDC}\n")
    
    # Prompt for MQTT credentials
    print(f"{Colors.WARNING}📋 Please enter MQTT credentials from the UI:{Colors.ENDC}")
    print(f"{Colors.OKBLUE}   (Create parking space → Copy credentials from dialog){Colors.ENDC}\n")
    
    try:
        mqtt_username = input(f"{Colors.BOLD}MQTT Username:{Colors.ENDC} ").strip()
        mqtt_password = input(f"{Colors.BOLD}MQTT Password:{Colors.ENDC} ").strip()
        
        if not mqtt_username or not mqtt_password:
            print_error("Username and password are required!")
            sys.exit(1)
        
        # Extract mcCode from username (format: owner_mcCode)
        if '_' in mqtt_username:
            mc_code = mqtt_username.split('_')[-1]
        else:
            mc_code = "UNKNOWN"
        
        # Build credentials dict
        mqtt_credentials = {
            'mqttHost': 'localhost',
            'mqttPort': 1883,
            'mqttUsername': mqtt_username,
            'mqttPassword': mqtt_password,
            'baseTopic': f'sps/{mqtt_username}',
            'mcCode': mc_code,
            'deviceName': f'ESP32-{mc_code}'
        }
        
        # Create and run device
        device = ESP32Device(mqtt_credentials)
        
        if device.connect():
            # Run for 60 seconds, publish every 5 seconds
            device.run_loop(duration=60, interval=5)
            device.disconnect()
        else:
            print_error("Failed to connect to MQTT broker!")
            sys.exit(1)
        
        print_header("SIMULATION COMPLETED")
        print_success("ESP32 provision flow simulated successfully!")
        
    except KeyboardInterrupt:
        print_warning("\n\nInterrupted by user")
        if 'device' in locals():
            device.disconnect()
    except Exception as e:
        print_error(f"Error: {str(e)}")
        sys.exit(1)


if __name__ == "__main__":
    main()
