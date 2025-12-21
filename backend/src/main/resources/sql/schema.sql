CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE devices (
    id BIGSERIAL PRIMARY KEY,
    device_code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100),
    oline BOOLEAN DEFAULT false,
    wifi_rssi INT,
    uptime_sec BIGINT,
    last_seen TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sensors (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT REFERENCES devices(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL,
    name VARCHAR(50),
    unit VARCHAR(20),
    pin VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sensor_data (
    id BIGSERIAL PRIMARY KEY,
    sensor_id BIGINT REFERENCES sensors(id) ON DELETE CASCADE,
    value DOUBLE PRECISION,
    extra JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE parking_slots (
    id BIGSERIAL PRIMARY KEY,
    slot_code VARCHAR(20) UNIQUE,
    device_id BIGINT REFERENCES devices(id),
    occupied BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE parking_records (
    id BIGSERIAL PRIMARY KEY,
    slot_id BIGINT REFERENCES parking_slots(id),
    rfid_uid VARCHAR(50),
    entry_time TIMESTAMP NOT NULL,
    exit_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rfid_logs (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT REFERENCES devices(id),
    uid VARCHAR(50) NOT NULL,
    action VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE actuator_logs (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT REFERENCES devices(id),
    actuator VARCHAR(20),
    command JSONB,
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE alerts (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT REFERENCES devices(id),
    severity VARCHAR(20),
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sensor_data_time ON sensor_data (created_at DESC);
CREATE INDEX idx_parking_records_time ON parking_records (entry_time DESC);
CREATE INDEX idx_devices_code ON devices (device_code);





