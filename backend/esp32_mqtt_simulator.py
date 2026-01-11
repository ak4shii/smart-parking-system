#!/usr/bin/env python3
"""
ESP32 MQTT Simulator
Simulates both a normal ESP32 and ESP32-CAM module for smart parking system testing
"""

import paho.mqtt.client as mqtt
import json
import time
import threading
import requests
from pathlib import Path
import sys
import os

class ESP32Simulator:
    def __init__(self):
        self.client = None
        self.mqtt_username = None
        self.mqtt_password = None
        self.base_topic = None
        self.connected = False
        self.uptime_start = None
        self.status_thread = None
        self.stop_status = False
        
        # Device info for provision
        self.device_info = {
            "deviceId": "ESP32_SIM_001",
            "deviceType": "ESP32",
            "firmwareVersion": "1.0.0",
            "macAddress": "AA:BB:CC:DD:EE:FF",
            "ipAddress": "192.168.1.100",
            "features": ["RFID", "SENSOR", "MQTT"]
        }
        
    def on_connect(self, client, userdata, flags, rc):
        """Callback when connected to MQTT broker"""
        if rc == 0:
            print(f"✓ Connected to MQTT broker successfully")
            self.connected = True
            # Subscribe to command topic
            command_topic = f"{self.base_topic}/command"
            client.subscribe(command_topic)
            print(f"✓ Subscribed to {command_topic}")
        else:
            print(f"✗ Connection failed with code {rc}")
            
    def on_disconnect(self, client, userdata, rc):
        """Callback when disconnected from MQTT broker"""
        self.connected = False
        print(f"✗ Disconnected from MQTT broker (code: {rc})")
        
    def on_message(self, client, userdata, msg):
        """Callback when message received"""
        print(f"\n📨 Received message on {msg.topic}:")
        try:
            payload = json.loads(msg.payload.decode())
            print(f"   {json.dumps(payload, indent=2)}")
        except:
            print(f"   {msg.payload.decode()}")
            
    def connect_mqtt(self, broker, port, username, password):
        """Connect to MQTT broker"""
        self.mqtt_username = username
        self.mqtt_password = password
        self.base_topic = f"sps/{username}"
        
        self.client = mqtt.Client(client_id=f"ESP32_SIM_{int(time.time())}")
        self.client.username_pw_set(username, password)
        self.client.on_connect = self.on_connect
        self.client.on_disconnect = self.on_disconnect
        self.client.on_message = self.on_message
        
        try:
            print(f"Connecting to MQTT broker at {broker}:{port}...")
            self.client.connect(broker, port, 60)
            self.client.loop_start()
            time.sleep(1)  # Wait for connection
            return True
        except Exception as e:
            print(f"✗ Failed to connect: {e}")
            return False
            
    def send_provision_request(self):
        """Send provision request (retained message)"""
        if not self.connected:
            print("✗ Not connected to MQTT broker")
            return
            
        topic = f"{self.base_topic}/provision/request"
        payload = json.dumps(self.device_info)
        
        result = self.client.publish(topic, payload, qos=1, retain=True)
        if result.rc == mqtt.MQTT_ERR_SUCCESS:
            print(f"✓ Sent provision request to {topic}")
            print(f"  {json.dumps(self.device_info, indent=2)}")
        else:
            print(f"✗ Failed to send provision request")
            
    def send_rfid_swipe(self, rfid_code):
        """Simulate RFID card swipe"""
        if not self.connected:
            print("✗ Not connected to MQTT broker")
            return
            
        topic = f"{self.base_topic}/exit/request"
        payload = json.dumps({"rfid_code": rfid_code})
        
        result = self.client.publish(topic, payload, qos=1)
        if result.rc == mqtt.MQTT_ERR_SUCCESS:
            print(f"✓ Sent RFID swipe to {topic}")
            print(f"  RFID Code: {rfid_code}")
        else:
            print(f"✗ Failed to send RFID swipe")
            
    def send_status(self):
        """Send status message"""
        if not self.connected:
            return
            
        uptime_sec = int(time.time() - self.uptime_start) if self.uptime_start else 0
        topic = f"{self.base_topic}/status"
        payload = json.dumps({
            "online": True,
            "uptimeSec": uptime_sec
        })
        
        result = self.client.publish(topic, payload, qos=0)
        if result.rc == mqtt.MQTT_ERR_SUCCESS:
            print(f"✓ Sent status: uptime={uptime_sec}s")
            
    def send_sensor_status(self, sensor_id, is_occupied):
        """Send sensor status change"""
        if not self.connected:
            print("✗ Not connected to MQTT broker")
            return
            
        topic = f"{self.base_topic}/sensor/status"
        payload = json.dumps({
            "sensorId": sensor_id,
            "isOccupied": is_occupied
        })
        
        result = self.client.publish(topic, payload, qos=1)
        if result.rc == mqtt.MQTT_ERR_SUCCESS:
            print(f"✓ Sent sensor status to {topic}")
            print(f"  Sensor {sensor_id}: {'Occupied' if is_occupied else 'Free'}")
        else:
            print(f"✗ Failed to send sensor status")
            
    def status_loop(self):
        """Background thread to send status every 10 seconds"""
        while not self.stop_status:
            if self.connected:
                self.send_status()
            time.sleep(10)
            
    def start_status_updates(self):
        """Start automatic status updates"""
        if self.status_thread and self.status_thread.is_alive():
            print("✓ Status updates already running")
            return
            
        self.uptime_start = time.time()
        self.stop_status = False
        self.status_thread = threading.Thread(target=self.status_loop, daemon=True)
        self.status_thread.start()
        print("✓ Started automatic status updates (every 10 seconds)")
        
    def stop_status_updates(self):
        """Stop automatic status updates"""
        self.stop_status = True
        if self.status_thread:
            self.status_thread.join(timeout=1)
        print("✓ Stopped automatic status updates")
        
    def disconnect(self):
        """Disconnect from MQTT broker"""
        self.stop_status_updates()
        if self.client:
            self.client.loop_stop()
            self.client.disconnect()
        print("✓ Disconnected from MQTT broker")


