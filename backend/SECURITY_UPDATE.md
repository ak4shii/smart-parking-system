# 🔒 MQTT Security Update - Smart Parking System

## Tổng quan

Bản cập nhật này triển khai hệ thống bảo mật MQTT toàn diện, đảm bảo mỗi thiết bị IoT (ESP32) chỉ có thể truy cập dữ liệu của chính nó.

---

## 🎯 Mục tiêu bảo mật

| Mục tiêu | Trạng thái |
|----------|------------|
| Chặn truy cập ẩn danh (anonymous) | ✅ |
| Yêu cầu xác thực username/password | ✅ |
| Mỗi device có credentials riêng | ✅ |
| Device chỉ access được topic của mình | ✅ |
| Backend có full access để quản lý | ✅ |
| Có thể revoke credentials khi cần | ✅ |

---

## 📁 Các file đã thay đổi/thêm mới

### Mosquitto Configuration

```
backend/mosquitto/config/
├── mosquitto.conf    # Cấu hình broker
├── acl               # Access Control List (MỚI)
└── passwords         # Password file
```

### Backend Java

```
backend/src/main/java/com/smart_parking_system/backend/
├── entity/
│   └── Microcontroller.java          # Thêm MQTT fields
├── dto/
│   └── MqttCredentialsResponseDto.java   # (MỚI)
├── service/
│   ├── IMqttCredentialService.java       # (MỚI)
│   └── impl/
│       └── MqttCredentialServiceImpl.java # (MỚI)
├── mqtt/
│   ├── MqttTopicUtils.java               # (MỚI)
│   ├── MqttConfig.java                   # Cập nhật topic pattern
│   ├── MqttEntryRequestHandler.java      # Cập nhật
│   ├── MqttExitRequestHandler.java       # Cập nhật
│   ├── MqttMicrocontrollerHandler.java   # Cập nhật
│   ├── MqttSensorHandler.java            # Cập nhật
│   └── MqttProvisionHandler.java         # Cập nhật
└── repository/
    └── MicrocontrollerRepository.java    # Thêm findByMqttUsername
```

### Database

```sql
-- Thêm columns vào bảng microcontroller
ALTER TABLE microcontroller ADD COLUMN mqtt_username VARCHAR UNIQUE;
ALTER TABLE microcontroller ADD COLUMN mqtt_password_hash VARCHAR;
ALTER TABLE microcontroller ADD COLUMN mqtt_enabled BOOLEAN DEFAULT true;
ALTER TABLE microcontroller ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
```

---

## 🏗️ Kiến trúc bảo mật

### Topic Structure (Mới)

```
Cũ:  sps/{username}/{mcCode}/...
Mới: sps/{mqttUsername}/...

Trong đó: mqttUsername = {ownerUsername}_{mcCode}
Ví dụ:    sps/john_mc12345678/sensor/status
```

### ACL Rules

```conf
# Backend (full access)
user sps-backend
topic readwrite sps/#

# Devices (chỉ access topic của mình)
pattern readwrite sps/%u/#
```

### Flow tạo device mới

```
┌─────────────────┐
│  User tạo       │
│  device trên    │
│  Web App        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Backend API    │
│  POST /api/     │
│  microcontrollers│
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  MqttCredentialService.generateCredentials()  │
│  - Tạo mqttUsername: john_mc12345678    │
│  - Generate random password              │
│  - Hash và lưu vào database             │
│  - Sync vào Mosquitto password file     │
└────────┬────────────────────────────────┘
         │
         ▼
┌─────────────────┐
│  Response với   │
│  MQTT credentials│
│  (1 lần duy nhất)│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  ESP32 lưu      │
│  credentials    │
│  và connect     │
└─────────────────┘
```

---

## 🔐 API Endpoints mới

### Tạo device (có credentials)

```http
POST /api/microcontrollers
Content-Type: application/json

{
  "mcCode": "mc12345678",
  "name": "Parking Gate 1",
  "parkingSpaceId": 1
}
```

**Response:**
```json
{
  "id": 1,
  "mcCode": "mc12345678",
  "name": "Parking Gate 1",
  "mqttUsername": "john_mc12345678",
  "mqttEnabled": true,
  "mqttCredentials": {
    "mqttHost": "your.server.ip",
    "mqttPort": 1883,
    "mqttUsername": "john_mc12345678",
    "mqttPassword": "generated_password_shown_once",
    "baseTopic": "sps/john_mc12345678"
  }
}
```

### Regenerate credentials

```http
POST /api/microcontrollers/{id}/mqtt/regenerate
```

