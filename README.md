# 🏦 Digital Wallet API - High-Performance Financial Engine

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen?style=flat&logo=spring)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat&logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat&logo=docker)](https://www.docker.com/)
[![AWS](https://img.shields.io/badge/AWS-Production-FF9900?style=flat&logo=amazonaws)](http://wallet-prod.eba-myky7k43.us-east-2.elasticbeanstalk.com/swagger-ui/index.html)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE.md)

Production-ready digital wallet REST API with pessimistic locking for concurrent transaction handling, circuit breaker patterns for external service resilience, and cloud-native architecture.

**🌐 Live Production:** [http://wallet-prod.eba-myky7k43.us-east-2.elasticbeanstalk.com](http://wallet-prod.eba-myky7k43.us-east-2.elasticbeanstalk.com/swagger-ui/index.html)

---

## Overview

Digital wallet REST API built to handle high-concurrency scenarios in financial transactions. Implements pessimistic locking at the database level to prevent race conditions, ensuring data consistency when multiple users attempt simultaneous operations.

### Technical Challenge Solved

Financial systems face critical issues under concurrent load:
- Negative balances from simultaneous withdrawals
- Lost updates and phantom reads
- Data inconsistency across distributed transactions

**Implementation:**
- PostgreSQL `SELECT FOR UPDATE` for row-level locking
- `READ_COMMITTED` isolation level
- Circuit breaker for external authorization service
- Bean Validation for input sanitization
- Stress-tested with 100+ concurrent threads

---

## Architecture

```
┌─────────────────┐
│   Controllers   │  ← REST API + Bean Validation
└────────┬────────┘
         │
┌────────▼────────┐
│    Services     │  ← Business Logic + @Transactional
└────────┬────────┘
         │
┌────────▼────────┐
│  Repositories   │  ← Pessimistic Locking (FOR UPDATE)
└────────┬────────┘
         │
┌────────▼────────┐
│   PostgreSQL    │  ← ACID Transactions
└─────────────────┘
```

### Concurrency Flow

```sql
-- Thread 1 acquires lock
SELECT * FROM wallets WHERE user_id = 'abc123' FOR UPDATE;
-- Threads 2-5 wait in queue

-- Thread 1 validates, updates, commits
UPDATE wallets SET balance = balance - 100 WHERE id = 'xyz';
COMMIT;
-- Lock released → Thread 2 proceeds with updated balance
```

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Language** | Java | 21 |
| **Framework** | Spring Boot | 3.4.1 |
| **Database** | PostgreSQL | 16 |
| **ORM** | Hibernate/JPA | 6.x |
| **Build** | Maven | 3.x |
| **Containerization** | Docker + Compose | - |
| **Resilience** | Resilience4j | 2.1.0 |
| **Security** | Spring Security (BCrypt) | 6.x |
| **Docs** | SpringDoc OpenAPI | 2.8.3 |
| **Monitoring** | Spring Actuator | - |
| **Testing** | JUnit 5, Mockito, Testcontainers | - |
| **Cloud** | AWS EC2, RDS, Elastic Beanstalk | - |

---

## Quick Start

### Prerequisites

- Java 21+ ([Download](https://adoptium.net/))
- Docker & Docker Compose ([Download](https://www.docker.com/))
- Maven (optional - wrapper included)

### Clone & Configure

```bash
git clone https://github.com/gabriel-r-machado/Distributed-Ledger-Core.git
cd Distributed-Ledger-Core

# Create .env file (see .env.example)
cp .env.example .env
```

### Run with Docker Compose (Recommended)

```bash
docker-compose up -d

# Check logs
docker-compose logs -f app
```

**Access (Local):**
- API: http://localhost:8081
- Swagger: http://localhost:8081/swagger-ui.html
- Health: http://localhost:8081/actuator/health

**Access (Production - AWS):**
- 🌐 API: http://wallet-prod.eba-myky7k43.us-east-2.elasticbeanstalk.com
- 📚 Swagger/API Docs: http://wallet-prod.eba-myky7k43.us-east-2.elasticbeanstalk.com/swagger-ui/index.html
- 💚 Health Check: http://wallet-prod.eba-myky7k43.us-east-2.elasticbeanstalk.com/actuator/health

### Run Locally

```bash
# PostgreSQL via Docker (database only)
docker-compose up -d postgres

# Run application
./mvnw spring-boot:run
```

---

## API Reference

### Endpoints

#### Users

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/users` | Create new user | ❌ |
| `GET` | `/users` | List all users | ❌ |
| `GET` | `/users/{id}` | Get user by ID | ❌ |

#### Transactions

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/transactions` | Create transaction | ❌ |

> **Note:** MVP has authentication disabled. Production requires JWT/OAuth2.

---

### Examples

#### Create Common User

```bash
POST http://localhost:8081/users
Content-Type: application/json

{
  "firstName": "João",
  "lastName": "Silva",
  "document": "12345678901",
  "email": "joao@email.com",
  "password": "senha123",
  "balance": 1000.00,
  "userType": "COMMON"
}
```

#### Create Merchant

```bash
POST http://localhost:8081/users
Content-Type: application/json

{
  "firstName": "Store",
  "lastName": "ABC",
  "document": "12345678000190",
  "email": "store@email.com",
  "password": "senha123",
  "balance": 0.00,
  "userType": "MERCHANT"
}
```

#### Create Transaction

```bash
POST http://localhost:8081/transactions
Content-Type: application/json

{
  "value": 100.50,
  "senderId": "550e8400-e29b-41d4-a716-446655440000",
  "receiverId": "660e8400-e29b-41d4-a716-446655440002"
}
```

---

### Business Rules

1. Merchants cannot send transfers (receive only)
2. Insufficient balance blocks transaction
3. External authorization validates each transfer (circuit breaker + retry)
4. Input validation:
   - CPF: 11 digits
   - CNPJ: 14 digits
   - Valid email format
   - Amount > 0
   - Password min 6 characters

---

## Testing

### Run All Tests

```bash
./mvnw test
```

### Test Coverage

| Type | Coverage | Tools |
|------|----------|-------|
| **Unit** | `TransactionService`, `AuthorizationService` | JUnit 5 + Mockito |
| **Integration** | `PostgresConcurrencyTest` (100 threads) | Testcontainers |
| **Concurrency** | Race condition scenarios | ExecutorService |

### Manual Concurrency Test

```bash
# Windows
.\test-concurrency.ps1

# Linux/Mac
./test-concurrency.sh
```

---

## AWS Deployment

### 🚀 Currently Running in Production

**Live Environment:** http://wallet-prod.eba-myky7k43.us-east-2.elasticbeanstalk.com

### Architecture (AWS Free Tier)

```
┌──────────────────────────────────────┐
│  Elastic Beanstalk (t2.micro)        │  ← Docker Container (wallet-prod)
│  US-East-2 (Ohio)                    │
└──────────┬───────────────────────────┘
           │
┌──────────▼───────────────────────────┐
│  RDS PostgreSQL                      │  ← db.t3.micro, 20GB
│  Multi-AZ disabled (cost savings)    │
└──────────────────────────────────────┘
```

### Deployment Guide

**Summary:**

1. Create RDS PostgreSQL (db.t3.micro, 20GB)
2. Configure security groups (port 8080, 5432)
3. Deploy via Docker or Elastic Beanstalk
4. Set environment variables

```bash
# Docker on EC2
docker run -d \
  -p 8081:8080 \
  -e DB_URL=jdbc:postgresql://rds-endpoint:5432/wallet_db \
  -e DB_USER=admin \
  -e DB_PASSWORD=secure_password \
  wallet-api
```

---

## Security

### ✅ Currently Implemented

- **Password Security:** BCrypt hashing (strength 10)
- **Configuration:** Environment variables for sensitive credentials
- **CORS:** Properly configured for cross-origin requests
- **Exception Handling:** Centralized error responses (no stack traces exposed)
- **Input Validation:** Bean Validation (@Valid) on all endpoints
- **Container Security:** Non-root Docker user
- **Logging:** Sanitized logs (no sensitive data exposure)

### ⚠️ MVP Limitations

- Authentication is disabled for development (`permitAll()` in SecurityConfig)
- No API rate limiting yet
- HTTPS not configured (use reverse proxy in production)

### 🚀 Roadmap & Future Improvements

> ⚠️ **Status:** This is an actively developed MVP demonstrating core architectural patterns for high-concurrency financial systems.

While the current implementation successfully handles concurrent transactions with pessimistic locking and circuit breaker patterns, the following features are planned:

**Security Enhancements:**
- [ ] Migrate from Basic Auth to **OAuth2/JWT** with Spring Security
- [ ] Add API key authentication for service-to-service communication
- [ ] Implement SSL/TLS (HTTPS) using AWS ACM or Let's Encrypt

**Resilience & Performance:**
- [ ] Add Rate Limiting using Bucket4j
- [ ] Implement caching layer with Redis
- [ ] Add message queue (RabbitMQ/Kafka) for async processing

**Observability:**
- [ ] Integration with Prometheus + Grafana dashboards
- [ ] Distributed tracing with OpenTelemetry
- [ ] Enhanced logging with ELK Stack

---

## Monitoring

### Actuator Endpoints

```bash
# Health check
GET /actuator/health

# Metrics
GET /actuator/metrics

# Circuit breaker status
GET /actuator/health/circuitBreakers
```

### Logs

```bash
# Docker Compose
docker-compose logs -f app

# Container
docker logs -f wallet-api
```

---

## Project Structure

```
Distributed-Ledger-Core/
├── src/
│   ├── main/
│   │   ├── java/com/wallet/wallet/
│   │   │   ├── controllers/      # REST endpoints
│   │   │   ├── services/         # Business logic
│   │   │   ├── repositories/     # Data access
│   │   │   ├── domain/           # Entities
│   │   │   ├── dtos/             # Data transfer objects
│   │   │   └── infra/            # Configuration, security
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/com/wallet/wallet/services/
│       └── resources/
├── docs/
│   ├── PESSIMISTIC_LOCK_TESTING.md
│   └── RESILIENCE.md
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── test-concurrency.ps1
├── test-concurrency.sh
└── README.md
```

---

## Contributing

Fork, branch, commit, push, pull request.

---

## License

MIT License - see [LICENSE](LICENSE.md) file.

---

## Author

**Gabriel Machado**

- LinkedIn: [gabrielmachado-se](https://www.linkedin.com/in/gabrielmachado-se/)
- GitHub: [@gabriel-r-machado](https://github.com/gabriel-r-machado)

---

## Additional Documentation

- [Pessimistic Lock Testing](docs/PESSIMISTIC_LOCK_TESTING.md)
- [Resilience Patterns](docs/RESILIENCE.md)

---

Built to demonstrate production-grade distributed systems engineering.
