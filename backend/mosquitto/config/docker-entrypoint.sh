#!/bin/sh
# ===========================================
# Mosquitto Docker Entrypoint Script
# Automatically generates password file on startup
# ===========================================

PASSWORDS_FILE="/mosquitto/config/passwords"
BACKEND_USER="sps-backend"
BACKEND_PASSWORD="${MQTT_BACKEND_PASSWORD:-sps_backend_secret}"

echo "=== Mosquitto Entrypoint ==="

# Create password file if it doesn't exist or is empty
if [ ! -s "$PASSWORDS_FILE" ]; then
    echo "Creating password file with backend user..."
    
    # Create password entry for backend
    mosquitto_passwd -c -b "$PASSWORDS_FILE" "$BACKEND_USER" "$BACKEND_PASSWORD"
    
    if [ $? -eq 0 ]; then
        echo "Password file created successfully"
        echo "  User: $BACKEND_USER"
    else
        echo "ERROR: Failed to create password file"
        exit 1
    fi
else
    echo "Password file exists, skipping generation"
fi

# Set correct permissions
chmod 600 "$PASSWORDS_FILE"

echo "Starting Mosquitto..."

# Execute the original mosquitto command
exec /usr/sbin/mosquitto -c /mosquitto/config/mosquitto.conf
