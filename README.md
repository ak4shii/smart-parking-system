# Smart Parking System

A comprehensive IoT-based smart parking management system with a Spring Boot backend, React frontend, and ESP32 microcontroller integration.

## 🚀 Features

- **User Authentication**: JWT-based authentication system with role-based access control
- **Parking Space Management**: Manage multiple parking spaces with real-time occupancy tracking
- **IoT Integration**: ESP32 microcontroller support for sensor data collection
- **Real-time Monitoring**: Track parking slot occupancy and entry/exit logs
- **RFID Support**: RFID-based vehicle entry/exit tracking
- **Modern UI**: Responsive React frontend with Tailwind CSS

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- **Java 21** or higher ([Download](https://www.oracle.com/java/technologies/downloads/))
- **Maven 3.6+** (or use the included Maven wrapper)
- **Node.js 18+** and **npm** ([Download](https://nodejs.org/))
- **Docker** and **Docker Compose** ([Download](https://www.docker.com/get-started))
- **PostgreSQL 16** (or use Docker Compose)

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
├── backend/                    # Spring Boot Backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/smart_parking_system/backend/
│   │   │   │       ├── config/          # Configuration classes
│   │   │   │       ├── constant/        # Application constants
│   │   │   │       ├── controller/       # REST controllers
│   │   │   │       ├── dto/              # Data Transfer Objects
│   │   │   │       ├── entity/           # JPA entities
│   │   │   │       ├── filter/           # HTTP filters
│   │   │   │       ├── repository/       # JPA repositories
│   │   │   │       ├── security/         # Security configuration
│   │   │   │       ├── service/          # Business logic
│   │   │   │       └── util/             # Utility classes
│   │   │   └── resources/
│   │   │       ├── sql/                  # Database schema
│   │   │       └── log/                  # Application logs
│   │   └── test/                          # Test classes
│   ├── docker-compose.yml                 # PostgreSQL Docker setup
│   └── pom.xml                            # Maven dependencies
│
├── frontend/                  # React + TypeScript Frontend
│   ├── src/
│   │   ├── components/        # Reusable components
│   │   ├── context/           # React contexts
│   │   ├── pages/             # Page components
│   │   ├── services/          # API services
│   │   ├── hooks/             # Custom React hooks
│   │   ├── layouts/           # Layout components
│   │   ├── styles/            # Additional styles
│   │   ├── utils/             # Utility functions
│   │   └── assets/            # Static assets
│   └── public/                # Public assets
│
├── esp32/                     # ESP32 Microcontroller Code
│
└── docs/                      # Documentation
```