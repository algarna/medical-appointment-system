# Architecture Decision Records

## ADR-001 — Soft delete over hard delete

**Date:** 2026-05-05  
**Status:** Accepted

**Context:**  
Patient data has historical and legal value in healthcare systems. Hard deleting a patient record could break referential integrity with appointments and other future services.

**Decision:**  
Implemented soft delete using an `active` boolean flag. Queries filter by `active = true` by default. The record is never physically removed from the database.

**Consequences:**
- `deletedAt` and `deletedBy` fields track when and who performed the deletion.
- All repository queries must explicitly filter by `active = true`.

---

## ADR-002 — Audit fields on every entity

**Date:** 2026-05-05  
**Status:** Accepted

**Context:**  
Traceability is a common requirement in healthcare and regulated industries.

**Decision:**  
All entities include `createdAt`, `updatedAt`, `createdBy` and `lastModifiedBy` via Spring Data JPA Auditing (`@EnableJpaAuditing`). A dedicated `AuditConfig` class registers the `AuditorAware` bean.

**Consequences:**
- `createdBy` / `lastModifiedBy` currently default to `"system"`.
- When Spring Security is added, `AuditorAware` will be updated to read from the security context — no changes needed in the entities.

---

## ADR-003 — One database per service

**Date:** 2026-05-05  
**Status:** Accepted

**Context:**  
Microservices should be independently deployable and loosely coupled.

**Decision:**  
Each service owns its database. No shared schemas, no cross-service foreign keys. The `appointment-service` stores `patientId` as a plain Long and validates patient existence via HTTP call to `patient-service` before creating an appointment.

**Consequences:**
- No referential integrity at the database level across services.
- Eventual consistency is accepted for cross-service data.

---

## ADR-004 — identityNumber over dni

**Date:** 2026-05-05  
**Status:** Accepted

**Context:**  
The system may be used in different countries with different identity document formats.

**Decision:**  
Named the field `identityNumber` instead of `dni` to keep the domain model country-agnostic.

**Consequences:**
- No format validation enforced at the database level beyond uniqueness.
- Format validation can be added per-country via Bean Validation if needed.

---

## ADR-005 — Records for DTOs, no Lombok

**Date:** 2026-05-05  
**Status:** Accepted

**Context:**  
DTOs are immutable data carriers with no business logic. Options considered were Lombok `@Data`/`@Value`, MapStruct, or Java records.

**Decision:**  
Used Java records for all DTOs. Records are immutable by design, require no external dependencies, and are idiomatic Java 16+.

**Consequences:**
- No MapStruct or additional mapping libraries needed at this scale.
- `PatientResponse` includes a static factory method `from(Patient)` to encapsulate mapping logic.
- If the number of entities grows significantly, MapStruct would be the natural next step.

---

## ADR-006 — identityNumber excluded from UpdatePatientRequest

**Date:** 2026-05-05  
**Status:** Accepted

**Context:**  
A patient's identity document number is a stable identifier that should not change once registered, similar to how medical record numbers work in real healthcare systems.

**Decision:**  
`identityNumber` is not included in `UpdatePatientRequest`. It can only be set at creation time.

**Consequences:**
- If a correction is needed, it would require a dedicated endpoint with stricter authorization — not implemented in this version.

---

## ADR-007 — Partial update via null checks, no PATCH library

**Date:** 2026-05-05  
**Status:** Accepted

**Context:**  
`UpdatePatientRequest` has no required fields — all are optional. The service needs to decide which fields to update.

**Decision:**  
The service applies null checks manually: only non-null fields in the request overwrite the existing entity values. No JSON Merge Patch or PATCH-specific libraries used.

**Consequences:**
- Simple and explicit — easy to read and test.
- If the number of updatable fields grows, a more systematic approach like `Optional` fields or JSON Merge Patch would be worth considering.

---

## ADR-008 — Centralized exception handling with @RestControllerAdvice

**Date:** 2026-05-05  
**Status:** Accepted

**Context:**  
Error handling should be consistent across all endpoints. Controllers should not contain try/catch blocks for domain exceptions.

**Decision:**  
All exception handling is centralized in `GlobalExceptionHandler` using `@RestControllerAdvice`. Four cases are covered:
- `PatientNotFoundException` → 404 NOT FOUND
- `IllegalArgumentException` → 409 CONFLICT (used for duplicate email/identityNumber)
- `MethodArgumentNotValidException` → 400 BAD REQUEST with per-field validation errors
- `Exception` (generic fallback) → 500 INTERNAL SERVER ERROR with no internal details exposed

