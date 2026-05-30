# ✈️ SkyCrew — Aviation Roster & Conflict Resolution API

[![CI/CD](https://github.com/Aftab0khan021/SkyCrew/actions/workflows/ci.yml/badge.svg)](https://github.com/Aftab0khan021/SkyCrew/actions)
[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)
[![Tests](https://img.shields.io/badge/Tests-51%20passing-brightgreen)](https://github.com/Aftab0khan021/SkyCrew)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

> A production-grade RESTful backend service for managing aviation crew scheduling with **7 automated conflict detection rules**, JWT authentication, and enterprise features.

### 🔗 [Live Demo (Swagger UI)](https://skycrew-api.onrender.com/swagger-ui.html) | 📄 [API Docs](https://skycrew-api.onrender.com/v3/api-docs)

---

## 🏗️ Architecture & Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.5, Spring Security, Spring Data JPA |
| **Database** | PostgreSQL 16, Flyway Migrations (6 versions) |
| **Auth** | Stateless JWT with BCrypt, Role-based (ADMIN/CREW) |
| **Testing** | JUnit 5, Mockito — **51 tests (100% pass)** |
| **DevOps** | Docker, GitHub Actions CI/CD |
| **Docs** | SpringDoc OpenAPI (Swagger UI) |
| **Cloud** | Render.com / AWS EC2 + RDS ready |

---

## 🧠 Smart Rostering Engine — 7 Business Rules

The core engine enforces **7 aviation-compliant rules** before any crew assignment:

| # | Rule | Conflict Type |
|---|------|--------------|
| 1 | **Overlap Detection** — No double-booking | `OVERLAP` |
| 2 | **Fatigue Management** — Min 12h rest between flights | `FATIGUE` |
| 3 | **Monthly Hours Cap** — Cannot exceed max hours/month | `HOURS_EXCEEDED` |
| 4 | **Crew Complement** — Flight can't exceed required crew count | `CREW_COMPLEMENT_EXCEEDED` |
| 5 | **Availability Check** — Leave/training/medical blocks | `UNAVAILABLE` |
| 6 | **Flight Duty Period** — Max 13h continuous duty | `FDP_EXCEEDED` |
| 7 | **Cumulative Duty** — 60h/7 days, 190h/28 days | `CUMULATIVE_DUTY_EXCEEDED` |

---

## 🔌 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/auth/register` | Register new user |
| `POST` | `/api/v1/auth/login` | Login → JWT token |
| `CRUD` | `/api/v1/crew` | Manage crew members |
| `CRUD` | `/api/v1/flights` | Manage flights |
| `POST` | `/api/v1/roster/assign` | Smart assignment with conflict check |
| `GET` | `/api/v1/roster/conflicts` | Detect all scheduling conflicts |
| `CRUD` | `/api/v1/crew/{id}/availability` | Crew leave/training calendar |
| `GET` | `/api/v1/notifications` | Notification history |
| `PUT` | `/api/v1/notifications/preferences` | Update notification settings |

---

## 🚀 Quick Start

```bash
# Clone
git clone https://github.com/Aftab0khan021/SkyCrew.git
cd SkyCrew

# Run with Docker
docker-compose up -d

# Or run with Maven (requires PostgreSQL)
./mvnw spring-boot:run

# Run tests
./mvnw clean test
```

**Default credentials:** `admin` / `admin123`
**Swagger UI:** http://localhost:8080/swagger-ui.html

---

## 🏢 Enterprise Features

- **Multi-tenancy** — `X-Tenant-Id` header isolates data per airline
- **Audit Trail** — `createdAt`, `updatedAt`, `createdBy` on all entities
- **Notification Queue** — @Scheduled batch email processor
- **Database Versioning** — 6 Flyway migrations (no `ddl-auto` in prod)
- **OOP Design** — Single Table Inheritance: `CrewMember` → `CockpitCrew` / `CabinCrew`

---

## 📊 Project Structure

```
src/main/java/com/skycrew/
├── config/          # Security, JWT, Multi-tenancy, Rate Limiting
├── controller/      # REST endpoints (6 controllers)
├── dto/             # Request/Response DTOs, ConflictReport
├── exception/       # Custom exceptions + global handler
├── model/           # JPA entities (8 entities)
├── repository/      # Spring Data JPA repositories (7 repos)
└── service/         # Business logic (6 services)
```

---

## 👤 Author

**Aftab Khan** — [GitHub](https://github.com/Aftab0khan021)
