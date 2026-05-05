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