class ESP32CamSimulator:
    def __init__(self):
        self.client = None
        self.mqtt_username = None
        self.base_topic = None
        self.connected = False
        self.http_endpoint = None
        
    def on_connect(self, client, userdata, flags, rc):
        """Callback when connected to MQTT broker"""
        if rc == 0:
            print(f"✓ ESP32-CAM connected to MQTT broker")
            self.connected = True
            # Subscribe to camera topic
            camera_topic = f"{self.base_topic}/camera"
            client.subscribe(camera_topic)
            print(f"✓ Subscribed to {camera_topic}")
        else:
            print(f"✗ ESP32-CAM connection failed with code {rc}")
            
    def on_disconnect(self, client, userdata, rc):
        """Callback when disconnected from MQTT broker"""
        self.connected = False
        print(f"✗ ESP32-CAM disconnected from MQTT broker")
        
    def on_message(self, client, userdata, msg):
        """Callback when camera command received"""
        print(f"\n📷 ESP32-CAM received message on {msg.topic}:")
        try:
            payload = json.loads(msg.payload.decode())
            print(f"   {json.dumps(payload, indent=2)}")
            
            # Check if it's a camera snap command
            if (payload.get("commandtype") == "camera" and 
                payload.get("command") == "snap"):
                rfid_code = payload.get("rfidCode", "UNKNOWN")
                self.capture_and_send_image(rfid_code)
        except Exception as e:
            print(f"   Error processing message: {e}")
            
    def connect_mqtt(self, broker, port, username, password):
        """Connect to MQTT broker"""
        self.mqtt_username = username
        self.base_topic = f"sps/{username}"
        
        self.client = mqtt.Client(client_id=f"ESP32CAM_SIM_{int(time.time())}")
        self.client.username_pw_set(username, password)
        self.client.on_connect = self.on_connect
        self.client.on_disconnect = self.on_disconnect
        self.client.on_message = self.on_message
        
        try:
            print(f"ESP32-CAM connecting to MQTT broker at {broker}:{port}...")
            self.client.connect(broker, port, 60)
            self.client.loop_start()
            time.sleep(1)
            return True
        except Exception as e:
            print(f"✗ ESP32-CAM failed to connect: {e}")
            return False
            
    def capture_and_send_image(self, rfid_code):
        """Simulate capturing and sending image to HTTP endpoint"""
        print(f"\n📸 Simulating image capture for RFID: {rfid_code}")
        
        # Look for existing image files in the same directory
        script_dir = Path(__file__).parent
        image_path = None
        image_extensions = ['.jpg', '.jpeg', '.png', '.JPG', '.JPEG', '.PNG']
        
        # Search for any image file
        for ext in image_extensions:
            for img_file in script_dir.glob(f'*{ext}'):
                if img_file.is_file():
                    image_path = img_file
                    print(f"  Found image: {image_path.name}")
                    break
            if image_path:
                break
        
        # If no image found, create a minimal test image
        if not image_path:
            image_path = script_dir / "test_image.jpg"
            # Create a minimal JPEG (1x1 pixel red image)
            jpeg_data = bytes([
                0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46,
                0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
                0xFF, 0xDB, 0x00, 0x43, 0x00, 0x08, 0x06, 0x06, 0x07, 0x06,
                0x05, 0x08, 0x07, 0x07, 0x07, 0x09, 0x09, 0x08, 0x0A, 0x0C,
                0x14, 0x0D, 0x0C, 0x0B, 0x0B, 0x0C, 0x19, 0x12, 0x13, 0x0F,
                0x14, 0x1D, 0x1A, 0x1F, 0x1E, 0x1D, 0x1A, 0x1C, 0x1C, 0x20,
                0x24, 0x2E, 0x27, 0x20, 0x22, 0x2C, 0x23, 0x1C, 0x1C, 0x28,
                0x37, 0x29, 0x2C, 0x30, 0x31, 0x34, 0x34, 0x34, 0x1F, 0x27,
                0x39, 0x3D, 0x38, 0x32, 0x3C, 0x2E, 0x33, 0x34, 0x32, 0xFF,
                0xC0, 0x00, 0x0B, 0x08, 0x00, 0x01, 0x00, 0x01, 0x01, 0x01,
                0x11, 0x00, 0xFF, 0xC4, 0x00, 0x14, 0x00, 0x01, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x03, 0xFF, 0xDA, 0x00, 0x08, 0x01, 0x01,
                0x00, 0x00, 0x3F, 0x00, 0xFE, 0x8A, 0x28, 0xFF, 0xD9
            ])
            with open(image_path, 'wb') as f:
                f.write(jpeg_data)
            print(f"  Created test image: {image_path.name}")
        
        # Determine MIME type from file extension
        file_ext = image_path.suffix.lower()
        if file_ext in ['.png']:
            mime_type = 'image/png'
        else:
            mime_type = 'image/jpeg'
        
        # Send to HTTP endpoint
        if not self.http_endpoint:
            print("  ⚠ No HTTP endpoint configured, using default")
            self.http_endpoint = "http://localhost:8080/sps/api/entry-logs/upload-image"
            
        try:
            with open(image_path, 'rb') as img_file:
                # Prepare multipart form-data with image
                files = {'image': (image_path.name, img_file, mime_type)}
                
                # Add rfidCode as query parameter
                params = {'rfidCode': rfid_code}
                
                # Add headers
                headers = {'accept': '*/*'}
                
                print(f"  Sending image to {self.http_endpoint}?rfidCode={rfid_code}")
                response = requests.post(
                    self.http_endpoint,
                    files=files,
                    params=params,
                    headers=headers,
                    timeout=10
                )
                
                if response.status_code == 200:
                    print(f"  ✓ Image uploaded successfully")
                    print(f"    Response: {response.text[:100]}")
                else:
                    print(f"  ⚠ Upload response code: {response.status_code}")
                    print(f"    Response: {response.text[:100]}")
        except requests.exceptions.ConnectionError:
            print(f"  ✗ Could not connect to {self.http_endpoint}")
        except Exception as e:
            print(f"  ✗ Error uploading image: {e}")
            
    def disconnect(self):
        """Disconnect from MQTT broker"""
        if self.client:
            self.client.loop_stop()
            self.client.disconnect()
        print("✓ ESP32-CAM disconnected")


