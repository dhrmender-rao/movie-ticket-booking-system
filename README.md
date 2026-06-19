# Movie Ticket Booking System

A production-grade Spring Boot REST API for a multi-city, multi-theater movie ticket booking platform.
Covers the full booking lifecycle — browsing shows, reserving seats, paying, and cancelling — with
concurrent seat safety, role-based security, data-driven pricing, tiered refunds, discount codes,
and async notifications.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 21 |
| Framework | Spring Boot 3.3 |
| Database (prod) | PostgreSQL 14+ |
| Database (tests) | H2 in-memory |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security 6 + JWT (JJWT 0.12) |
| Validation | Jakarta Bean Validation |
| Async | Spring `@Async` (ThreadPoolTaskExecutor) |
| Scheduler | Spring `@Scheduled` |
| Build | Maven 3.x |
| Testing | JUnit 5 · Mockito · MockMvc |

---

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 14+ on `localhost:5432`

### Database

```bash
# Option A — Docker (recommended)
docker run -d --name movieticket-pg \
  -e POSTGRES_DB=movieticket \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:16

# Option B — local psql
psql -U postgres -c "CREATE DATABASE movieticket;"
```

### Run the application

```bash
mvn spring-boot:run
# Dev profile is active by default
# App starts on http://localhost:8080
```

On first boot the app seeds:
- Admin user: `admin@movieticket.com` / `admin123`

Refund policies, cities, theaters, and shows must be created via the Admin API (or applied via `schema.sql`).

### Run tests

```bash
mvn test
# Uses H2 in-memory — no PostgreSQL required
# 29 tests, all passing
```

### Apply SQL schema manually (optional)

```bash
psql -U postgres -d movieticket -f schema.sql
```

---

## Package Structure

```
org.example.ticketbooking
│
├── authanduser/              User identity and authentication
│   ├── controller/           AuthController
│   ├── service/              AuthService, UserDetailsServiceImpl
│   ├── entity/               User
│   ├── repository/           UserRepository
│   └── structs/
│       ├── request/          LoginRequest, RegisterRequest
│       ├── response/         AuthResponse
│       └── enums/            Role
│
├── booking/                  Core domain
│   ├── controller/           AdminCityController, AdminDiscountController,
│   │                         AdminMovieController, AdminPricingController,
│   │                         AdminRefundPolicyController, AdminScreenController,
│   │                         AdminShowController, BookingController,
│   │                         CityController, MovieController,
│   │                         PaymentController, SeatController, ShowController
│   ├── service/              BookingService, DiscountService, MovieService,
│   │                         NotificationService, PaymentService, PricingService,
│   │                         RefundPolicyService, ScreenService, SeatHoldService,
│   │                         ShowService, TheaterService
│   ├── entity/               Booking, BookingItem, City, DiscountCode, Movie,
│   │                         Notification, Payment, PricingTier, RefundPolicy,
│   │                         Screen, Seat, Show, ShowSeat, Theater
│   ├── repository/           (one interface per entity)
│   ├── scheduler/            HoldExpiryScheduler, ReminderScheduler
│   └── structs/
│       ├── request/          CreateBookingRequest, CreateDiscountCodeRequest,
│       │                     CreateMovieRequest, CreateShowRequest, …
│       ├── response/         BookingResponse, ShowResponse, ShowSeatResponse, …
│       ├── internal/         DiscountApplicationResult
│       └── enums/            BookingStatus, DayType, DiscountType,
│                             PaymentStatus, SeatType, ShowSeatStatus, …
│
└── common/                   Cross-cutting infrastructure
    ├── config/               AsyncConfig, DataInitializer, SchedulerConfig,
    │                         SecurityConfig
    ├── exception/            AppException, GlobalExceptionHandler
    └── security/             JwtAuthFilter, JwtUtil
```

**Layering rule:** Controllers accept request DTOs and delegate to services. Services own all
business logic — they convert DTOs to entities internally and return response DTOs. Entities never
cross a service method boundary. Cross-service calls pass only IDs or primitives.

---

## API Reference

### Auth (public)

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Register as customer, returns JWT |
| POST | `/api/auth/login` | Login (admin or customer), returns JWT |

### Browse (no auth required)

| Method | Path | Description |
|---|---|---|
| GET | `/api/cities` | List all cities |
| GET | `/api/cities/{cityId}/shows` | Shows in a city (`?movieId=&date=YYYY-MM-DD`) |
| GET | `/api/shows/{id}` | Show details |
| GET | `/api/shows/{showId}/seats` | Live seat map (AVAILABLE / HELD / BOOKED) |
| GET | `/api/movies` | List movies |
| GET | `/api/movies/{id}` | Movie details |

