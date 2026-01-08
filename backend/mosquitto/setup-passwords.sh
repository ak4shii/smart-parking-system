#!/bin/bash
# ===========================================
# Mosquitto Password Setup Script
# Smart Parking System
# ===========================================

PASSWORDS_FILE="./config/passwords"
BACKEND_USER="sps-backend"

echo "=== Mosquitto Password Setup ==="
echo ""

# Check if mosquitto_passwd is available
if ! command -v mosquitto_passwd &> /dev/null; then
    echo "ERROR: mosquitto_passwd not found!"
    echo "Install mosquitto-clients package:"
    echo "  - Ubuntu/Debian: sudo apt install mosquitto-clients"
    echo "  - macOS: brew install mosquitto"
    echo "  - Windows: Install Mosquitto from https://mosquitto.org/download/"
    exit 1
fi

# Create config directory if not exists
mkdir -p config

# Generate secure random password
generate_password() {
    # Generate 32 character password
    openssl rand -base64 24 2>/dev/null || head -c 24 /dev/urandom | base64
}

echo "Setting up MQTT backend password..."

# Check if user wants to use custom password
read -p "Use custom password for backend? (y/N): " use_custom

if [ "$use_custom" = "y" ] || [ "$use_custom" = "Y" ]; then
    read -s -p "Enter password for $BACKEND_USER: " BACKEND_PASSWORD
    echo ""
else
    BACKEND_PASSWORD=$(generate_password)
    echo "Generated password: $BACKEND_PASSWORD"
fi

# Create/update password file
echo "Creating password file: $PASSWORDS_FILE"
mosquitto_passwd -c -b "$PASSWORDS_FILE" "$BACKEND_USER" "$BACKEND_PASSWORD"

if [ $? -eq 0 ]; then
    echo ""
    echo "=== SUCCESS ==="
    echo "Password file created: $PASSWORDS_FILE"
    echo ""
    echo "Backend credentials:"
    echo "  Username: $BACKEND_USER"
    echo "  Password: $BACKEND_PASSWORD"
    echo ""
    echo "Add to your .env file or docker-compose environment:"
    echo "  MQTT_BACKEND_PASSWORD=$BACKEND_PASSWORD"
    echo ""
    echo "Or update application.properties:"
    echo "  mqtt.username=$BACKEND_USER"
    echo "  mqtt.password=$BACKEND_PASSWORD"
else
    echo "ERROR: Failed to create password file"
    exit 1
fi

