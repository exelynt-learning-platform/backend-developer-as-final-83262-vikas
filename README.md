# RESTful Resource Booking System with Spring Boot, JWT & RBAC

A production-grade RESTful backend application built with **Java 17+**, **Spring Boot 3**, **Spring Security 6**, **JSON Web Tokens (JWT)**, **Spring Data JPA / Hibernate**, and **MySQL**, implementing comprehensive **Role-Based Access Control (RBAC)**.

---

## Table of Contents
- [Project Overview](#project-overview)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Project Architecture](#project-architecture)
- [Environment Variables & Configuration](#environment-variables--configuration)
- [Seed Credentials](#seed-credentials)
- [How to Run the Application](#how-to-run-the-application)
- [API Documentation & Swagger UI](#api-documentation--swagger-ui)
- [Security & RBAC Enforcement](#security--rbac-enforcement)
- [Testing Instructions](#testing-instructions)

---

## Project Overview

The **Resource Booking System** allows standard users to browse available resources and book/manage personal reservations, while administrators have full privileges over resources and all user reservations.

Tenant isolation and server-side identity:
- **Server-Side Identity**: The authenticated user identity for every reservation is strictly extracted from the validated JWT token in the Spring SecurityContext. Client-supplied user identifiers in payloads are never trusted.
- **Access Isolation**: Standard users can only view, modify, or cancel their own reservations. Attempts to access another user's reservation return `403 Forbidden`.
- **Administrative Control**: Admins have unrestricted CRUD privileges over both resources and reservations.

---

## Key Features

- **Stateless JWT Authentication**: Secure, stateless authentication using HS512 HMAC-SHA512 signed tokens.
- **BCrypt Password Hashing**: Strong one-way password hashing for all user accounts.
- **Role-Based Access Control (RBAC)**: Distinguishes between `ADMIN` and `USER` roles across all resource and reservation endpoints.
- **Strict Reservation Ownership**: Prevents cross-tenant access and IDOR (Insecure Direct Object Reference) vulnerabilities.
- **Direct SQL Join Filtering**: Scalable criteria queries without in-memory user ID collections.
- **Dynamic Multi-Field Filtering**: Filter reservations simultaneously by `status`, `minPrice`, and `maxPrice`.
- **Spring Data Pagination & Safe Sorting**: Whitelisted sort properties (`id`, `startTime`, `endTime`, `price`, `status`, `createdAt`, `updatedAt`) protecting against injection.
- **Comprehensive Bean Validation**: Validates prices (`>= 0`), start/end times (`endTime` after `startTime`), required attributes, and string constraints.
- **Centralized Error Handling**: Standardized JSON responses for `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `409 Conflict`, and `500 Internal Server Error`.
- **Interactive Swagger / OpenAPI UI**: Built-in Swagger documentation with Bearer JWT token authorization.

---

## Technology Stack

- **Java**: 17+ (tested on Java 25 LTS)
- **Framework**: Spring Boot 3.5.11
- **Security**: Spring Security 6, JJWT 0.12.6 (HMAC-SHA512)
- **Database**: MySQL 8.0+ (production/development), H2 (in-memory test profile)
- **ORM / Persistence**: Spring Data JPA, Hibernate 6.6
- **API Documentation**: SpringDoc OpenAPI 2.8.5 / Swagger UI
- **Build Tool**: Maven 3.9+ (Maven Wrapper included)
- **Testing**: JUnit 5, Mockito, Spring Security Test, MockMvc

---

## Prerequisites

- **Java Development Kit (JDK)**: Java 17 or higher (`java -version`)
- **Database (Optional for test runs)**: MySQL 8.0+ (running locally or in Docker)
- **Terminal / Shell**: PowerShell or Bash

---

## Project Architecture

The application adopts a clean layered architecture with constructor dependency injection and separated authorization services:

```
src/main/java/com/example/authsystem/
├── config/                  # Swagger & Idempotent Data initialization
│   ├── DataInitializer.java
│   └── OpenApiConfig.java
├── controller/              # REST Controllers with RBAC annotations
│   ├── AuthController.java
│   ├── ReservationController.java
│   └── ResourceController.java
├── dto/                     # Strongly-typed Data Transfer Objects
│   ├── auth/                # LoginRequest, LoginResponse, RegisterRequest
│   ├── common/              # PagedResponse, ErrorResponse
│   ├── reservation/         # ReservationRequest, ReservationUpdateRequest, ReservationResponse
│   └── resource/            # ResourceRequest, ResourceResponse
├── entity/                  # JPA Entities & Enums
│   ├── Reservation.java
│   ├── ReservationStatus.java
│   ├── Resource.java
│   ├── Role.java
│   └── User.java
├── exception/               # Custom Exceptions & Global Exception Handler (@RestControllerAdvice)
│   ├── AccessDeniedCustomException.java
│   ├── BadRequestException.java
│   ├── GlobalExceptionHandler.java
│   ├── ResourceConflictException.java
│   └── ResourceNotFoundException.java
├── repository/              # Spring Data JPA Repositories & Specifications
│   ├── ReservationRepository.java
│   ├── ReservationSpecification.java
│   ├── ResourceRepository.java
│   └── UserRepository.java
├── security/                # Security configuration, HS512 JWT utility, & filters
│   ├── CustomAccessDeniedHandler.java
│   ├── CustomUserDetails.java
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationEntryPoint.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtUtils.java
│   └── SecurityConfig.java
└── service/                 # Business logic and transactions
    ├── AuthService.java
    ├── ReservationAuthorizationService.java
    ├── ReservationService.java
    └── ResourceService.java
```

---

## Environment Variables & Configuration

The application is configured using environment variables with secure defaults:

| Environment Variable | Description | Default / Production Guidance |
|----------------------|-------------|-------------------------------|
| `DB_URL` | JDBC URL for MySQL | `jdbc:mysql://localhost:3306/auth_rbac_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | *(empty)* |
| `JWT_SECRET` | HMAC-SHA512 Secret Key (min 64 bytes) | **Required in production via environment variable**. For local development, if omitted, a cryptographically secure 512-bit key is automatically generated at startup. |
| `JWT_EXPIRATION_MS` | JWT token validity in milliseconds | `86400000` (24 hours) |
| `APP_SEED_ENABLED` | Enable test seed accounts | `true` in `local`/`dev`/`test` profiles; disabled in `prod` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `local` (default). In production, set to `prod` to disable H2 console and test seeding. |

### Creating Database in MySQL (if needed)

```sql
CREATE DATABASE IF NOT EXISTS auth_rbac_db;
```

---

## Seed Credentials

> [!NOTE]
> The following pre-configured credentials are idempotently seeded on startup by `DataInitializer` using BCrypt password hashing. They are for **testing and development purposes only**.

| Role | Username | Email | Password | Permissions |
|------|----------|-------|----------|-------------|
| **ADMIN** | `admin` | `admin@example.com` | `Admin@123` | Full CRUD on resources & reservations; view all reservations |
| **USER** | `user` | `user@example.com` | `User@123` | View resources; create & manage own reservations only |

---

## How to Run the Application

### 1. Running with Maven Wrapper

Navigate to the project directory:
```bash
cd auth-rbac-backend
```

Run with default environment variables (local MySQL):
```powershell
.\mvnw.cmd spring-boot:run
```

Or run with custom environment variables:
```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/auth_rbac_db"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
$env:JWT_SECRET="your-secure-512-bit-secret-key-at-least-64-characters-long-for-hs512"
.\mvnw.cmd spring-boot:run
```

The application starts on port `8080` by default.

---

## API Documentation & Swagger UI

- **Interactive Swagger UI**: Explore and execute requests interactively in your browser at:
  ```
  http://localhost:8080/swagger-ui/index.html
  ```
- **OpenAPI 3 JSON Specification**:
  ```
  http://localhost:8080/v3/api-docs
  ```
- **Full API Documentation**: For full endpoint details, schemas, cURL examples, and error responses, see [API.md](API.md).

---

## Security & RBAC Enforcement

- **JWT Signing**: HS512 (HMAC-SHA512) algorithm strictly enforced during signing and validation.
- **IDOR Protection**: All reservation operations (GET by ID, UPDATE, DELETE) verify the authenticated user ID from `CustomUserDetails` against the reservation owner ID. Non-owners receive `403 Forbidden`.
- **Scalable Queries**: Filtering uses direct SQL joins (`ReservationSpecification`) rather than fetching lists of user IDs into application memory.
- **Idempotent Data Seeding**: `DataInitializer` uses transactional atomic checks to prevent `DataIntegrityViolationException` on subsequent startups.

---

## Testing Instructions

The repository includes automated integration tests covering all requirements:
- Login with username / email
- Invalid credentials & validation checks
- ADMIN vs USER authorization rules on resources
- USER reservation creation taking identity from JWT
- USER ownership isolation (forbidden to view/update/delete another user's reservation)
- ADMIN access across all reservations
- Filtering by status and price range
- Pagination metadata validation
- Safe sorting and unsafe sort property rejection

Run tests:
```powershell
.\mvnw.cmd test
```

All 28 tests run using the isolated in-memory H2 database with zero external dependencies required.
