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
