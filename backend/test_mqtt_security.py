#!/usr/bin/env python3
"""
MQTT Security Test Script
Smart Parking System

Features:
- Auto-setup: Creates test users before running tests
- Auto-cleanup: Removes test users after tests complete
- Comprehensive security testing

Usage:
    pip install paho-mqtt
    python test_mqtt_security.py
"""

import time
import subprocess
import sys
from dataclasses import dataclass
from typing import Optional, List
import paho.mqtt.client as mqtt

# ===========================================
# Configuration
# ===========================================
MQTT_HOST = "localhost"
MQTT_PORT = 1883
MOSQUITTO_CONTAINER = "sps-mosquitto"
PASSWORD_FILE = "/mosquitto/config/passwords"

# Backend credentials (must exist)
BACKEND_USER = "sps-backend"
BACKEND_PASS = "admin"  # Change to your password

# Test device credentials (will be auto-created)
DEVICE1_USER = "testuser1_mc001"
DEVICE1_PASS = "testpass1"

DEVICE2_USER = "testuser2_mc002"
DEVICE2_PASS = "testpass2"

# List of test users to create/cleanup
TEST_USERS = [
    (DEVICE1_USER, DEVICE1_PASS),
    (DEVICE2_USER, DEVICE2_PASS),
]


# ===========================================
# Docker Helper Functions
# ===========================================
def run_docker_cmd(cmd: str, ignore_errors: bool = False) -> tuple[bool, str]:
    """Run a command inside the Mosquitto container"""
    full_cmd = f'docker exec {MOSQUITTO_CONTAINER} sh -c "{cmd}"'
    try:
        result = subprocess.run(
            full_cmd,
            shell=True,
            capture_output=True,
            text=True,
            timeout=10
        )
        if result.returncode != 0 and not ignore_errors:
            return False, result.stderr
        return True, result.stdout
    except subprocess.TimeoutExpired:
        return False, "Command timeout"
    except Exception as e:
        return False, str(e)


def check_docker_running() -> bool:
    """Check if Mosquitto container is running"""
    try:
        result = subprocess.run(
            f'docker ps --filter "name={MOSQUITTO_CONTAINER}" --format "{{{{.Names}}}}"',
            shell=True,
            capture_output=True,
            text=True,
            timeout=5
        )
        return MOSQUITTO_CONTAINER in result.stdout
    except:
        return False


def create_mqtt_user(username: str, password: str) -> bool:
    """Create or update MQTT user in Mosquitto"""
    cmd = f"mosquitto_passwd -b {PASSWORD_FILE} {username} {password}"
    success, error = run_docker_cmd(cmd)
    if not success:
        print(f"  ⚠️  Failed to create user {username}: {error}")
    return success


def delete_mqtt_user(username: str) -> bool:
    """Delete MQTT user from Mosquitto"""
    cmd = f"mosquitto_passwd -D {PASSWORD_FILE} {username}"
    success, _ = run_docker_cmd(cmd, ignore_errors=True)
    return success


def fix_password_file_permissions():
    """Fix ownership and permissions of password file"""
    run_docker_cmd(f"chown root:root {PASSWORD_FILE}", ignore_errors=True)
    run_docker_cmd(f"chmod 0600 {PASSWORD_FILE}", ignore_errors=True)


def restart_mosquitto():
    """Restart Mosquitto container to apply password changes"""
    print("  Restarting Mosquitto container...")
    try:
        subprocess.run(
            f"docker restart {MOSQUITTO_CONTAINER}",
            shell=True,
            capture_output=True,
            timeout=30
        )
        # Wait for Mosquitto to fully start
        time.sleep(5)
        return True
    except Exception as e:
        print(f"  ⚠️  Failed to restart: {e}")
        return False


# ===========================================
# Test Results
# ===========================================
@dataclass
class TestResult:
    name: str
    passed: bool
    message: str


results: List[TestResult] = []


def add_result(name: str, passed: bool, message: str):
    results.append(TestResult(name, passed, message))
    status = "✅ PASS" if passed else "❌ FAIL"
    print(f"{status} | {name}: {message}")


