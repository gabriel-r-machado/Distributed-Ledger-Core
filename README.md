# 💳 Wallet API - Digital Wallet System

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen?style=flat&logo=spring)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat&logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat&logo=docker)](https://www.docker.com/)
[![AWS](https://img.shields.io/badge/AWS-Free%20Tier-FF9900?style=flat&logo=amazonaws)](https://aws.amazon.com/)

Production-ready digital wallet API with pessimistic locking for concurrent transaction handling, circuit breaker patterns for external service resilience, and cloud-native architecture.

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
| **Docs** | SpringDoc OpenAPI | 2.3.0 |
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
git clone https://github.com/your-username/wallet-api.git
cd wallet-api

# Create .env file (see .env.example)
cp .env.example .env
```

### Run with Docker Compose (Recommended)

```bash
docker-compose up -d

# Check logs
docker-compose logs -f app
```

**Access:**
- API: http://localhost:8081
- Swagger: http://localhost:8081/swagger-ui.html
- Health: http://localhost:8081/actuator/health

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

### Architecture (Free Tier)

```
┌──────────────────────────┐
│  EC2 (t2.micro) / EB     │  ← Docker Container
└──────────┬───────────────┘
           │
┌──────────▼───────────────┐
│  RDS PostgreSQL          │  ← db.t3.micro, 20GB
└──────────────────────────┘
```

### Quick Deploy

See [DEPLOY_CHECKLIST.md](DEPLOY_CHECKLIST.md) for complete guide.

**Summary:**

1. Create RDS PostgreSQL (db.t3.micro, 20GB)
2. Configure security groups (port 8080, 5432)
3. Deploy via Docker or Elastic Beanstalk
4. Set environment variables

```bash
# Docker on EC2
docker run -d \
  -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://rds-endpoint:5432/wallet_db \
  -e DB_USER=admin \
  -e DB_PASSWORD=secure_password \
  wallet-api
```

---

## Security

### Implemented

- BCrypt password hashing (strength 10)
- Environment variables for credentials
- CORS configured
- Centralized exception handling
- Bean Validation on all endpoints
- Non-root Docker user
- Sanitized logs (sensitive data masking)

### Production Checklist

- [ ] Implement JWT or OAuth2
- [ ] Enable HTTPS (Let's Encrypt + Nginx)
- [ ] Add rate limiting
- [ ] Implement audit logs
- [ ] Configure WAF
- [ ] Add 2FA for critical operations

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
wallet/
├── src/
│   ├── main/
│   │   ├── java/com/wallet/wallet/
│   │   │   ├── controllers/      # REST endpoints
│   │   │   ├── services/         # Business logic
│   │   │   ├── repositories/     # Data access
│   │   │   ├── domain/           # Entities
│   │   │   ├── dtos/             # Data transfer objects
│   │   │   └── infra/            # Configuration
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

---

## Contributing

Fork, branch, commit, push, pull request.

---

## License

MIT License - see [LICENSE](LICENSE) file.

---

## Author

**Gabriel Machado**

- LinkedIn: [your-profile](https://linkedin.com/in/your-profile)
- GitHub: [@your-username](https://github.com/your-username)
- Email: your.email@example.com

---

## Additional Documentation

- [AWS Deploy Checklist](DEPLOY_CHECKLIST.md)
- [Pessimistic Lock Testing](PESSIMISTIC_LOCK_TESTING.md)
- [Resilience Patterns](RESILIENCE.md)

---

Built to demonstrate production-grade distributed systems engineering.