**Consequences:**
- Controllers stay clean — no error handling logic.
- Error responses are consistent in structure across all endpoints.
- `ErrorResponse` is a record defined as an inner class of `GlobalExceptionHandler` — it is only used there, so no separate file is needed.
- Internal error details are never exposed to the client in the generic handler.

---

## ADR-009 — @Transactional(readOnly = true) at class level in services

**Date:** 2026-05-06  
**Status:** Accepted

**Context:**  
Most service methods are read operations. Marking each one individually is error-prone and verbose.

**Decision:**  
`@Transactional(readOnly = true)` is applied at class level in service classes. Write methods override with `@Transactional` explicitly.

**Consequences:**
- All read methods benefit from read-only optimizations automatically.
- Write methods are explicit and visible — easier to audit.

---

## ADR-010 — Duplicate validation before persist

**Date:** 2026-05-06  
**Status:** Accepted

**Context:**  
Email and identityNumber have unique constraints at the database level. Letting the DB throw a constraint violation produces an obscure error for the client.

**Decision:**  
The service validates uniqueness with `existsByEmail` and `existsByIdentityNumber` before persisting. On conflict, throws `IllegalArgumentException` which `GlobalExceptionHandler` maps to 409 CONFLICT with a descriptive message.

**Consequences:**
- Clear, descriptive error messages for the client.
- Slight race condition risk under extreme concurrency — acceptable for this scope.

---

## ADR-011 — Controller responsibility limited to HTTP concerns

**Date:** 2026-05-06  
**Status:** Accepted

**Context:**  
Controllers tend to accumulate business logic over time, making them hard to test and maintain.

**Decision:**  
Controllers handle only HTTP: request mapping, input validation trigger (@Valid), response status, and delegation to the service. No business logic in controllers.

**Consequences:**
- Services are testable without HTTP context.
- Clear separation of concerns across layers.

---

## ADR-012 — @PageableDefault for pagination defaults

**Date:** 2026-05-06  
**Status:** Accepted

**Context:**  
List endpoints need sensible defaults when the client does not specify pagination parameters.

**Decision:**  
`@PageableDefault(size = 20, sort = "lastName")` sets page size to 20 and default sort by last name. Client can override with `?page=0&size=10&sort=firstName`.

**Consequences:**
- Predictable behavior for clients that don't specify pagination.
- No unbounded queries — always paginated.

---

## ADR-013 — Multi-stage Docker build

**Date:** 2026-05-07  
**Status:** Accepted

**Context:**  
A single-stage Docker build using the JDK image produces a large image (~500MB) that includes the compiler and build tools, which are not needed at runtime.

**Decision:**  
Two-stage build: `eclipse-temurin:21-jdk-alpine` for compilation, `eclipse-temurin:21-jre-alpine` for runtime. The final image only contains the JRE and the application jar.

**Consequences:**
- Final image is ~100MB smaller than a single-stage JDK build.
- Reduced attack surface — no compiler or build tools in production.
- Layer caching: dependencies are downloaded before source code is copied, so unchanged dependencies don't trigger a re-download on rebuild.

---

## ADR-014 — Non-root container user

**Date:** 2026-05-07  
**Status:** Accepted

**Context:**  
Running containers as root is a security anti-pattern. If an attacker exploits a vulnerability in the application, they would have root access to the container.

**Decision:**  
A dedicated system user `medapp` is created in the Dockerfile. The application jar is owned by this user and the container runs as `medapp`.

**Consequences:**
- Reduced blast radius if the application is compromised.
- Standard practice for production container deployments.

---

## ADR-015 — Docker profile for container networking

**Date:** 2026-05-07  
**Status:** Accepted

**Context:**  
When running inside Docker Compose, services communicate via container names, not `localhost`. The `local` profile uses `localhost` which only works when running the app directly on the host machine.

**Decision:**  
A dedicated `application-docker.properties` profile sets the datasource URL using `patients-db` as the hostname, matching the Docker Compose service name. Activated via `SPRING_PROFILES_ACTIVE=docker` in docker-compose.yml.

