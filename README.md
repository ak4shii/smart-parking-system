# Smart Parking System

A comprehensive IoT-based smart parking management system with a Spring Boot backend, React frontend, and ESP32 microcontroller integration.

## 🚀 Features

- **User Authentication**: JWT-based authentication system with role-based access control
- **Parking Space Management**: Manage multiple parking spaces with real-time occupancy tracking
- **IoT Integration**: ESP32 microcontroller support for sensor data collection
- **Real-time Monitoring**: Track parking slot occupancy and entry/exit logs
- **RFID Support**: RFID-based vehicle entry/exit tracking
- **MQTT Communication**: Real-time device communication via MQTT broker
- **Modern UI**: Responsive React frontend with Tailwind CSS

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- **Java 21** or higher ([Download](https://www.oracle.com/java/technologies/downloads/))
- **Maven 3.6+** (or use the included Maven wrapper)
- **Node.js 18+** and **npm** ([Download](https://nodejs.org/))
- **Docker** and **Docker Compose** ([Download](https://www.docker.com/get-started))
- **PostgreSQL 16** (or use Docker Compose)

## 🐳 Docker Setup

### Quick Start

Run the entire application stack:

```bash
docker-compose up -d
```

Build and restart services:

```bash
docker-compose up -d --build
```

Stop all services:

```bash
docker-compose down
```

View logs:

```bash
docker-compose logs -f
```

### Services

| Service        | URL                      | Description         |
| -------------- | ------------------------ | ------------------- |
| Frontend       | http://localhost         | React + Vite        |
| Backend API    | http://localhost:8080    | Spring Boot         |
| PostgreSQL     | localhost:5434           | Database            |
| MQTT Broker    | localhost:1883           | Mosquitto           |
| Adminer        | http://localhost:8081    | Database Management |

## 🛠️ Installation

### 1. Clone the Repository

```bash
git clone <repository-url>
cd smart-parking-system
```

### 2. Backend Setup

#### Option A: Using Docker Compose (Recommended)

The backend includes a `docker-compose.yml` file that automatically sets up PostgreSQL:

```bash
cd backend
docker-compose up -d
```

This will start a PostgreSQL container with the following default configuration:
- **Database**: `sps_db`
- **Username**: `postgres`
- **Password**: `admin`
- **Port**: `5434`

#### Option B: Manual PostgreSQL Setup

If you prefer to use a local PostgreSQL installation:

1. Create a database named `sps_db`
2. Update the database credentials in `backend/src/main/resources/application.properties`

#### Build and Run Backend

```bash
cd backend

# Using Maven wrapper (Windows)
mvnw.cmd clean install

# Using Maven wrapper (Linux/Mac)
./mvnw clean install

# Or using Maven directly
mvn clean install

# Run the application
mvnw.cmd spring-boot:run
# Or
./mvnw spring-boot:run
```

**API Documentation**: Once the backend is running, access Swagger UI at:
- `http://localhost:8080/swagger-ui.html`

### 3. Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

### 4. Build for Production

#### Backend

```bash
cd backend
mvnw.cmd clean package
# The JAR file will be in target/backend-0.0.1-SNAPSHOT.jar
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

#### Frontend

```bash
cd frontend
npm run build
# The production build will be in dist/
```

## 📁 Project Structure

```
smart-parking-system/
├── backend/                          # Spring Boot Backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/.../backend/
│   │   │   │   ├── config/           # Configuration classes
│   │   │   │   ├── constant/         # Application constants
│   │   │   │   ├── controller/       # REST controllers
│   │   │   │   ├── dto/              # Data Transfer Objects
│   │   │   │   ├── entity/           # JPA entities
│   │   │   │   ├── filter/           # HTTP filters
│   │   │   │   ├── mqtt/             # MQTT handlers & services
│   │   │   │   ├── repository/       # JPA repositories
│   │   │   │   ├── scheduler/        # Scheduled tasks
│   │   │   │   ├── security/         # Security configuration
│   │   │   │   ├── service/          # Business logic
│   │   │   │   └── util/             # Utility classes
│   │   │   └── resources/            # Config files & SQL scripts
│   │   └── test/                     # Test classes
│   ├── mosquitto/                    # MQTT broker configuration
│   ├── esp32_mqtt_simulator.py       # ESP32 MQTT simulator
│   ├── esp32_provision_simulator.py  # Provisioning simulator
│   ├── Dockerfile                    # Backend Docker image
│   └── pom.xml                       # Maven dependencies
│
├── frontend/                         # React + TypeScript Frontend
│   ├── src/
│   │   ├── assets/                   # Static assets
│   │   ├── components/               # Reusable components
│   │   ├── context/                  # React contexts
│   │   ├── hooks/                    # Custom React hooks
│   │   ├── pages/                    # Page components
│   │   └── services/                 # API services
│   ├── public/                       # Public assets
│   ├── prototype/                    # UI prototypes
│   ├── Dockerfile                    # Frontend Docker image
│   ├── nginx.conf                    # Nginx configuration
│   └── vite.config.ts                # Vite configuration
│
├── firmware/                         # ESP32 Microcontroller Code (PlatformIO)
│   ├── src/
│   │   ├── main.cpp                  # Main application
│   │   ├── CredentialManager.*       # WiFi/MQTT credentials
│   │   ├── IRController.*            # IR sensor control
│   │   ├── LCDController.*           # LCD display control
│   │   ├── MQTTController.*          # MQTT communication
│   │   ├── RFIDController.*          # RFID reader control
│   │   ├── ServoController.*         # Servo motor control
│   │   ├── UltrasonicController.*    # Ultrasonic sensor control
│   │   └── WiFiController.*          # WiFi management
│   ├── include/                      # Header files
│   ├── lib/                          # Libraries
│   ├── test/                         # Unit tests
│   └── platformio.ini                # PlatformIO configuration
│
├── .github/                          # GitHub configuration
│   └── CODEOWNERS                    # Code ownership rules
│
├── docker-compose.yml                # Full stack Docker setup
└── README.md                         # This file
```