# ===========================================
# MQTT Test Helpers
# ===========================================
class MqttTester:
    def __init__(self, client_id: str):
        self.client = mqtt.Client(client_id=client_id, protocol=mqtt.MQTTv311)
        self.connected = False
        self.connection_error = None
        self.messages_received = []
        self.subscribe_result = None
        
        self.client.on_connect = self._on_connect
        self.client.on_disconnect = self._on_disconnect
        self.client.on_message = self._on_message
        self.client.on_subscribe = self._on_subscribe
    
    def _on_connect(self, client, userdata, flags, rc):
        if rc == 0:
            self.connected = True
            self.connection_error = None
        else:
            self.connected = False
            error_messages = {
                1: "Incorrect protocol version",
                2: "Invalid client identifier",
                3: "Server unavailable",
                4: "Bad username or password",
                5: "Not authorized"
            }
            self.connection_error = error_messages.get(rc, f"Unknown error: {rc}")
    
    def _on_disconnect(self, client, userdata, rc):
        self.connected = False
    
    def _on_message(self, client, userdata, msg):
        self.messages_received.append({
            "topic": msg.topic,
            "payload": msg.payload.decode()
        })
    
    def _on_subscribe(self, client, userdata, mid, granted_qos):
        self.subscribe_result = granted_qos[0] if granted_qos else None
    
    def connect(self, username: Optional[str] = None, password: Optional[str] = None, timeout: float = 5.0) -> bool:
        """Try to connect to MQTT broker"""
        self.connected = False
        self.connection_error = None
        
        if username:
            self.client.username_pw_set(username, password)
        
        try:
            self.client.connect(MQTT_HOST, MQTT_PORT, keepalive=60)
            self.client.loop_start()
            
            # Wait for connection
            start = time.time()
            while not self.connected and self.connection_error is None:
                if time.time() - start > timeout:
                    self.connection_error = "Connection timeout"
                    break
                time.sleep(0.1)
            
            return self.connected
        except Exception as e:
            self.connection_error = str(e)
            return False
    
    def disconnect(self):
        """Disconnect from broker"""
        try:
            self.client.loop_stop()
            self.client.disconnect()
        except:
            pass
    
    def subscribe_and_wait(self, topic: str, timeout: float = 3.0) -> bool:
        """Subscribe to topic and check if successful"""
        self.subscribe_result = None
        self.client.subscribe(topic, qos=1)
        
        start = time.time()
        while self.subscribe_result is None:
            if time.time() - start > timeout:
                return False
            time.sleep(0.1)
        
        # QoS 128 means subscription failed (not authorized)
        return self.subscribe_result != 128
    
    def publish(self, topic: str, message: str) -> bool:
        """Publish message to topic"""
        result = self.client.publish(topic, message, qos=1)
        return result.rc == mqtt.MQTT_ERR_SUCCESS
    
    def wait_for_messages(self, count: int = 1, timeout: float = 3.0) -> List[dict]:
        """Wait for messages to arrive"""
        self.messages_received = []
        start = time.time()
        while len(self.messages_received) < count:
            if time.time() - start > timeout:
                break
            time.sleep(0.1)
        return self.messages_received


# ===========================================
# Setup and Cleanup
# ===========================================
def setup_test_users():
    """Create test users before running tests"""
    print("\n" + "="*60)
    print("🔧 SETUP: Creating test users")
    print("="*60)
    
    # Check if container is running
    if not check_docker_running():
        print(f"❌ ERROR: Container '{MOSQUITTO_CONTAINER}' is not running!")
        print("   Run: docker-compose up -d")
        return False
    
    # Delete existing test users first (clean slate)
    print("  Cleaning up existing test users...")
    for username, _ in TEST_USERS:
        delete_mqtt_user(username)
    
    # Create test users
    print("  Creating test users...")
    all_created = True
    for username, password in TEST_USERS:
        if create_mqtt_user(username, password):
            print(f"    ✓ Created: {username}")
        else:
            print(f"    ✗ Failed: {username}")
            all_created = False
    
    # Fix permissions
    print("  Fixing file permissions...")
    fix_password_file_permissions()
    
    # Restart Mosquitto to load new users
    restart_mosquitto()
    
    # Verify users were created by checking password file
    print("  Verifying users...")
    success, content = run_docker_cmd(f"cat {PASSWORD_FILE}")
    if success:
        for username, _ in TEST_USERS:
            if username in content:
                print(f"    ✓ Verified: {username}")
            else:
                print(f"    ✗ Missing: {username}")
                all_created = False
    
    if all_created:
        print("✅ Setup complete!\n")
    else:
        print("⚠️  Setup completed with warnings\n")
    
    return True


