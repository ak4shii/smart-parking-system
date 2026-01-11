# Mosquitto MQTT Broker with auto password setup
FROM eclipse-mosquitto:2

# Copy entrypoint script and fix line endings (Windows CRLF -> Unix LF)
COPY config/docker-entrypoint.sh /docker-entrypoint.sh
RUN sed -i 's/\r$//' /docker-entrypoint.sh && chmod +x /docker-entrypoint.sh

# Use custom entrypoint
ENTRYPOINT ["/docker-entrypoint.sh"]
