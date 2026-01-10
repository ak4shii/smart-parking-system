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
from datetime import datetime
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
        
        print_header("ESP32 DEVICE INITIALIZED")
        print(f"Device Name:  {Colors.BOLD}{self.device_name}{Colors.ENDC}")
        print(f"Device Code:  {Colors.BOLD}{self.mc_code}{Colors.ENDC}")
        print(f"MQTT Host:    {self.mqtt_host}:{self.mqtt_port}")
        print(f"MQTT User:    {self.mqtt_username}")
        print(f"Base Topic:   {self.base_topic}")
        print(f"Sensors:      {len(self.sensors)} configured")
    
    def on_connect(self, client, userdata, flags, rc):
        """Callback when connected to MQTT broker"""
        if rc == 0:
            self.connected = True
            print_success(f"Connected to MQTT Broker (RC: {rc})")
            
            # Subscribe to command topics
            cmd_topic = f"{self.base_topic}/cmd/#"
            client.subscribe(cmd_topic)
            print_info(f"Subscribed to: {cmd_topic}")
            
            # Publish birth message
            self._publish_birth_message()
            
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
            "timestamp": datetime.utcnow().isoformat(),
            "ip": "192.168.1.100",
            "mac": "AA:BB:CC:DD:EE:FF",
            "firmwareVersion": "1.0.0",
            "sensors": self.sensors
        }
        self.client.publish(topic, json.dumps(payload), retain=True)
        print_success("Published birth message")
    
    def _publish_status(self):
        """Publish device status"""
        topic = f"{self.base_topic}/status"
        payload = {
            "device": self.mc_code,
            "status": "online",
            "uptime": self.uptime,
            "freeHeap": random.randint(50000, 100000),
            "rssi": random.randint(-70, -40),
            "timestamp": datetime.utcnow().isoformat()
        }
        self.client.publish(topic, json.dumps(payload), retain=True)
    
    def _publish_sensor_data(self):
        """Publish sensor readings"""
        topic = f"{self.base_topic}/sensors"
        
        readings = []
        for sensor in self.sensors:
            if sensor['type'] == 'ultrasonic':
                # Distance in cm (10-200cm)
                distance = random.randint(10, 200)
                occupied = distance < 50  # Car detected if < 50cm
                readings.append({
                    "id": sensor['id'],
                    "type": sensor['type'],
                    "distance": distance,
                    "occupied": occupied
                })
            elif sensor['type'] == 'infrared':
                # Binary detection (0 or 1)
                occupied = random.choice([0, 1])
                readings.append({
                    "id": sensor['id'],
                    "type": sensor['type'],
                    "occupied": bool(occupied)
                })
        
        payload = {
            "device": self.mc_code,
            "sensors": readings,
            "timestamp": datetime.utcnow().isoformat()
        }
        
        self.client.publish(topic, json.dumps(payload))
        
        # Pretty print sensor data
        print(f"\n{Colors.OKGREEN}📊 SENSOR DATA PUBLISHED{Colors.ENDC}")
        for reading in readings:
            if reading['type'] == 'ultrasonic':
                status = "🚗 OCCUPIED" if reading['occupied'] else "🅿️  EMPTY"
                print(f"   {reading['id']}: {reading['distance']}cm - {status}")
            else:
                status = "🚗 OCCUPIED" if reading['occupied'] else "🅿️  EMPTY"
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
    
    def _respond_pong(self):
        """Respond to ping command"""
        topic = f"{self.base_topic}/pong"
        payload = {
            "device": self.mc_code,
            "response": "pong",
            "uptime": self.uptime,
            "timestamp": datetime.utcnow().isoformat()
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
            "timestamp": datetime.utcnow().isoformat()
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
            "timestamp": datetime.utcnow().isoformat()
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
        print_info("Press Ctrl+C to stop\n")
        
        start_time = time.time()
        last_publish = 0
        
        try:
            while (time.time() - start_time) < duration:
                current_time = time.time() - start_time
                self.uptime = int(current_time)
                
                # Publish sensor data at interval
                if current_time - last_publish >= interval:
                    self._publish_sensor_data()
                    self._publish_status()
                    last_publish = current_time
                
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
                "timestamp": datetime.utcnow().isoformat()
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
