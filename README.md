# Medical Appointment System

Microservices-based system for managing medical appointments, built with Java 21, Spring Boot 4 and Apache Kafka.

## Services

| Service | Description | Port | Status |
|---|---|---|---|
| `patient-service` | Patient management (CRUD) | 8081 | ✅ Done |
| `appointment-service` | Appointment scheduling and cancellation | 8082 | ✅ Done |
| `notification-service` | Event-driven notifications via Kafka | 8083 | ✅ Done |

## Tech Stack

- **Java 21** + **Spring Boot 4**
- **PostgreSQL 16** — one database per service (patient-service, appointment-service)
- **Apache Kafka 3.9** (KRaft) — async communication between services
- **Flyway** — versioned database migrations
- **Docker / Docker Compose** — containerized local environment
- **Swagger / OpenAPI** — API documentation on each service
- **Testcontainers** — integration tests with real PostgreSQL and Kafka

## Architecture

```
Client
  │
  ├── patient-service (:8081) ──► patients-db (PostgreSQL :5432)
  │
  ├── appointment-service (:8082) ──► appointments-db (PostgreSQL :5433)
  │         │  (sync) GET /patients/{id}          │
  │         └──────────────────────────────────────┘
  │         │  (async) appointment.created
  │         │          appointment.cancelled
  │         ▼
  │        Kafka (:9092)
  │         │
  └── notification-service (:8083) ◄── consumes events, simulates email
```

**Synchronous communication:** `appointment-service` validates patient existence via HTTP before persisting an appointment.

**Asynchronous communication:** `appointment-service` publishes `appointment.created` and `appointment.cancelled` events to Kafka. `notification-service` consumes them and simulates sending confirmation/cancellation emails.

## Getting Started

### Prerequisites

- Docker Desktop
- Java 21 (via [SDKMAN](https://sdkman.io): `sdk install java 21.0.7-tem`)

### Run the full stack

```bash
cp .env.example .env    # fill in your values
docker compose up -d
```

Services available at:
- patient-service: `http://localhost:8081`
- appointment-service: `http://localhost:8082`
- notification-service: `http://localhost:8083`

### Run a service locally (for development)

```bash
# Start infrastructure only
docker compose up -d patients-db appointments-db kafka

# Run a service
cd patient-service
./mvnw spring-boot:run
```

### Run tests

```bash
cd patient-service && ./mvnw test
cd appointment-service && ./mvnw test
cd notification-service && ./mvnw test
```

Tests use Testcontainers — Docker must be running. No manual setup required.

## Project Structure

```
medical-appointment-system/
├── docker-compose.yml
├── .env.example
├── DECISIONS.md
├── patient-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/medapp/patient_service/
│       │   ├── config/          # OpenApiConfig
│       │   ├── controller/      # PatientController
│       │   ├── domain/          # Patient entity
│       │   ├── dto/             # Request and response records
│       │   ├── exception/       # GlobalExceptionHandler
│       │   ├── repository/      # PatientRepository
│       │   └── service/         # PatientService
│       └── test/                # Unit and integration tests
├── appointment-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/medapp/appointment_service/
│       │   ├── config/          # KafkaConfig, OpenApiConfig
│       │   ├── controller/      # AppointmentController
│       │   ├── domain/          # Appointment entity, AppointmentStatus enum
│       │   ├── dto/             # Request and response records
│       │   ├── event/           # AppointmentCreatedEvent, AppointmentCancelledEvent
│       │   ├── exception/       # GlobalExceptionHandler
│       │   ├── repository/      # AppointmentRepository
│       │   └── service/         # AppointmentService, AppointmentEventProducer, PatientValidationService
│       └── test/                # Unit and integration tests
└── notification-service/
    ├── Dockerfile
    ├── pom.xml
    └── src/
        ├── main/java/com/medapp/notification_service/
        │   ├── config/          # KafkaConfig
        │   ├── consumer/        # AppointmentEventConsumer
        │   ├── event/           # AppointmentCreatedEvent, AppointmentCancelledEvent
        │   └── service/         # NotificationService
        └── test/                # Unit and integration tests
```

## API Documentation

### patient-service — `http://localhost:8081/swagger-ui.html`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/patients` | List active patients (paginated) |
| GET | `/api/v1/patients/{id}` | Get patient by ID |
| POST | `/api/v1/patients` | Create patient |
| PUT | `/api/v1/patients/{id}` | Update patient |
| DELETE | `/api/v1/patients/{id}` | Soft delete patient |

### appointment-service — `http://localhost:8082/swagger-ui.html`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/appointments` | List appointments (paginated) |
| GET | `/api/v1/appointments/{id}` | Get appointment by ID |
| GET | `/api/v1/appointments/patient/{patientId}` | List appointments by patient |
| GET | `/api/v1/appointments/status/{status}` | List appointments by status |
| POST | `/api/v1/appointments` | Create appointment |
| PATCH | `/api/v1/appointments/{id}/cancel` | Cancel appointment |
| PATCH | `/api/v1/appointments/{id}/complete` | Complete appointment |

### notification-service — `http://localhost:8083/swagger-ui.html`

No REST endpoints. Consumes Kafka events and logs simulated email output.

## Kafka Topics

| Topic | Producer | Consumer | Description |
|---|---|---|---|
| `appointment.created` | appointment-service | notification-service | Published after a new appointment is persisted |
| `appointment.cancelled` | appointment-service | notification-service | Published after an appointment is cancelled |
| `appointment.created.DLT` | notification-service (error handler) | — | Dead letter topic for failed created-event processing |
| `appointment.cancelled.DLT` | notification-service (error handler) | — | Dead letter topic for failed cancelled-event processing |

## Inter-service Communication

**Synchronous (HTTP):**
```
POST /api/v1/appointments
  → GET http://patient-service:8081/api/v1/patients/{patientId}
  → 200: proceed
  → 404: return 422 UNPROCESSABLE_CONTENT
  → unreachable: return 503 SERVICE_UNAVAILABLE
```

**Asynchronous (Kafka):**
```
appointment-service publishes → appointment.created / appointment.cancelled
notification-service consumes → simulates email, routes failures to DLT after 3 attempts
```

## Architecture Decisions

Key design decisions are documented in [DECISIONS.md](./DECISIONS.md) using the ADR format.

## CI/CD

GitHub Actions runs on every push and pull request to `main`:

| Workflow | Triggers on |
|---|---|
| `patient-service-ci` | Changes in `patient-service/**` |
| `appointment-service-ci` | Changes in `appointment-service/**` |
| `notification-service-ci` | Changes in `notification-service/**` |

Branch protection requires CI to pass before merging to `main`.
