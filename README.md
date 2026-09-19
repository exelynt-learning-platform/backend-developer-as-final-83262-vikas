# RESTful Resource Booking System with Spring Boot, JWT & RBAC

A production-ready RESTful backend application built with **Java 17+**, **Spring Boot 3**, **Spring Security 6**, **JSON Web Tokens (JWT)**, **Spring Data JPA / Hibernate**, and **MySQL**, implementing comprehensive **Role-Based Access Control (RBAC)**.

---

## Table of Contents
- [Project Overview](#project-overview)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Project Architecture](#project-architecture)
- [Database Setup & Environment Variables](#database-setup--environment-variables)
- [Seed Credentials](#seed-credentials)
- [How to Run the Application](#how-to-run-the-application)
- [Authentication & JWT Workflow](#authentication--jwt-workflow)
- [API Endpoints Reference](#api-endpoints-reference)
- [Example Requests (cURL)](#example-requests-curl)
- [Filtering, Pagination, and Sorting](#filtering-pagination-and-sorting)
- [Validation & Error Handling](#validation--error-handling)
- [Swagger / OpenAPI Documentation](#swagger--openapi-documentation)
- [Testing Instructions](#testing-instructions)

---

## Project Overview

The **Resource Booking System** allows standard users to browse resources and create/manage their personal reservations, while administrators have full privileges over resources and all reservations. 

Security and tenant isolation are enforced at the core:
- **Server-Side Identity**: The authenticated user identity for every reservation is strictly extracted from the validated JWT token in the Spring SecurityContext. Client-supplied user identifiers are never trusted.
- **Access Isolation**: Standard users can only view, modify, or cancel their own reservations. Attempts to access another user's reservation return `403 Forbidden`.
- **Administrative Control**: Admins have unrestricted CRUD privileges over both resources and reservations.

---

## Key Features

- **Stateless JWT Authentication**: Secure, stateless authentication using HS256 HMAC-SHA256 signed tokens.
- **BCrypt Password Hashing**: Strong one-way password hashing for all user accounts.
- **Role-Based Access Control (RBAC)**: Distinguishes between `ADMIN` and `USER` roles across all resource and reservation endpoints.
- **Strict Reservation Ownership**: Prevents cross-tenant access and IDOR (Insecure Direct Object Reference) vulnerabilities.
- **Dynamic Multi-Field Filtering**: Filter reservations simultaneously by `status`, `minPrice`, and `maxPrice`.
- **Spring Data Pagination & Safe Sorting**: Whitelisted sort properties (`id`, `startTime`, `endTime`, `price`, `status`, `createdAt`, `updatedAt`) protecting against JPA injection and bad field errors.
- **Comprehensive Bean Validation**: Validates prices (`>= 0`), start/end times (`endTime` after `startTime`), required attributes, and string constraints.
- **Centralized Error Handling**: Standardized JSON responses for `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `409 Conflict`, and `500 Internal Server Error`.
- **Interactive Swagger / OpenAPI UI**: Built-in Swagger documentation with Bearer JWT token authorization.

---

## Technology Stack

- **Java**: 17+ (tested on Java 25 LTS)
- **Framework**: Spring Boot 3.5.11
- **Security**: Spring Security 6, JJWT 0.12.6
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

The application adopts a clean layered architecture with constructor dependency injection:

```
src/main/java/com/example/authsystem/
├── config/                  # Swagger & Data initialization configuration
│   ├── DataInitializer.java
│   └── OpenApiConfig.java
├── controller/              # REST Controllers handling HTTP requests & OpenAPI annotations
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
├── security/                # Security configuration, JWT utility, & filters
│   ├── CustomAccessDeniedHandler.java
│   ├── CustomUserDetails.java
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationEntryPoint.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtUtils.java
│   └── SecurityConfig.java
└── service/                 # Business logic, authorization rules, and transactions
    ├── AuthService.java
    ├── ReservationService.java
    └── ResourceService.java
```

---

## Database Setup & Environment Variables

The application is configured to connect to MySQL using environment variables, with sensible local defaults:

| Environment Variable | Description | Default Value |
|----------------------|-------------|---------------|
| `DB_URL` | JDBC URL for MySQL | `jdbc:mysql://localhost:3306/auth_rbac_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | *(empty)* |
| `JWT_SECRET` | HMAC-SHA256 Secret Key (min 32 bytes) | `9a4f2c8d3e5a1b7c9f8e2d4c6a8b0d2e4f6a8c0d2e4f6a8b0c2d4e6f8a0b2c4d` |
| `JWT_EXPIRATION_MS` | JWT token validity in milliseconds | `86400000` (24 hours) |

### Creating Database in MySQL (if needed)

```sql
CREATE DATABASE IF NOT EXISTS auth_rbac_db;
```

---

## Seed Credentials

> [!NOTE]
> The following pre-configured credentials are seeded on startup by `DataInitializer` using BCrypt password hashing. They are for **testing and development purposes only**.

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
.\mvnw.cmd spring-boot:run
```

The application starts on port `8080` by default.

---

## Authentication & JWT Workflow

1. Send `POST /auth/login` with your username or email and password.
2. The server authenticates credentials against BCrypt hashes and returns a JWT Bearer token:
   ```json
   {
     "token": "eyJhbGciOiJIUzUxMiJ...",
     "tokenType": "Bearer",
     "id": 1,
     "username": "admin",
     "email": "admin@example.com",
     "role": "ADMIN"
   }
   ```
3. Include the token in subsequent HTTP requests inside the `Authorization` header:
   ```
   Authorization: Bearer <your-jwt-token>
   ```

---

## API Endpoints Reference

### Authentication
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| `POST` | `/auth/login` | Public | Authenticate with username/email & password, return JWT |
| `POST` | `/auth/register` | Public | Register new user account |

### Resources
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| `GET` | `/api/resources` | `ADMIN`, `USER` | List resources (paginated, sortable) |
| `GET` | `/api/resources/{id}` | `ADMIN`, `USER` | Get resource details by ID |
| `POST` | `/api/resources` | `ADMIN` only | Create a new resource |
| `PUT` | `/api/resources/{id}` | `ADMIN` only | Update resource details |
| `DELETE` | `/api/resources/{id}` | `ADMIN` only | Delete a resource |

### Reservations
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| `POST` | `/api/reservations` | `ADMIN`, `USER` | Create reservation (user identity strictly from JWT) |
| `GET` | `/api/reservations` | `ADMIN`, `USER` | List reservations with filtering, pagination & sorting (`ADMIN` sees all; `USER` sees only their own) |
| `GET` | `/api/reservations/{id}` | `ADMIN`, `USER` | Get reservation by ID (`ADMIN` any; `USER` own only) |
| `PUT` | `/api/reservations/{id}` | `ADMIN`, `USER` | Update reservation (`ADMIN` any; `USER` own only) |
| `DELETE` | `/api/reservations/{id}` | `ADMIN`, `USER` | Delete reservation (`ADMIN` any; `USER` own only) |

---

## Example Requests (cURL)

### 1. Login as Admin
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "Admin@123"}'
```

### 2. Login as User
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "User@123"}'
```

### 3. Create a Resource (ADMIN only)
```bash
curl -X POST http://localhost:8080/api/resources \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Meeting Pod Beta",
    "description": "Soundproof meeting pod for 4 people",
    "type": "MEETING_ROOM",
    "available": true
  }'
```

### 4. Create a Reservation (USER or ADMIN)
```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceId": 1,
    "startTime": "2026-10-01T10:00:00",
    "endTime": "2026-10-01T12:00:00",
    "price": 150.00,
    "status": "PENDING"
  }'
```

### 5. Filter Reservations by Status and Price Range
```bash
curl -X GET "http://localhost:8080/api/reservations?status=CONFIRMED&minPrice=100&maxPrice=1000&page=0&size=10&sort=price,desc" \
  -H "Authorization: Bearer <USER_TOKEN>"
```

### 6. Update a Reservation
```bash
curl -X PUT http://localhost:8080/api/reservations/1 \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "startTime": "2026-10-01T11:00:00",
    "endTime": "2026-10-01T13:00:00",
    "price": 175.00,
    "status": "CONFIRMED"
  }'
```

### 7. Delete a Reservation
```bash
curl -X DELETE http://localhost:8080/api/reservations/1 \
  -H "Authorization: Bearer <USER_TOKEN>"
```

---

## Filtering, Pagination, and Sorting

### Filtering
Combine any subset of parameters:
- `status`: `PENDING`, `CONFIRMED`, `CANCELLED`
- `minPrice`: numeric lower bound (e.g. `100`)
- `maxPrice`: numeric upper bound (e.g. `500`)

### Pagination
- `page`: 0-indexed page index (default: `0`)
- `size`: number of records per page (default: `10`)

### Sorting
- Whitelisted sort fields: `id`, `startTime`, `endTime`, `price`, `status`, `createdAt`, `updatedAt`
- Direction: `asc` or `desc`
- Examples: `sort=price,desc` or `sort=startTime,asc`

---

## Validation & Error Handling

Standardized JSON responses format:
```json
{
  "timestamp": "2026-09-19T18:10:00.000",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields",
  "path": "/api/reservations",
  "validationErrors": {
    "price": "Price must be greater than or equal to 0",
    "endTime": "End time must be strictly after start time"
  }
}
```

HTTP Status Codes handled:
- `400 Bad Request`: Validation failures, invalid parameters, endTime before startTime, negative price.
- `401 Unauthorized`: Missing, expired, or invalid JWT; invalid login credentials.
- `403 Forbidden`: Standard user accessing another user's reservation or attempting resource creation/modification.
- `404 Not Found`: Non-existent resource, reservation, or user.
- `409 Conflict`: Duplicate username, duplicate email, or resource name conflict.
- `500 Internal Server Error`: Generic internal server failure without stack traces.

---

## Swagger / OpenAPI Documentation

Once the application is running, access Swagger UI in your browser:
```
http://localhost:8080/swagger-ui/index.html
```

Or view the raw OpenAPI 3 JSON specification:
```
http://localhost:8080/v3/api-docs
```

### Testing Protected Endpoints via Swagger UI:
1. Call `/auth/login` to obtain a JWT token.
2. Click the **Authorize** (lock icon) button at the top of Swagger UI.
3. Enter `Bearer <your-token>` and click **Authorize**.
4. Test any protected endpoint directly from the browser!

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
