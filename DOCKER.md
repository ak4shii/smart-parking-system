# Smart Parking System - Docker Setup

## Quick Start

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

## Services

- **Frontend**: http://localhost (React + Vite)
- **Backend API**: http://localhost:8080 (Spring Boot)
- **PostgreSQL**: localhost:5434
- **MQTT Broker**: localhost:1883
- **Adminer**: http://localhost:8081

## Development

For individual service development, use the respective folders:

- `cd frontend && npm run dev`
- `cd backend && ./mvnw spring-boot:run`