def print_menu():
    """Print the main menu"""
    print("\n" + "="*60)
    print("ESP32 MQTT SIMULATOR - MAIN MENU")
    print("="*60)
    print("Setup:")
    print("  1. Configure and connect to MQTT broker")
    print("  2. Send provision request (retained)")
    print("  3. Start automatic status updates (every 10s)")
    print("  4. Stop automatic status updates")
    print("\nESP32 Events:")
    print("  5. Simulate RFID card swipe")
    print("  6. Simulate sensor status change")
    print("  7. Send manual status update")
    print("\nESP32-CAM Events:")
    print("  8. Setup ESP32-CAM connection")
    print("  9. Configure HTTP endpoint for image upload")
    print("  10. Manually trigger camera capture")
    print("\nOther:")
    print("  11. Show current configuration")
    print("  0. Exit")
    print("="*60)


def main():
    """Main program loop"""
    esp32 = ESP32Simulator()
    esp32_cam = ESP32CamSimulator()
    
    print("\n🚀 ESP32 MQTT Simulator Started")
    print("This program simulates ESP32 and ESP32-CAM devices for testing")
    
    while True:
        print_menu()
        choice = input("\nEnter your choice: ").strip()
        
        if choice == '0':
            print("\n👋 Shutting down...")
            esp32.disconnect()
            esp32_cam.disconnect()
            print("Goodbye!")
            break
            
        elif choice == '1':
            print("\n📡 MQTT Configuration")
            broker = input("MQTT Broker [localhost]: ").strip() or "localhost"
            port = input("MQTT Port [1883]: ").strip() or "1883"
            username = input("MQTT Username: ").strip()
            password = input("MQTT Password: ").strip()
            
            if esp32.connect_mqtt(broker, int(port), username, password):
                print("✓ ESP32 connected successfully")
            else:
                print("✗ Failed to connect ESP32")
                
        elif choice == '2':
            esp32.send_provision_request()
            
        elif choice == '3':
            esp32.start_status_updates()
            
        elif choice == '4':
            esp32.stop_status_updates()
            
        elif choice == '5':
            print("\n💳 RFID Card Swipe")
            rfid_code = input("Enter RFID code [TEST_RFID_001]: ").strip() or "TEST_RFID_001"
            esp32.send_rfid_swipe(rfid_code)
            
        elif choice == '6':
            print("\n🔍 Sensor Status Change")
            sensor_id = input("Enter sensor ID [1]: ").strip() or "1"
            occupied = input("Is occupied? (y/n) [y]: ").strip().lower() or "y"
            esp32.send_sensor_status(int(sensor_id), occupied == 'y')
            
        elif choice == '7':
            esp32.send_status()
            
        elif choice == '8':
            print("\n📷 ESP32-CAM Configuration")
            if esp32.mqtt_username:
                print(f"Using existing MQTT credentials for {esp32.mqtt_username}")
                broker = input("MQTT Broker [localhost]: ").strip() or "localhost"
                port = input("MQTT Port [1883]: ").strip() or "1883"
                
                if esp32_cam.connect_mqtt(broker, int(port), esp32.mqtt_username, esp32.mqtt_password):
                    print("✓ ESP32-CAM connected successfully")
                else:
                    print("✗ Failed to connect ESP32-CAM")
            else:
                print("⚠ Please configure ESP32 MQTT connection first (option 1)")
                
        elif choice == '9':
            print("\n🌐 HTTP Endpoint Configuration")
            endpoint = input("HTTP Endpoint [http://localhost:8080/api/camera/upload]: ").strip()
            esp32_cam.http_endpoint = endpoint or "http://localhost:8080/api/camera/upload"
            print(f"✓ HTTP endpoint set to: {esp32_cam.http_endpoint}")
            
        elif choice == '10':
            print("\n📸 Manual Camera Capture")
            rfid_code = input("Enter RFID code for image [TEST_RFID_001]: ").strip() or "TEST_RFID_001"
            esp32_cam.capture_and_send_image(rfid_code)
            
        elif choice == '11':
            print("\n⚙️ Current Configuration")
            print(f"ESP32:")
            print(f"  Connected: {esp32.connected}")
            print(f"  MQTT Username: {esp32.mqtt_username or 'Not configured'}")
            print(f"  Base Topic: {esp32.base_topic or 'Not configured'}")
            print(f"  Status Updates: {'Running' if (esp32.status_thread and esp32.status_thread.is_alive()) else 'Stopped'}")
            print(f"\nESP32-CAM:")
            print(f"  Connected: {esp32_cam.connected}")
            print(f"  HTTP Endpoint: {esp32_cam.http_endpoint or 'Not configured'}")
            
        else:
            print("✗ Invalid choice, please try again")
            
        # Small delay for better UX
        time.sleep(0.5)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⚠ Interrupted by user")
        sys.exit(0)