### Revoke credentials

```http
POST /api/microcontrollers/{id}/mqtt/revoke
```

---

## 🛡️ Kịch bản tấn công & Phòng thủ

### Kịch bản 1: Hacker cố subscribe topic người khác

```
Hacker (user2_mc002) subscribe → sps/user1_mc001/#
                                        ↓
                               ACL Check: %u = user2_mc002
                               Pattern: sps/user2_mc002/#
                               Requested: sps/user1_mc001/#
                                        ↓
                               ❌ KHÔNG KHỚP → Không nhận message
```

### Kịch bản 2: Hacker cố subscribe wildcard

```
Hacker subscribe → sps/#
                      ↓
              ACL: Chỉ deliver messages
              matching sps/{hacker_username}/#
                      ↓
              ✅ Chỉ nhận message của chính hacker
```

### Kịch bản 3: Credentials bị lộ

```
Admin phát hiện credentials bị lộ
              ↓
POST /api/microcontrollers/{id}/mqtt/revoke
              ↓
- mqttUsername bị xóa khỏi password file
- Device không thể connect nữa
              ↓
POST /api/microcontrollers/{id}/mqtt/regenerate
              ↓
- Credentials mới được tạo
- Cập nhật vào ESP32
```

---

## 🧪 Testing

### Chạy security test

```powershell
cd backend
pip install paho-mqtt
python test_mqtt_security.py
```

### Test cases

| Test | Mô tả | Expected |
|------|-------|----------|
| Anonymous Access | Connect không có credentials | ❌ Denied |
| Wrong Password | Connect với password sai | ❌ Denied |
| Correct Credentials | Connect với credentials đúng | ✅ OK |
| Backend Full Access | Backend subscribe sps/# | ✅ OK |
| Device Own Topic | Device subscribe topic của mình | ✅ OK |
| Cross-Device Isolation | Device không nhận message của device khác | ✅ Isolated |
| Wildcard Isolation | Device subscribe sps/# chỉ nhận message của mình | ✅ Isolated |

---

## 🚀 Hướng dẫn Deploy

### 1. Khởi động services

```powershell
cd backend
docker-compose up -d
```

### 2. Tạo backend MQTT user

```powershell
docker exec sps-mosquitto mosquitto_passwd -c -b /mosquitto/config/passwords sps-backend YOUR_SECURE_PASSWORD

# Fix permissions
docker exec sps-mosquitto chown root:root /mosquitto/config/passwords
docker exec sps-mosquitto chmod 0600 /mosquitto/config/passwords

# Restart
docker restart sps-mosquitto
```

### 3. Cập nhật environment

```env
MQTT_BACKEND_PASSWORD=YOUR_SECURE_PASSWORD
MQTT_PUBLIC_HOST=your.server.ip
MQTT_PUBLIC_PORT=1883
```

### 4. Verify

```powershell
python test_mqtt_security.py
```

---

## 📋 Checklist bảo mật

- [ ] Đổi default password của backend (`sps-backend`)
- [ ] Cấu hình `MQTT_PUBLIC_HOST` đúng IP/domain public
- [ ] Chạy `test_mqtt_security.py` và pass tất cả tests
- [ ] Backup password file thường xuyên
- [ ] Monitor logs cho các connection attempts bất thường
- [ ] Rotate credentials định kỳ cho production devices

---

## 🔧 Troubleshooting

### Device không connect được

1. Kiểm tra credentials đã tạo chưa:
   ```powershell
   docker exec sps-mosquitto cat /mosquitto/config/passwords
   ```

2. Kiểm tra Mosquitto logs:
   ```powershell
   docker logs sps-mosquitto --tail 50
   ```

3. Test thủ công:
   ```powershell
   docker exec sps-mosquitto mosquitto_pub -h localhost -t "test" -u "USERNAME" -P "PASSWORD" -m "test"
   ```

### ACL không hoạt động

1. Kiểm tra file ACL:
   ```powershell
   docker exec sps-mosquitto cat /mosquitto/config/acl
   ```

2. Restart Mosquitto:
   ```powershell
   docker restart sps-mosquitto
   ```

---

## 📚 Tài liệu tham khảo

- [Mosquitto Authentication](https://mosquitto.org/man/mosquitto-conf-5.html)
- [Mosquitto ACL](https://mosquitto.org/man/mosquitto-conf-5.html#idm121)
- [MQTT Security Best Practices](https://www.hivemq.com/blog/mqtt-security-fundamentals/)

---

**Cập nhật lần cuối:** January 2026  
**Tác giả:** Smart Parking System Team

