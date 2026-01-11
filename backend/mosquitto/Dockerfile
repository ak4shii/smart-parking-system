# Mosquitto MQTT Broker with auto password setup
FROM eclipse-mosquitto:2

# Copy entrypoint script
COPY config/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# Use custom entrypoint
ENTRYPOINT ["/docker-entrypoint.sh"]