### Customer (JWT required — ROLE_CUSTOMER)

| Method | Path | Description |
|---|---|---|
| POST | `/api/shows/{showId}/seats/hold` | Hold seats for 10 min |
| POST | `/api/bookings` | Create booking from held seats |
| POST | `/api/payments/{bookingId}/pay` | Pay for booking |
| GET | `/api/bookings` | My booking history |
| GET | `/api/bookings/{id}` | Booking details |
| DELETE | `/api/bookings/{id}` | Cancel booking + refund |

### Admin (JWT required — ROLE_ADMIN)

| Method | Path | Description |
|---|---|---|
| POST | `/api/admin/cities` | Create city |
| POST | `/api/admin/theaters` | Create theater |
| POST | `/api/admin/screens` | Create screen (auto-generates seats A–Z × N cols) |
| POST | `/api/admin/movies` | Add movie |
| GET | `/api/admin/movies` | List movies |
| POST | `/api/admin/shows` | Schedule a show |
| DELETE | `/api/admin/shows/{id}` | Cancel show (100% refund for all confirmed bookings) |
| POST | `/api/admin/pricing` | Set pricing tier (show + seat type + day type) |
| GET | `/api/admin/pricing/show/{showId}` | Get pricing for a show |
| POST | `/api/admin/discount-codes` | Create discount code |
| GET | `/api/admin/discount-codes` | List discount codes |
| PUT | `/api/admin/discount-codes/{id}/deactivate` | Deactivate a code |
| POST | `/api/admin/refund-policies` | Create refund policy tier |
| GET | `/api/admin/refund-policies` | List refund policies |
| DELETE | `/api/admin/refund-policies/{id}` | Delete refund policy tier |

---

## Core Design Decisions

### Seat reservation — two-phase with dual-layer locking

Seat status transitions: `AVAILABLE → HELD → BOOKED`

1. `POST /seats/hold` runs through two concurrency layers before writing anything:
2. `POST /bookings` validates the held seats belong to the caller and creates the booking as `PENDING`.
3. `POST /payments/{id}/pay` confirms payment and moves seats to `BOOKED`.

**Layer 1 — In-memory fast-fail (`SeatLockRegistry`)**

A `ConcurrentHashMap<seatId, expiresAt>` acts as a gate before any DB connection is used.
`tryLockAll()` uses `ConcurrentHashMap.compute()` (atomic per key) with all-or-nothing semantics:
if any seat is already claimed, every seat acquired in the same call is rolled back and the request
returns `409 CONFLICT` immediately — in microseconds, with zero DB connections consumed.

Without this layer, 1000 concurrent requests for the same seat would all queue on the DB row lock,
exhausting the connection pool. With it, 999 fail in memory and only 1 reaches the database.

**Layer 2 — DB pessimistic lock (`SELECT FOR UPDATE`)**

`ShowSeatRepository.findAllByIdWithLock()` is annotated `@Lock(LockModeType.PESSIMISTIC_WRITE)`,
which appends `FOR UPDATE` to the SQL. This is the authoritative correctness guarantee and cannot
be removed because the in-memory map does not survive a JVM restart and is not shared across
multiple app instances.

**Why both are needed**

| Scenario | In-memory layer | DB layer |
|---|---|---|
| 1000 requests, same JVM | Stops 999 at the gate | Confirms the 1 winner |
| App restart (map wiped) | Gone — passes everything | Still enforces HELD status |
| 2 app instances, load balancer | Each has its own map — both pass | Blocks the second at DB |

`ShowSeat` also carries `@Version` (optimistic lock) as a tertiary guard against any edge case
that slips past both layers.

`HoldExpiryScheduler` runs every 60 seconds: releases expired DB holds via a single bulk-update
query AND evicts expired entries from the in-memory map to keep it bounded.

### Dynamic, data-driven pricing

Price = `PricingTier(show, seatType, dayType)`. Day type (WEEKDAY / WEEKEND) is derived from the
show's `startTime`. Admins configure tiers at runtime via the API — no code changes needed.

### Tiered refund policies

Policies live in the `refund_policies` table and are evaluated in descending `hoursBeforeShow` order:

| Hours before show | Refund |
|---|---|
| ≥ 48 h | 100% |
| 24 – 48 h | 50% |
| 2 – 24 h | 25% |
| < 2 h | 0% |

Admin show cancellation always triggers 100% refund regardless of policy.