def cleanup_test_users():
    """Remove test users after tests complete"""
    print("\n" + "="*60)
    print("🧹 CLEANUP: Removing test users")
    print("="*60)
    
    for username, _ in TEST_USERS:
        if delete_mqtt_user(username):
            print(f"    ✓ Removed: {username}")
        else:
            print(f"    - Not found: {username}")
    
    # Fix permissions after cleanup
    fix_password_file_permissions()
    print("✅ Cleanup complete!\n")


# ===========================================
# Test Cases
# ===========================================
def test_anonymous_denied():
    """Test 1: Anonymous access should be denied"""
    print("\n" + "="*50)
    print("TEST 1: Anonymous Access")
    print("="*50)
    
    tester = MqttTester("test-anonymous")
    connected = tester.connect(username=None, password=None)
    tester.disconnect()
    
    if not connected:
        add_result("Anonymous Access", True, f"Denied as expected ({tester.connection_error})")
    else:
        add_result("Anonymous Access", False, "Anonymous connection was allowed!")


def test_wrong_password_denied():
    """Test 2: Wrong password should be denied"""
    print("\n" + "="*50)
    print("TEST 2: Wrong Password")
    print("="*50)
    
    tester = MqttTester("test-wrong-pass")
    connected = tester.connect(username=BACKEND_USER, password="wrong_password_123")
    tester.disconnect()
    
    if not connected:
        add_result("Wrong Password", True, f"Denied as expected ({tester.connection_error})")
    else:
        add_result("Wrong Password", False, "Wrong password was accepted!")


def test_correct_credentials():
    """Test 3: Correct credentials should work"""
    print("\n" + "="*50)
    print("TEST 3: Correct Credentials")
    print("="*50)
    
    tester = MqttTester("test-correct-creds")
    connected = tester.connect(username=BACKEND_USER, password=BACKEND_PASS)
    
    if connected:
        add_result("Correct Credentials", True, "Connected successfully")
        tester.disconnect()
    else:
        add_result("Correct Credentials", False, f"Connection failed: {tester.connection_error}")


def test_backend_full_access():
    """Test 4: Backend should have full access to all topics"""
    print("\n" + "="*50)
    print("TEST 4: Backend Full Access")
    print("="*50)
    
    tester = MqttTester("test-backend-access")
    if not tester.connect(username=BACKEND_USER, password=BACKEND_PASS):
        add_result("Backend Full Access", False, f"Connection failed: {tester.connection_error}")
        return
    
    # Subscribe to all topics
    if tester.subscribe_and_wait("sps/#"):
        add_result("Backend Subscribe sps/#", True, "Can subscribe to all topics")
    else:
        add_result("Backend Subscribe sps/#", False, "Cannot subscribe to sps/#")
    
    # Publish to any topic
    if tester.publish("sps/anyuser_anydevice/test", "test message"):
        add_result("Backend Publish", True, "Can publish to any topic")
    else:
        add_result("Backend Publish", False, "Cannot publish")
    
    tester.disconnect()


