# Clarify — Backend (Java / Spring Boot)

> Personal finance REST API: JWT authentication, expense tracking, real-time budget alerts and a from-scratch linear-regression spending forecast.

🌐 **Live Demo**: https://clarify-frontend-production.up.railway.app (not available now, Sorry)

A Python/FastAPI implementation of the **same API contract** lives in `clarify-backend-python`; the frontend works against either.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 (virtual threads enabled) |
| Database | PostgreSQL 16, Spring Data JPA / Hibernate 6 |
| Migrations | Flyway |
| Authentication | JWT (jjwt 0.12, HS256) + BCrypt, Google Sign-In |
| Real-time | Native WebSocket |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers |
| Docs | springdoc-openapi |
| Ops | Actuator health probes, Docker, GitHub Actions |

## Getting Started

```bash
docker compose up --build          # API on http://localhost:8080
```

Without Docker for the app (Java 21 required; database from compose):

```bash
docker compose up -d db
./mvnw spring-boot:run             # Windows: mvnw.cmd spring-boot:run
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # SQL logging
```

Tests:

```bash
./mvnw verify
```

Unit tests always run. `ApiIntegrationTest` starts a real PostgreSQL with Testcontainers and is skipped automatically when Docker isn't available.

Swagger UI: `http://localhost:8080/api/swagger` · OpenAPI JSON: `/api/docs` · Health: `/api/health`

## API Contract

| Method | Endpoint | Notes |
|---|---|---|
| POST | `/api/auth/register` | `{email, password, name}` → `{token}` |
| POST | `/api/auth/login` | `{email, password}` → `{token}` |
| POST | `/api/auth/google` | `{credential}` (Google ID token) → `{token}` |
| GET | `/api/transactions?month=YYYY-MM` | Caller's transactions; all of them without `month` |
| POST | `/api/transactions` | `{amount, category, description?, type, date}` |
| PUT | `/api/transactions/{id}` | Same body |
| DELETE | `/api/transactions/{id}` | `204` |
| GET | `/api/stats/monthly?month=YYYY-MM` | `{totalIncome, totalExpense, netSavings, byCategory}` |
| GET | `/api/stats/trend?months=6` | Last N months incl. the current one; gaps filled with 0 |
| GET | `/api/forecast?months=3` | `[{month, predictedExpense, isForecast}]` |
| WS | `/ws` | Send `{"type":"auth","token":"<JWT>"}` → `{"type":"AUTH_OK"}`, then `BUDGET_EXCEEDED` pushes. Bad token → close `4401`. |

Errors: `400 {"message"}` for business rules, `400 {"<field>": "<message>"}` for validation, `401 {"message"}` for a missing/invalid token, `404 {"message"}` for a transaction that doesn't exist or belongs to someone else.

## Configuration

| Variable | Default |
|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | `jdbc:postgresql://localhost:5432/expensedb`, `user`, `password` |
| `JWT_SECRET` | dev value — at least 32 bytes, startup fails otherwise. **Change in production.** |
| `GOOGLE_CLIENT_ID` | project client ID |
| `CORS_ORIGINS` | `http://localhost:3000,https://clarify-frontend-production.up.railway.app` |
| `PORT` | `8080` |

Budget limits per category are in `application.yml` under `clarify.budget`.

## Architecture

Package by feature: each folder owns its controller, service, data access and DTOs, so a change to one feature stays in one place.

```
com.clarify
├── auth/          register, login, Google sign-in (GoogleTokenVerifier port + adapter)
├── user/          User entity + repository
├── transaction/   CRUD, domain enums, TransactionCreatedEvent
├── stats/         monthly summary + trend (own read-only query interface)
├── forecast/      LinearRegression model + service
├── alert/         budget rules, WebSocket handler, session registry
├── security/      JWT service, auth filter, 401 entry point
├── common/error/  exception hierarchy + global handler
└── config/        security, CORS, WebSocket, OpenAPI, Clock
```

### Design decisions

**Ownership in the query, not after it.** Every transaction lookup is `findByIdAndUserId`. There is no "load, then check the owner" step to forget, and other users' ids can't be probed (missing and foreign both answer 404).

**Domain events instead of direct calls.** `TransactionService` publishes `TransactionCreatedEvent` and knows nothing about alerts. `BudgetAlertService` listens with `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`: alerts fire only for data that was really committed, the HTTP response doesn't wait for them, and a failing alert can't roll back a saved transaction.

**Typed domain, validated edges.** `Category` and `TransactionType` are enums, not regex-checked strings. Request DTOs are immutable records with Bean Validation; entities have no public setters — state changes go through `create(...)` / `update(...)`.

**Typed configuration that fails fast.** `@ConfigurationProperties` records (JWT, Google, CORS, budget) are validated at startup; a JWT secret that's too short stops the app instead of breaking the first login.

**Injectable `Clock`.** Everything that depends on "today" (budget month, trend window, token expiry, `createdAt`) takes a `Clock`, so tests pin the date instead of racing the calendar.

**Ports for external systems.** Google verification sits behind `GoogleTokenVerifier`; the adapter builds one `GoogleIdTokenVerifier` and reuses it, which caches Google's signing keys. `AuthService.loginWithGoogle` isn't transactional, so no DB connection is held while waiting on Google.

**A predictable error contract.** One `@RestControllerAdvice` maps every failure to the documented shapes. Unknown exceptions return a generic 500 and are logged — internals never reach the client.

**Portable, index-friendly queries.** Months are `[first day, next first day)` ranges in JPQL, not `TO_CHAR(date) = ?`, so `idx_tx_user_date` is usable and the queries aren't tied to Postgres functions.

**Thread-safe WebSocket fan-out.** Alerts are pushed from async threads; each socket is wrapped in `ConcurrentWebSocketSessionDecorator` with send-time and buffer limits so one slow client can't block the others. The token is sent in the first message, not the URL, so it stays out of access logs.

**A tested pyramid.** Pure model (`LinearRegressionTest`), services with mocks and a fixed clock, the WebSocket handler with mocked sessions, and one Testcontainers suite that exercises HTTP → security → validation → JPA → Flyway on real PostgreSQL.


## Frontend

React + TypeScript (Vite): `clarify-frontend`.
