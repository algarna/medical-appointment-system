# Medical Appointment System

Microservices-based system for managing medical appointments, built with Java 21, Spring Boot and Apache Kafka.

## Services

| Service | Description | Port | Status |
|---|---|---|---| 
| `patient-service` | Patient management (CRUD) | 8081 | ✅ Done |
| `appointment-service` | Appointment scheduling and cancellation | 8082 | 🚧 Coming soon |
| `notification-service` | Event-driven notifications via Kafka | 8083 | 🚧 Coming soon |

## Tech Stack

- **Java 21** + **Spring Boot 4**
- **PostgreSQL 16** — one database per service
- **Apache Kafka** — async communication between services
- **Flyway** — versioned database migrations
- **Docker / Docker Compose** — containerized local environment
- **Swagger / OpenAPI** — API documentation on each service
- **Testcontainers** — integration tests with real PostgreSQL

## Architecture

```bash
├── Client
│   ├── patient-service (:8081) → patients-db (PostgreSQL:5432)
│   ├── appointment-service (:8082) → appointments-db (PostgreSQL:5433) [coming soon] → publishes events to Kafka
│   ├── notification-service (:8083) [coming soon] (consumes events)
```

## Getting Started

### Prerequisites

- Docker Desktop
- Java 21 (via [SDKMAN](https://sdkman.io): `sdk install java 21.0.7-tem`)

### Run the full stack with Docker

```bash
cp .env.example .env    # fill in your values
docker compose up -d
```

API available at `http://localhost:8081`  
Swagger UI at `http://localhost:8081/swagger-ui.html`

### Run a service locally (for development)

```bash
# 1. Start infrastructure only
docker compose up -d patients-db

# 2. Create local config (git-ignored)
cp patient-service/src/main/resources/application-local.properties.example \
   patient-service/src/main/resources/application-local.properties
# Fill in your local DB credentials

# 3. Run the service
cd patient-service
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

### Run tests

```bash
cd patient-service
./mvnw test
```

Tests use Testcontainers — Docker must be running. No manual DB setup required.

## Project Structure

```bash
medical-appointment-system/
├── docker-compose.yml
├── .env.example
├── DECISIONS.md
├── patient-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/medapp/patient_service/
│       │   │   ├── config/          # AuditConfig, OpenApiConfig
│       │   │   ├── controller/      # PatientController
│       │   │   ├── domain/          # Patient entity
│       │   │   ├── dto/             # Request and response records
│       │   │   ├── exception/       # GlobalExceptionHandler
│       │   │   ├── repository/      # PatientRepository
│       │   │   └── service/         # PatientService
│       │   └── resources/
│       │       └── db/migration/    # Flyway SQL scripts
│       └── test/                    # Unit and integration tests
├── appointment-service/             # coming soon
└── notification-service/            # coming soon
```

## API Documentation

### patient-service

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/patients` | List active patients (paginated) |
| GET | `/api/v1/patients/{id}` | Get patient by ID |
| POST | `/api/v1/patients` | Create patient |
| PUT | `/api/v1/patients/{id}` | Update patient |
| DELETE | `/api/v1/patients/{id}` | Soft delete patient |

Full interactive documentation at `http://localhost:8081/swagger-ui.html`

## Architecture Decisions

Key design decisions are documented in [DECISIONS.md](./DECISIONS.md) using the ADR format.

## CI/CD

GitHub Actions runs on every push and pull request to `main` affecting each service:
- Compiles the project
- Runs all tests (unit + integration via Testcontainers)

Branch protection requires CI to pass before merging to `main`.