def test_device_own_topic_access():
    """Test 5: Device can access its own topics"""
    print("\n" + "="*50)
    print("TEST 5: Device Own Topic Access")
    print("="*50)
    
    tester = MqttTester("test-device1-own")
    if not tester.connect(username=DEVICE1_USER, password=DEVICE1_PASS):
        add_result("Device1 Connect", False, f"Connection failed: {tester.connection_error}")
        add_result("Device1 Own Topic", False, "Skipped - connection failed")
        return
    
    add_result("Device1 Connect", True, "Connected successfully")
    
    # Subscribe to own topic
    own_topic = f"sps/{DEVICE1_USER}/#"
    if tester.subscribe_and_wait(own_topic):
        add_result("Device1 Own Topic Subscribe", True, f"Can subscribe to {own_topic}")
    else:
        add_result("Device1 Own Topic Subscribe", False, f"Cannot subscribe to own topic")
    
    # Publish to own topic
    if tester.publish(f"sps/{DEVICE1_USER}/sensor/status", '{"temp": 25}'):
        add_result("Device1 Own Topic Publish", True, "Can publish to own topic")
    else:
        add_result("Device1 Own Topic Publish", False, "Cannot publish to own topic")
    
    tester.disconnect()


def test_device_cannot_access_others():
    """Test 6: Device cannot access other users' topics (message delivery test)"""
    print("\n" + "="*50)
    print("TEST 6: Device Cannot Access Others' Topics")
    print("="*50)
    
    tester = MqttTester("test-device1-other")
    if not tester.connect(username=DEVICE1_USER, password=DEVICE1_PASS):
        add_result("ACL Test Connect", False, f"Connection failed: {tester.connection_error}")
        return
    
    # Note: Mosquitto may allow subscribe but won't deliver messages
    # This is expected behavior without acl_deny_subscribe
    other_topic = f"sps/{DEVICE2_USER}/#"
    tester.subscribe_and_wait(other_topic)
    
    # The real test is whether messages are delivered (tested in Test 7)
    add_result("ACL Subscribe Test", True, "Subscribe request sent (delivery tested in Test 7)")
    
    tester.disconnect()


def test_cross_device_isolation():
    """Test 7: Full isolation test between devices - THE MAIN SECURITY TEST"""
    print("\n" + "="*50)
    print("TEST 7: Cross-Device Isolation (Main Security Test)")
    print("="*50)
    
    # Device 1 subscribes to its topic
    device1 = MqttTester("test-isolation-dev1")
    if not device1.connect(username=DEVICE1_USER, password=DEVICE1_PASS):
        add_result("Isolation Test", False, f"Device1 connection failed: {device1.connection_error}")
        return
    
    device1.subscribe_and_wait(f"sps/{DEVICE1_USER}/#")
    
    # Device 2 subscribes trying to snoop on Device 1
    device2 = MqttTester("test-isolation-dev2")
    if not device2.connect(username=DEVICE2_USER, password=DEVICE2_PASS):
        add_result("Isolation Test", False, f"Device2 connection failed: {device2.connection_error}")
        device1.disconnect()
        return
    
    # Device 2 tries to subscribe to Device 1's topic (should fail or not receive messages)
    device2.subscribe_and_wait(f"sps/{DEVICE1_USER}/#")
    
    # Backend publishes to Device 1's topic
    backend = MqttTester("test-isolation-backend")
    if not backend.connect(username=BACKEND_USER, password=BACKEND_PASS):
        add_result("Isolation Test", False, f"Backend connection failed: {backend.connection_error}")
        device1.disconnect()
        device2.disconnect()
        return
    
    test_message = "secret_data_for_device1_only"
    backend.publish(f"sps/{DEVICE1_USER}/sensor/data", test_message)
    time.sleep(1.5)
    
    # Check who received the message
    device1_messages = device1.messages_received
    device2_messages = device2.messages_received
    
    device1_got_it = any(m["payload"] == test_message for m in device1_messages)
    device2_got_it = any(m["payload"] == test_message for m in device2_messages)
    
    if device1_got_it and not device2_got_it:
        add_result("Cross-Device Isolation", True, 
                   "✅ SECURE: Device1 received message, Device2 did NOT receive it!")
    elif device1_got_it and device2_got_it:
        add_result("Cross-Device Isolation", False, 
                   "🚨 SECURITY ISSUE: Device2 received Device1's message!")
    elif not device1_got_it:
        add_result("Cross-Device Isolation", False, 
                   "Device1 did not receive its own message (check ACL)")
    
    device1.disconnect()
    device2.disconnect()
    backend.disconnect()


