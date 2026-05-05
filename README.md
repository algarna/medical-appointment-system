# Medical Appointment System

Microservices-based system for managing medical appointments, built with Java 21, Spring Boot and Apache Kafka.

## Services

| Service | Description | Port |
|---|---|---|
| `patient-service` | Patient management (CRUD) | 8081 |
| `appointment-service` | Appointment scheduling and cancellation | 8082 |
| `notification-service` | Event-driven notifications via Kafka | 8083 |

## Tech Stack

- Java 21 + Spring Boot 4
- PostgreSQL 16 — one database per service
- Apache Kafka — async communication between services
- Docker Compose — local environment orchestration
- Swagger / OpenAPI — API documentation on each service

## Architecture

```bash
├── Client
│   ├── patient-service → patients-db (PostgreSQL)
│   ├── appointment-service → appointments-db (PostgreSQL) → publishes events to Kafka
│   ├── notification-service (consumes events)
```

## Getting Started

### Prerequisites

- Docker Desktop
- Java 21

### Run locally

1. Copy the environment file and fill in the values:
```bash
   cp .env.example .env
```

2. Start the infrastructure:
```bash
   docker compose up -d
```

3. Create your local config for each service (git-ignored):
```bash
   cp patient-service/src/main/resources/application-local.properties.example \
      patient-service/src/main/resources/application-local.properties
```

4. Run a service:
```bash
   cd patient-service
   SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

### API Documentation

Each service exposes Swagger UI when running locally:

- patient-service: http://localhost:8081/swagger-ui.html
- appointment-service: http://localhost:8082/swagger-ui.html
- notification-service: http://localhost:8083/swagger-ui.html

## Project Structure

```bash
├── medical-appointment-system/
│   ├── patient-service/
│   ├── appointment-service/
│   ├── notification-service/
├── docker-compose.yml
├── .env.example
```