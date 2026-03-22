# Clarify — Backend

> A personal finance REST API built with Java Spring Boot, featuring JWT authentication, expense tracking, and ML-powered spending forecasts.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Authentication | JWT (jjwt 0.12) |
| Deployment | Docker + Docker Compose |

## Features

- **JWT Authentication** — Stateless auth with BCrypt password hashing
- **Transaction Management** — Full CRUD for income and expense records
- **Monthly Analytics** — Aggregated spending by category and type
- **Expense Forecasting** — Linear regression model predicting next 3 months of spending (implemented from scratch in Java, no ML libraries)
- **Database Migrations** — Schema versioning with Flyway

## API Endpoints

### Auth
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and receive JWT token |

### Transactions
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/transactions?month=YYYY-MM` | Get all transactions for a month |
| POST | `/api/transactions` | Create a new transaction |
| PUT | `/api/transactions/{id}` | Update a transaction |
| DELETE | `/api/transactions/{id}` | Delete a transaction |

### Stats
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/stats/monthly?month=YYYY-MM` | Monthly income, expenses, savings and category breakdown |
| GET | `/api/stats/trend?months=6` | Historical trend data |

### Forecast
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/forecast?months=3` | Predicted expenses for next N months |

## Getting Started

### Prerequisites
- Docker Desktop

### Run locally

```bash
git clone https://github.com/YOUR_USERNAME/clarify-backend.git
cd clarify-backend
docker-compose up --build
```

The API will be available at `http://localhost:8080`.

### Example request

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123","name":"John"}'

# Response
{"token": "eyJhbGciOiJIUzI1NiJ9..."}
```

## Architecture

```
controller/   → HTTP request handling, input validation
service/      → Business logic
repository/   → Database queries (Spring Data JPA + custom JPQL)
security/     → JWT generation and filter
model/        → JPA entities
dto/          → Request and response objects
config/       → Security, CORS, exception handling
```

## Forecast Model

The linear regression model is implemented from scratch using the least squares formula:

```
slope     = (n·Σxy - Σx·Σy) / (n·Σx² - (Σx)²)
intercept = (Σy - slope·Σx) / n
```

It uses the last 6 months of expense data as training points, with month index as X and total expense as Y, then extrapolates to predict future months.

## Frontend

The React + TypeScript frontend is available at [clarify-frontend](https://github.com/YOUR_USERNAME/clarify-frontend).