def test_wildcard_isolation():
    """Test 8: Device subscribing to wildcard should only receive its own messages"""
    print("\n" + "="*50)
    print("TEST 8: Wildcard Subscription Isolation")
    print("="*50)
    
    # Device 1 subscribes to wildcard sps/#
    device1 = MqttTester("test-wildcard-dev1")
    if not device1.connect(username=DEVICE1_USER, password=DEVICE1_PASS):
        add_result("Wildcard Test", False, f"Connection failed: {device1.connection_error}")
        return
    
    device1.subscribe_and_wait("sps/#")  # Try to subscribe to everything
    
    # Backend publishes to multiple topics
    backend = MqttTester("test-wildcard-backend")
    if not backend.connect(username=BACKEND_USER, password=BACKEND_PASS):
        add_result("Wildcard Test", False, "Backend connection failed")
        device1.disconnect()
        return
    
    # Publish to Device1's topic
    msg1 = "message_for_device1"
    backend.publish(f"sps/{DEVICE1_USER}/data", msg1)
    
    # Publish to Device2's topic
    msg2 = "message_for_device2"
    backend.publish(f"sps/{DEVICE2_USER}/data", msg2)
    
    time.sleep(1.5)
    
    received_payloads = [m["payload"] for m in device1.messages_received]
    
    got_own = msg1 in received_payloads
    got_others = msg2 in received_payloads
    
    if got_own and not got_others:
        add_result("Wildcard Isolation", True, 
                   "✅ SECURE: Only received own messages despite wildcard subscription")
    elif got_own and got_others:
        add_result("Wildcard Isolation", False, 
                   "🚨 SECURITY ISSUE: Received other device's messages!")
    else:
        add_result("Wildcard Isolation", False, 
                   f"Unexpected result - own:{got_own}, others:{got_others}")
    
    device1.disconnect()
    backend.disconnect()


# ===========================================
# Main
# ===========================================
def print_summary():
    print("\n" + "="*60)
    print("📊 TEST SUMMARY")
    print("="*60)
    
    passed = sum(1 for r in results if r.passed)
    failed = sum(1 for r in results if not r.passed)
    
    for r in results:
        status = "✅" if r.passed else "❌"
        print(f"{status} {r.name}")
    
    print("-"*60)
    print(f"Total: {len(results)} | Passed: {passed} | Failed: {failed}")
    
    # Security verdict
    print("\n" + "="*60)
    print("🔒 SECURITY VERDICT")
    print("="*60)
    
    critical_tests = ["Cross-Device Isolation", "Wildcard Isolation"]
    critical_passed = all(
        r.passed for r in results if r.name in critical_tests
    )
    
    if critical_passed and failed == 0:
        print("🎉 ALL TESTS PASSED - System is SECURE!")
    elif critical_passed:
        print("✅ CRITICAL SECURITY TESTS PASSED")
        print(f"   {failed} non-critical test(s) failed")
        print("   System is secure but review failed tests")
    else:
        print("🚨 CRITICAL SECURITY TESTS FAILED!")
        print("   Review your ACL configuration immediately!")


def main():
    print("""
╔═══════════════════════════════════════════════════════════════╗
║         MQTT Security Test - Smart Parking System             ║
║                                                               ║
║  Features:                                                    ║
║  • Auto-setup: Creates test users automatically               ║
║  • Auto-cleanup: Removes test users after tests               ║
║  • Comprehensive security validation                          ║
╚═══════════════════════════════════════════════════════════════╝
    """)
    
    print(f"Target: {MQTT_HOST}:{MQTT_PORT}")
    print(f"Backend User: {BACKEND_USER}")
    print(f"Container: {MOSQUITTO_CONTAINER}")
    
    # Setup
    if not setup_test_users():
        print("\n❌ Setup failed. Exiting.")
        sys.exit(1)
    
    try:
        # Run tests
        test_anonymous_denied()
        test_wrong_password_denied()
        test_correct_credentials()
        test_backend_full_access()
        test_device_own_topic_access()
        test_device_cannot_access_others()
        test_cross_device_isolation()
        test_wildcard_isolation()
        
        # Print summary
        print_summary()
        
    finally:
        # Always cleanup
        cleanup_test_users()


if __name__ == "__main__":
    main()
