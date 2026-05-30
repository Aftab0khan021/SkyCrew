# ✈️ SkyCrew — Aviation Crew Roster & Conflict Resolution API

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue?logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Multi--Stage-2496ED?logo=docker)](https://www.docker.com/)
[![AWS](https://img.shields.io/badge/AWS-EC2%20%2B%20RDS-FF9900?logo=amazonaws)](https://aws.amazon.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

> A production-grade RESTful API for managing aviation crew scheduling with **8 automated conflict detection rules**, JWT authentication, multi-tenancy, and full AWS deployment.

### 🔗 [Live Demo (Swagger UI)](http://65.2.177.129:8080/swagger-ui/index.html) &nbsp;|&nbsp; 📄 [API Docs](http://65.2.177.129:8080/v3/api-docs)

---

## 🏗️ Architecture & Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Java 17 (LTS) |
| **Framework** | Spring Boot 3.5, Spring Security 6, Spring Data JPA |
| **Database** | PostgreSQL 18.3 (AWS RDS), H2 (dev), Flyway (6 migrations) |
| **Auth** | Stateless JWT (JJWT 0.12.6) + BCrypt, Role-based (ADMIN / CREW) |
| **Rate Limiting** | Bucket4j — IP-based Token Bucket algorithm |
| **API Docs** | SpringDoc OpenAPI 3.1 (Swagger UI) |
| **Monitoring** | Spring Boot Actuator (health checks) |
| **Email** | Spring Boot Mail (notification system) |
| **Containerization** | Docker — multi-stage build, non-root user |
| **Cloud** | AWS EC2 (compute) + RDS (managed PostgreSQL) |
| **Testing** | JUnit 5, Mockito — unit + integration tests |
| **Build** | Maven 3.9 |

---

## 🧠 Smart Rostering Engine — 8 Business Rules

The core engine enforces **8 aviation safety rules** before any crew-to-flight assignment:

| # | Rule | Conflict Type | Action |
|---|------|--------------|--------|
| 1 | **Overlap Detection** — No crew member on two flights at the same time | `OVERLAP` | ❌ Reject |
| 2 | **Fatigue Management** — Minimum rest period between consecutive flights | `FATIGUE` | ❌ Reject |
| 3 | **Monthly Hours Cap** — Cannot exceed configurable max flying hours/month | `HOURS_EXCEEDED` | ❌ Reject |
| 4 | **90% Threshold Warning** — Alert when crew reaches 90% of monthly limit | — | ⚠️ Warn |
| 5 | **Crew Complement** — Flight cannot exceed required pilot/cabin count | `CREW_COMPLEMENT_EXCEEDED` | ❌ Reject |
| 6 | **Availability Check** — Blocks assignment during leave/training/medical | `UNAVAILABLE` | ❌ Reject |
| 7 | **Flight Duty Period (FDP)** — Max 13 hours continuous duty | `FDP_EXCEEDED` | ❌ Reject |
| 8 | **Cumulative Duty** — Max 60h in 7 days, 190h in 28 days | `CUMULATIVE_DUTY_EXCEEDED` | ❌ Reject |

When conflicts are detected, the API returns a detailed `409 CONFLICT` response:

```json
{
  "status": 409,
  "message": "Cannot assign crew member to flight — 2 conflict(s) detected",
  "conflicts": [
    {
      "crewName": "Captain Khan",
      "conflictType": "OVERLAP",
      "flightANumber": "SK102",
      "flightBNumber": "SK089",
      "message": "Flight SK102 overlaps with already assigned flight SK089"
    }
  ]
}
```

---

## 🔌 API Endpoints

### Authentication (Public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/auth/register` | Register new user (query param: `role=ADMIN\|CREW`) |
| `POST` | `/api/v1/auth/login` | Authenticate → receive JWT token |

### Crew Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/crew` | List all crew members (paginated) |
| `GET` | `/api/v1/crew/{crewId}` | Get crew member by ID |
| `GET` | `/api/v1/crew/{crewId}/schedule` | Get crew member's flight schedule |
| `POST` | `/api/v1/crew` | Create crew member (COCKPIT or CABIN) |
| `PUT` | `/api/v1/crew/{crewId}` | Update crew member |
| `DELETE` | `/api/v1/crew/{crewId}` | Delete crew member |

### Flight Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/flights` | List all flights (paginated) |
| `GET` | `/api/v1/flights/{flightId}` | Get flight by ID |
| `POST` | `/api/v1/flights` | Create new flight |
| `PUT` | `/api/v1/flights/{flightId}` | Update flight |
| `DELETE` | `/api/v1/flights/{flightId}` | Delete flight |

### Roster — Smart Scheduling
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/roster` | List all roster assignments (paginated) |
| `GET` | `/api/v1/roster/conflicts` | Scan & detect all scheduling conflicts |
| `POST` | `/api/v1/roster` | Assign crew to flight (**runs 8 safety checks**) |
| `DELETE` | `/api/v1/roster/{rosterId}` | Remove roster assignment |

### Availability
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/availability/crew/{crewId}` | Get crew availability calendar |
| `POST` | `/api/v1/availability` | Create availability block (LEAVE/TRAINING/MEDICAL/STANDBY) |
| `DELETE` | `/api/v1/availability/{id}` | Delete availability block |

### Notifications
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/notifications` | List all notifications |
| `PUT` | `/api/v1/notifications/preferences` | Update notification preferences |

---

## 🚀 Quick Start

### Run Locally (H2 in-memory database)

```bash
git clone https://github.com/Aftab0khan021/SkyCrew.git
cd SkyCrew
mvn spring-boot:run
```

### Run with Docker

```bash
docker build -t skycrew-api .
docker run -p 8080:8080 skycrew-api
```

### Run with Docker Compose (PostgreSQL)

```bash
docker-compose up -d
```

**Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### First Steps

1. **Register** — `POST /api/v1/auth/register?role=ADMIN`
   ```json
   { "username": "admin", "password": "admin123" }
   ```
2. **Copy the JWT token** from the response
3. **Authorize** — Click the 🔓 Authorize button in Swagger, enter `Bearer <token>`
4. **Create crew** → **Create flights** → **Assign crew to flights** — watch the conflict engine in action!

---

## 🏢 Enterprise Features

| Feature | Description |
|---------|-------------|
| **Multi-Tenancy** | `X-Tenant-Id` header isolates data per airline organization |
| **Audit Trail** | `createdAt`, `updatedAt`, `createdBy` auto-populated on all entities |
| **Rate Limiting** | IP-based Token Bucket algorithm (Bucket4j) — returns `429` on abuse |
| **Notification System** | Automatic email notifications on schedule changes |
| **Database Versioning** | 6 Flyway migrations — zero `ddl-auto` in production |
| **OOP Inheritance** | Single Table Inheritance: `CrewMember` → `CockpitCrew` / `CabinCrew` |
| **Global Error Handling** | Consistent JSON error responses via `@RestControllerAdvice` |
| **Docker Security** | Multi-stage build, runs as non-root `skycrew` user |
| **Health Checks** | Docker `HEALTHCHECK` + Spring Boot Actuator endpoints |
| **JVM Tuning** | Container-aware memory: `UseContainerSupport`, `MaxRAMPercentage=75%` |

---

## 📊 Project Structure

```
SkyCrew/
├── src/main/java/com/skycrew/
│   ├── config/           # Security, JWT, Rate Limiting, Multi-tenancy (6 classes)
│   ├── controller/       # REST endpoints (6 controllers)
│   ├── dto/              # Request/Response DTOs, ConflictReport (14 classes)
│   ├── exception/        # Custom exceptions + GlobalExceptionHandler (5 classes)
│   ├── model/            # JPA entities + enums (17 classes)
│   ├── repository/       # Spring Data JPA repositories (7 interfaces)
│   ├── security/         # JWT filter, service, UserDetailsService (3 classes)
│   └── service/          # Business logic — RosterService is the core (7 classes)
├── src/main/resources/
│   ├── application.yml           # Default config (H2, dev settings)
│   ├── application-prod.yml      # Production config (PostgreSQL, validate)
│   └── db/migration/             # 6 Flyway SQL migration files
├── src/test/java/com/skycrew/
│   ├── service/          # 5 unit test classes (RosterServiceTest = 23KB)
│   ├── controller/       # 2 integration test classes
│   └── security/         # Security tests
├── Dockerfile            # Multi-stage build (Maven → JRE Alpine)
├── docker-compose.yml    # Local dev with PostgreSQL
├── pom.xml               # Maven dependencies (17 deps)
└── Read.md               # This file
```

**60+ source files** | **6 SQL migrations** | **8 business rules** | **20+ API endpoints**

---

## 🗄️ Database Schema

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  APP_USERS   │     │ CREW_MEMBER  │     │    FLIGHT    │
│──────────────│     │──────────────│     │──────────────│
│ id           │     │ crew_id      │     │ flight_id    │
│ username     │     │ crew_type    │     │ flight_number│
│ password     │     │ name         │     │ origin       │
│ role         │     │ role         │     │ destination  │
│ tenant_id    │     │ base_airport │     │ departure    │
└──────────────┘     │ max_monthly  │     │ arrival      │
                     │ tenant_id    │     │ req_pilots   │
┌──────────────┐     └──────┬───────┘     │ req_cabin    │
│  NOTIF_PREF  │            │             │ report_time  │
│──────────────│     ┌──────┴───────┐     │ debrief_time │
│ user_id (FK) │     │   ROSTER     │     │ tenant_id    │
│ email_enabled│     │──────────────│     └──────┬───────┘
│ sms_enabled  │     │ roster_id    │            │
└──────────────┘     │ crew_id (FK) ├────────────┘
                     │ flight_id(FK)│
┌──────────────┐     │ status       │     ┌──────────────┐
│ AVAILABILITY │     │ tenant_id    │     │ NOTIFICATION │
│──────────────│     └──────────────┘     │──────────────│
│ crew_id (FK) │                          │ roster_id(FK)│
│ type         │                          │ type         │
│ start_date   │                          │ status       │
│ end_date     │                          │ subject      │
│ approved     │                          │ body         │
└──────────────┘                          └──────────────┘
```

Uses **Single Table Inheritance** for `CrewMember`:
- `COCKPIT` → `license_number`, `type_ratings`
- `CABIN` → `languages_spoken`, `safety_training_expiry`

---

## ☁️ AWS Deployment

| Component | Configuration |
|-----------|--------------|
| **EC2** | t2.micro · Amazon Linux 2023 · Docker |
| **RDS** | db.t3.micro · PostgreSQL 18.3 · 20GB SSD |
| **Security Groups** | EC2: ports 22, 8080 · RDS: port 5432 (EC2 only) |
| **Docker** | `--restart always` · `--network host` · non-root |

```bash
# Production deployment command
docker run -d --name skycrew --restart always --network host \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://<RDS_ENDPOINT>:5432/skycrew_db" \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD="<password>" \
  -e SKYCREW_JWT_SECRET="<base64-encoded-secret>" \
  skycrew-api:latest
```

---

## 🔐 Security

- **JWT Authentication** — HMAC-SHA256 signed tokens, 24h expiration
- **BCrypt Password Hashing** — Industry-standard, 10 rounds
- **Role-Based Access Control** — `ADMIN` (full access) / `CREW` (read-only)
- **Stateless Sessions** — No server-side session storage
- **Rate Limiting** — IP-based token bucket, configurable RPM
- **Non-Root Docker** — Container runs as unprivileged user
- **Input Validation** — Jakarta Bean Validation on all DTOs

---

## 🧪 Testing

| Type | Classes | Focus |
|------|---------|-------|
| **Unit** | 5 service tests | Conflict detection, business rules, CRUD |
| **Integration** | 2 controller tests | Full HTTP cycle with security |
| **Security** | Auth tests | JWT validation, role enforcement |

Key test scenarios in `RosterServiceTest` (23KB):
- ✅ Successful assignment
- ✅ Overlap, fatigue, hours, FDP, cumulative duty violations
- ✅ Crew complement exceeded
- ✅ Unavailability blocks
- ✅ 90% threshold warnings
- ✅ Multiple simultaneous conflicts

```bash
mvn clean test
```

---

## 🎯 Design Patterns

| Pattern | Usage |
|---------|-------|
| Repository | 7 JPA data access interfaces |
| DTO | 14 request/response objects |
| Builder | Lombok `@Builder` on responses |
| Template Method | `Auditable` base entity |
| Strategy | `CockpitCrew` / `CabinCrew` polymorphism |
| Filter Chain | Rate Limit → JWT → Security filters |
| Token Bucket | IP-based rate limiting (Bucket4j) |
| Single Table Inheritance | Crew member type hierarchy |
| Global Exception Handler | `@RestControllerAdvice` |

---

## 👤 Author

**Aftab Khan** — [GitHub](https://github.com/Aftab0khan021)

---

## 📜 License

This project is licensed under the [MIT License](LICENSE).
