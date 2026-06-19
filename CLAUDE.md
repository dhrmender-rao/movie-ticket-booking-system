# CLAUDE.md — Movie Ticket Booking System

This file is read by Claude Code at the start of every session.
It captures permanent project context so Claude does not need to re-explore the codebase each time.

---

## Project Overview

Spring Boot 3.3 / Java 21 monolithic REST API for movie ticket booking.
PostgreSQL in production, H2 in-memory for tests.
Stateless JWT authentication (JJWT 0.12). No frontend — pure JSON REST API.

---

## Package Structure

```
org.example.ticketbooking
├── authanduser/    user identity — AuthController, AuthService, User entity, LoginRequest, RegisterRequest
├── booking/        core domain — all controllers, services, entities, repos, schedulers, structs
└── common/         infrastructure — SecurityConfig, JwtAuthFilter, JwtUtil, AppException, DataInitializer
```

Structs inside each domain follow: `structs/request/`, `structs/response/`, `structs/internal/`, `structs/enums/`.

---

## Layering Rules (strictly enforced)

- **Controller** → receives request DTO, calls service, maps result to HTTP response. Zero business logic.
- **Service** → converts DTO to entity, runs business logic, returns response DTO. Never returns a JPA entity.
- **Cross-service calls** → pass only primitives, IDs, or DTOs. Never pass a JPA entity across a service boundary.
- **Entities** are internal to each service method. They never appear in any public method signature of a service.

This was a deliberate refactoring step. Do not regress it.

---

## Key Design Decisions

### Seat reservation
Two-phase: `AVAILABLE → HELD (10-min TTL) → BOOKED`.

Hold uses **two concurrency layers**:

1. **`SeatLockRegistry`** (in-memory, `ConcurrentHashMap<seatId, expiresAt>`) — fast-fail gate.
   `tryLockAll()` uses `compute()` (atomic per key) with all-or-nothing rollback.
   Rejects concurrent requests in microseconds without consuming a DB connection.
   Cannot be the only guard: does not survive JVM restart, not shared across app instances.

2. **`PESSIMISTIC_WRITE`** (`SELECT FOR UPDATE` via `@Lock`) — authoritative DB-level guard.
   Correctness source of truth. Cannot be removed regardless of the in-memory layer.

`ShowSeat` has `@Version` as a tertiary optimistic-lock guard.
`HoldExpiryScheduler` runs every 60 s: bulk-releases expired DB holds AND calls `seatLockRegistry.evictExpired()` to keep the map bounded.

### Discount codes
`DiscountService.applyDiscount(String code, BigDecimal totalPrice)` is the single public method.
It validates, calculates, and increments usage atomically in one `@Transactional` call.
Returns `DiscountApplicationResult` (internal DTO with `discountCodeId` + `discountAmount`).
The three old methods (`validateCode`, `calculateDiscount`, `incrementUsage`) were deleted — do not re-add them.

### Notifications
All `NotificationService` methods are `@Async` + `@Transactional(propagation = REQUIRES_NEW)`.
They accept `Long bookingId` — never a `Booking` entity. They reload from DB inside the new transaction.
Emails are simulated: log a `[EMAIL TRIGGERED] To: … | Subject: … | Body: …` line + save a `Notification` row.
`spring-boot-starter-mail` is NOT in the pom — do not add it.

### Pricing
`PricingTier` is keyed on `(show_id, seat_type, day_type)`. Day type is derived from show `startTime` at booking time.
No hardcoded prices. Admins configure tiers via `POST /api/admin/pricing`.

### Refund policies
Stored in `refund_policies` table, evaluated in descending `hoursBeforeShow` order.
Default = 0% when no tier matches. Admin show cancellation always gives 100%.

### Security
JWT: `sub=email`, `claim("role", "CUSTOMER"|"ADMIN")`, 24 h expiry.
Admin endpoints: protected at URL pattern level in `SecurityFilterChain` AND `@PreAuthorize("hasRole('ADMIN')")` on each method.

---

## Spring Profiles

| Profile | When active | DB |
|---|---|---|
| `dev` | default | PostgreSQL `localhost:5432/movieticket` |
| `test` | `mvn test` | H2 in-memory, `create-drop` DDL |

Config files: `application.yaml` (shared base), `application-dev.yaml`, `application-test.yaml`.
Do not put datasource or Hibernate settings in the base `application.yaml`.

---

## Build & Test

```bash
mvn spring-boot:run          # start with dev profile (PostgreSQL required)
mvn test                     # run all 29 tests against H2 (no PostgreSQL needed)
mvn compile                  # compile check only
```

Expected: **29 tests, BUILD SUCCESS**.

Test layout:
- `src/test/java/.../common/service/` — unit tests: `AuthServiceTest`
- `src/test/java/.../service/` — unit tests: `DiscountServiceTest`, `RefundPolicyServiceTest`, `SeatHoldServiceTest`
- `src/test/java/.../integration/` — `BookingFlowIntegrationTest` (full HTTP stack, H2)

---

## Seed Data (DataInitializer)

On every startup:
- Admin user: `admin@movieticket.com` / `admin123` (idempotent — guarded by `existsByEmail`)

Refund policies are NOT auto-seeded — create via `POST /api/admin/refund-policies` or apply `schema.sql`.

---

## What NOT to do

- Do not add `spring-boot-starter-mail` or any SMTP config.
- Do not pass JPA entities across service method boundaries.
- Do not re-introduce `validateCode()`, `calculateDiscount()`, or `incrementUsage()` as separate public methods on `DiscountService`.
- Do not add helper methods like `getShowById(Long)` that return entities — they were deleted as dead code.
- Do not add mocks for the database in integration tests — they use a real H2 context.
- Do not remove the DB `PESSIMISTIC_WRITE` lock thinking the in-memory `SeatLockRegistry` is sufficient — the DB lock is the correctness guarantee; the in-memory layer is a performance optimisation only.
- Do not put dev/test-specific settings in `application.yaml`.

---

## Files Worth Reading First

When picking up a new task, these files give the fastest orientation:

| File | Why |
|---|---|
| `booking/service/BookingService.java` | Central orchestrator — hold, book, pay, cancel |
| `booking/service/SeatLockRegistry.java` | In-memory fast-fail gate (ConcurrentHashMap) |
| `booking/service/SeatHoldService.java` | Two-layer hold: SeatLockRegistry → DB pessimistic lock |
| `booking/service/DiscountService.java` | Atomic discount flow |
| `booking/service/NotificationService.java` | Async + REQUIRES_NEW pattern |
| `common/config/SecurityConfig.java` | Auth rules, public vs protected endpoints |
| `common/config/DataInitializer.java` | What gets seeded on startup |
| `src/test/java/.../integration/BookingFlowIntegrationTest.java` | End-to-end happy path + concurrency test |
