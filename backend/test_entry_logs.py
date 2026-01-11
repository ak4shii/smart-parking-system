#!/usr/bin/env python3
"""
Test script to generate 10 entry-exit logs for testing the Activity panel.

Usage:
    python test_entry_logs.py

This script will:
1. Connect to MQTT broker
2. Simulate 10 entry/exit events in sequence
3. Each event has a 2-second delay to see real-time updates
"""

import paho.mqtt.client as mqtt
import json
import time
import random
from datetime import datetime, timezone

# Configuration
MQTT_HOST = 'localhost'
MQTT_PORT = 1883

# ANSI Colors
class Colors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

def print_header(text):
    print(f"\n{Colors.HEADER}{Colors.BOLD}{'='*60}{Colors.ENDC}")
    print(f"{Colors.HEADER}{Colors.BOLD}{text:^60}{Colors.ENDC}")
    print(f"{Colors.HEADER}{Colors.BOLD}{'='*60}{Colors.ENDC}\n")

def print_success(text):
    print(f"{Colors.OKGREEN}✅ {text}{Colors.ENDC}")

def print_info(text):
    print(f"{Colors.OKBLUE}ℹ️  {text}{Colors.ENDC}")

def print_warning(text):
    print(f"{Colors.WARNING}⚠️  {text}{Colors.ENDC}")

def print_error(text):
    print(f"{Colors.FAIL}❌ {text}{Colors.ENDC}")


class EntryLogTester:
    def __init__(self, mqtt_username: str, mqtt_password: str):
        self.mqtt_username = mqtt_username
        self.mqtt_password = mqtt_password
        self.base_topic = f'sps/{mqtt_username}'
        self.client = None
        self.connected = False
        
        # Track active entries (RFID codes that are inside)
        self.active_entries = set()
        
        # RFID cards for testing (3 cards for 3 slots)
        self.rfid_cards = [
            {"code": "RFID001", "owner": "Nguyen Van A"},
            {"code": "RFID002", "owner": "Tran Thi B"},
            {"code": "RFID003", "owner": "Le Van C"},
        ]
        
    def on_connect(self, client, userdata, flags, rc):
        if rc == 0:
            self.connected = True
            print_success(f"Connected to MQTT Broker")
            
            # Subscribe to command topics to see responses
            client.subscribe(f"{self.base_topic}/command")
        else:
            print_error(f"Connection failed (RC: {rc})")
            if rc == 5:
                print_error("   → Not authorized - Check credentials!")
    
    def on_disconnect(self, client, userdata, rc):
        self.connected = False
        if rc != 0:
            print_warning(f"Unexpected disconnection (RC: {rc})")
    
    def on_message(self, client, userdata, msg):
        print(f"\n{Colors.OKCYAN}📨 Response: {msg.topic}{Colors.ENDC}")
        try:
            payload = json.loads(msg.payload.decode())
            print(f"   {json.dumps(payload, indent=2)}")
        except:
            print(f"   {msg.payload.decode()}")
    
    def connect(self):
        print_info(f"Connecting to {MQTT_HOST}:{MQTT_PORT}...")
        
        client_id = f"test-entry-logs-{int(time.time())}"
        self.client = mqtt.Client(client_id=client_id)
        self.client.username_pw_set(self.mqtt_username, self.mqtt_password)
        
        self.client.on_connect = self.on_connect
        self.client.on_disconnect = self.on_disconnect
        self.client.on_message = self.on_message
        
        try:
            self.client.connect(MQTT_HOST, MQTT_PORT, 60)
            self.client.loop_start()
            
            # Wait for connection
            timeout = 5
            start = time.time()
            while not self.connected and (time.time() - start) < timeout:
                time.sleep(0.1)
            
            return self.connected
        except Exception as e:
            print_error(f"Connection error: {str(e)}")
            return False
    
    def simulate_entry(self, rfid_code: str):
        """Simulate vehicle entry"""
        topic = f"{self.base_topic}/entry/request"
        payload = {"rfidCode": rfid_code}
        
        self.client.publish(topic, json.dumps(payload))
        self.active_entries.add(rfid_code)
        print_success(f"ENTRY: {rfid_code}")
    
    def simulate_exit(self, rfid_code: str):
        """Simulate vehicle exit"""
        topic = f"{self.base_topic}/exit/request"
        payload = {"rfidCode": rfid_code}
        
        self.client.publish(topic, json.dumps(payload))
        self.active_entries.discard(rfid_code)
        print_success(f"EXIT:  {rfid_code}")
    
    def run_test(self, num_events: int = 10, delay: float = 2.0):
        """Run the test with specified number of entry/exit events"""
        print_header("STARTING ENTRY LOG TEST")
        print_info(f"Will generate {num_events} events with {delay}s delay each")
        print_info(f"RFID Cards: {[c['code'] for c in self.rfid_cards]}\n")
        
        event_count = 0
        card_index = 0
        
        while event_count < num_events:
            card = self.rfid_cards[card_index % len(self.rfid_cards)]
            rfid_code = card['code']
            
            # Alternate between entry and exit
            if rfid_code not in self.active_entries:
                # Entry
                print(f"\n[Event {event_count + 1}/{num_events}] ", end="")
                self.simulate_entry(rfid_code)
            else:
                # Exit
                print(f"\n[Event {event_count + 1}/{num_events}] ", end="")
                self.simulate_exit(rfid_code)
            
            event_count += 1
            card_index += 1
            
            if event_count < num_events:
                time.sleep(delay)
        
        print_header("TEST COMPLETED")
        print_success(f"Generated {num_events} entry/exit events")
        print_info(f"Active entries remaining: {list(self.active_entries)}")
    
    def disconnect(self):
        if self.client:
            self.client.loop_stop()
            self.client.disconnect()
        print_info("Disconnected from MQTT broker")


def main():
    print_header("ENTRY LOG TEST SCRIPT")
    print(f"{Colors.BOLD}Smart Parking System - Entry Log Tester{Colors.ENDC}\n")
    
    print(f"{Colors.WARNING}📋 Enter MQTT credentials:{Colors.ENDC}\n")
    
    try:
        mqtt_username = input(f"{Colors.BOLD}MQTT Username:{Colors.ENDC} ").strip()
        mqtt_password = input(f"{Colors.BOLD}MQTT Password:{Colors.ENDC} ").strip()
        
        if not mqtt_username or not mqtt_password:
            print_error("Username and password are required!")
            return
        
        # Create tester
        tester = EntryLogTester(mqtt_username, mqtt_password)
        
        if tester.connect():
            # Wait a bit for connection to stabilize
            time.sleep(1)
            
            # Run test: 10 events, 2 second delay
            tester.run_test(num_events=10, delay=2.0)
            
            # Wait to see any responses
            time.sleep(2)
            
            tester.disconnect()
        else:
            print_error("Failed to connect to MQTT broker!")
            
    except KeyboardInterrupt:
        print_warning("\n\nTest interrupted by user")
        if 'tester' in locals():
            tester.disconnect()


if __name__ == "__main__":
    main()