**Consequences:**
- Clean separation between local development and containerized environments.
- `application-docker.properties` is committed to the repo — it contains no credentials, only the hostname reference.

---

## ADR-016 — OpenAPI annotations on controllers only

**Date:** 2026-05-07  
**Status:** Accepted

**Context:**  
Swagger annotations can be added at multiple levels: controller, service, DTO. Over-annotating creates noise and makes code harder to read.

**Decision:**  
OpenAPI annotations are added only at the controller layer: `@Tag` on the class, `@Operation` and `@ApiResponses` on each endpoint, `@Parameter` on path variables. DTOs and services are annotation-free.

**Consequences:**
- Controllers are the HTTP contract — annotations belong there.
- Services and DTOs remain clean and focused on business logic.
- `ErrorResponse` schema is referenced directly from `GlobalExceptionHandler` to keep error documentation consistent.

---

## ADR-017 — WebClient in blocking mode for inter-service HTTP calls

**Date:** 2026-05-09  
**Status:** Accepted

**Context:**  
The appointment-service needs to validate that a patient exists before creating an appointment. Options considered: RestTemplate (deprecated), WebClient (reactive), OpenFeign (declarative).

**Decision:**  
Used WebClient in blocking mode (`.block()`). The appointment-service is a synchronous MVC application, not reactive. WebClient is the modern replacement for RestTemplate, and blocking it keeps the programming model consistent with the rest of the service.

**Consequences:**
- No reactive programming model introduced — simpler codebase.
- WebClient handles connection errors via `WebClientRequestException`, mapped to 503.
- If the system evolves to high concurrency, switching to non-blocking WebClient is straightforward.

---

## ADR-018 — 422 UNPROCESSABLE_CONTENT for cross-service validation failures

**Date:** 2026-05-09  
**Status:** Accepted

**Context:**  
When the appointment-service cannot create an appointment because the referenced patient does not exist, the appropriate HTTP status code is debatable.

**Decision:**  
Returns `422 UNPROCESSABLE_CONTENT` when the patient is not found in patient-service. The request is well-formed and the endpoint exists, but the business rule cannot be satisfied. This is semantically more precise than `404` (which would imply the appointment endpoint doesn't exist) or `400` (which implies a malformed request).

**Consequences:**
- `404` is reserved for appointment not found.
- `422` clearly signals a cross-service business rule violation.
- `503` is reserved for when patient-service is unreachable.

---

## ADR-019 — PATCH for state transitions, not PUT

**Date:** 2026-05-09  
**Status:** Accepted

**Context:**  
Cancelling and completing an appointment are partial updates — only the status and related fields change. Two options: `PUT /appointments/{id}` with a full body, or `PATCH /appointments/{id}/cancel`.

**Decision:**  
Used `PATCH` with sub-resource paths: `PATCH /appointments/{id}/cancel` and `PATCH /appointments/{id}/complete`. This is more expressive and RESTful — the action is explicit in the URL, and the body only carries additional data (cancellation reason).

**Consequences:**
- Adding new state transitions in the future doesn't require changing existing endpoints.
- Clients know exactly what operation they're performing from the URL alone.

---

## ADR-020 — Appointments are immutable once scheduled

**Date:** 2026-05-09  
**Status:** Accepted

**Context:**  
Should patients be able to reschedule appointments (change date, doctor, specialty)?

**Decision:**  
No update endpoint for appointments. The only allowed state transitions are SCHEDULED → CANCELLED and SCHEDULED → COMPLETED. To reschedule, the patient cancels and creates a new appointment. This keeps the appointment history clean and auditable.

**Consequences:**
- No `UpdateAppointmentRequest` DTO needed.
- Full appointment history is preserved — cancellations are never overwritten.
- Simpler state machine: only two valid transitions from SCHEDULED.

---

## ADR-021 — Duplicate appointment detection before persisting

**Date:** 2026-05-09  
**Status:** Accepted

**Context:**  
A patient could accidentally create two appointments with the same doctor at the same date and time.

**Decision:**  
Before persisting, the service checks for an existing non-cancelled appointment with the same `patientId`, `doctorName` and `appointmentDate`. If found, returns `409 CONFLICT`.

**Consequences:**
- Cancelled appointments are excluded from duplicate detection — a patient can rebook after cancelling.
- Slight race condition risk under extreme concurrency — acceptable at this scale.