### Atomic discount application

`DiscountService.applyDiscount(code, totalPrice)` validates the code, calculates the discount amount,
and increments the usage counter atomically inside a single `@Transactional` call. This prevents
race conditions where two concurrent requests redeem the same single-use code.

### Async notifications with transaction isolation

`NotificationService` methods are `@Async` and `@Transactional(propagation = REQUIRES_NEW)`:
- A notification failure never rolls back the parent booking/payment transaction.
- The async thread opens its own transaction, accepts only a `bookingId`, and reloads the booking
  from DB — avoiding `LazyInitializationException` in a detached-entity context.
- Emails are simulated: a structured `[EMAIL TRIGGERED]` line is logged and a row is saved to the
  `notifications` table. No SMTP server required.

---

## Configuration

Three YAML files, selected by Spring profile:

| File | Profile | Purpose |
|---|---|---|
| `application.yaml` | (base, always loaded) | App name, port, JWT secret/expiry, hold TTL |
| `application-dev.yaml` | `dev` (default) | PostgreSQL datasource, Hibernate `update` DDL, SQL logging |
| `application-test.yaml` | `test` | H2 datasource, `create-drop` DDL, payment always-succeed |

Switch profile: `SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run`

Key properties:

```yaml
app:
  jwt:
    secret: <base64-256-bit-key>
    expiration-ms: 86400000     # 24 h
  hold:
    ttl-minutes: 10
  payment:
    always-succeed: false       # set true to skip simulated 10% failure rate
```

---

## Assumptions

1. Hold TTL is 10 minutes (configurable via `app.hold.ttl-minutes`).
2. Seats are auto-generated when a Screen is created — rows A–Z × N columns. The first `premiumRowsFromFront` rows are `PREMIUM`; the rest are `REGULAR`.
3. Payment is mocked with a 90% success rate. Set `app.payment.always-succeed=true` for deterministic behaviour.
4. Notifications are log-based — no real SMTP server needed.
5. Default refund is 0% when no policy matches the cancellation window.
6. One booking can contain multiple seats from the same show.
7. Discount codes are show-agnostic and valid across all shows within their date range.
8. Weekend = Saturday or Sunday by show start time (not booking time).
9. Admin show cancellation triggers 100% refund for all confirmed bookings, overriding any refund policy.
10. Reminder window: shows starting 24h – 24h 10min from scheduler run time receive a reminder.

---

## Test Coverage

| Test class | Type | Scenarios covered |
|---|---|---|
| `AuthServiceTest` | Unit | Register, login, duplicate email (409), wrong password (401), user not found |
| `DiscountServiceTest` | Unit | Valid % code, valid flat code, flat capped at total, null/blank code, inactive, max uses, expired, not yet valid |
| `SeatHoldServiceTest` | Unit | Successful hold, already-booked conflict, held-by-other conflict, expired hold re-holdable, wrong show |
| `RefundPolicyServiceTest` | Unit | 100% ≥48h, 50% at 24–48h, 25% at 2–24h, 0% <2h, no policies → zero |
| `BookingFlowIntegrationTest` | Integration | Full flow (hold→book→pay→cancel), discount codes, concurrent hold (409), admin show cancellation |
| `MovieTicketBookingApplicationTests` | Integration | Spring context loads cleanly |

**29 tests, all passing.** Unit tests use Mockito with no Spring context. Integration tests use
`@SpringBootTest` + `MockMvc` + H2 — no external dependencies.

---

## Key Files

| File | Purpose |
|---|---|
| `booking/entity/ShowSeat.java` | Tracks seat status + hold TTL; carries `@Version` |
| `booking/service/SeatLockRegistry.java` | In-memory fast-fail gate (ConcurrentHashMap) |
| `booking/service/SeatHoldService.java` | Two-layer hold: in-memory fast-fail + DB pessimistic lock |
| `booking/service/BookingService.java` | Full booking lifecycle |
| `booking/service/DiscountService.java` | Atomic discount validate-calculate-increment |
| `booking/service/PaymentService.java` | Mock payment + refund processing |
| `booking/service/NotificationService.java` | `@Async` notification dispatch |
| `booking/scheduler/HoldExpiryScheduler.java` | Releases stale holds every 60 s |
| `booking/scheduler/ReminderScheduler.java` | Sends show reminders every 10 min |
| `common/config/DataInitializer.java` | Seeds admin user + default refund policies on startup |
| `common/security/JwtUtil.java` | JWT generation and validation |
| `common/config/SecurityConfig.java` | Filter chain, CORS, role-based URL protection |
