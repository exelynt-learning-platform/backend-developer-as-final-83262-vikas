# RESTful Resource Booking System - API Documentation

Comprehensive REST API reference for the Resource Booking System.

All protected endpoints require a valid JWT Bearer token in the `Authorization` header:
```http
Authorization: Bearer <your-jwt-token>
```

---

## 1. Authentication Endpoints

Base Path: `/auth`

### 1.1 Login
Authenticate with username or email and receive a signed JWT token.

- **URL**: `/auth/login`
- **Method**: `POST`
- **Access**: Public
- **Request Body**:
  ```json
  {
    "username": "admin",
    "password": "Admin@123"
  }
  ```
  *(Or with email:)*
  ```json
  {
    "email": "admin@example.com",
    "password": "Admin@123"
  }
  ```
- **Success Response (200 OK)**:
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
- **Error Responses**:
  - `400 Bad Request`: Validation error (missing username/email or password).
  - `401 Unauthorized`: Invalid credentials.

### 1.2 User Registration
Register a new user account with role assignment.

- **URL**: `/auth/register`
- **Method**: `POST`
- **Access**: Public
- **Request Body**:
  ```json
  {
    "username": "newuser",
    "email": "newuser@example.com",
    "password": "Password@123",
    "role": "USER"
  }
  ```
- **Success Response (201 Created)**:
  ```json
  {
    "id": 3,
    "username": "newuser",
    "email": "newuser@example.com",
    "role": "USER"
  }
  ```
- **Error Responses**:
  - `400 Bad Request`: Validation failed (e.g. invalid email format, password under 6 chars).
  - `409 Conflict`: Username or email is already registered.

---

## 2. Resource Management Endpoints

Base Path: `/api/resources`

### 2.1 List All Resources
List resources with pagination and sorting support.

- **URL**: `/api/resources`
- **Method**: `GET`
- **Access**: `ADMIN` or `USER`
- **Query Parameters**:
  - `page`: Page index (default: `0`)
  - `size`: Page size (default: `10`)
  - `sort`: Sorting criteria (e.g., `name,asc`, default: `id,asc`)
- **Success Response (200 OK)**:
  ```json
  {
    "content": [
      {
        "id": 1,
        "name": "Conference Room A",
        "description": "Executive conference room with 4K display and conference phone",
        "type": "CONFERENCE_ROOM",
        "available": true,
        "createdAt": "2026-09-19T18:00:00",
        "updatedAt": "2026-09-19T18:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 3,
    "totalPages": 1,
    "last": true,
    "first": true
  }
  ```

### 2.2 Get Resource by ID
- **URL**: `/api/resources/{id}`
- **Method**: `GET`
- **Access**: `ADMIN` or `USER`
- **Success Response (200 OK)**:
  ```json
  {
    "id": 1,
    "name": "Conference Room A",
    "description": "Executive conference room with 4K display and conference phone",
    "type": "CONFERENCE_ROOM",
    "available": true,
    "createdAt": "2026-09-19T18:00:00",
    "updatedAt": "2026-09-19T18:00:00"
  }
  ```
- **Error Response**: `404 Not Found` if resource ID does not exist.

### 2.3 Create Resource
- **URL**: `/api/resources`
- **Method**: `POST`
- **Access**: `ADMIN` only
- **Request Body**:
  ```json
  {
    "name": "Innovation Lab",
    "description": "High-tech collaboration space with VR rigs",
    "type": "LAB",
    "available": true
  }
  ```
- **Success Response (201 Created)**: Returns the created resource object.
- **Error Responses**:
  - `400 Bad Request`: Validation failure.
  - `403 Forbidden`: Authenticated as `USER` instead of `ADMIN`.
  - `409 Conflict`: Resource name already exists.

### 2.4 Update Resource
- **URL**: `/api/resources/{id}`
- **Method**: `PUT`
- **Access**: `ADMIN` only
- **Request Body**: Same as Create Resource.
- **Success Response (200 OK)**: Returns the updated resource object.
- **Error Responses**: `403 Forbidden` (non-admin), `404 Not Found`, `409 Conflict`.

### 2.5 Delete Resource
- **URL**: `/api/resources/{id}`
- **Method**: `DELETE`
- **Access**: `ADMIN` only
- **Success Response (204 No Content)**
- **Error Responses**: `403 Forbidden` (non-admin), `404 Not Found`.

---

## 3. Reservation Management Endpoints

Base Path: `/api/reservations`

### 3.1 Create Reservation
User identity is strictly derived from the authenticated JWT token.

- **URL**: `/api/reservations`
- **Method**: `POST`
- **Access**: `ADMIN` or `USER`
- **Request Body**:
  ```json
  {
    "resourceId": 1,
    "startTime": "2026-10-01T09:00:00",
    "endTime": "2026-10-01T11:00:00",
    "price": 120.00,
    "status": "PENDING"
  }
  ```
- **Success Response (201 Created)**:
  ```json
  {
    "id": 1,
    "resourceId": 1,
    "resourceName": "Conference Room A",
    "resourceType": "CONFERENCE_ROOM",
    "userId": 2,
    "username": "user",
    "userEmail": "user@example.com",
    "startTime": "2026-10-01T09:00:00",
    "endTime": "2026-10-01T11:00:00",
    "price": 120.00,
    "status": "PENDING",
    "createdAt": "2026-09-19T18:05:00",
    "updatedAt": "2026-09-19T18:05:00"
  }
  ```
- **Validation Rules**:
  - `startTime` and `endTime` are required.
  - `endTime` must be strictly after `startTime`.
  - `price` must be non-negative (`>= 0`).
  - Referenced resource must exist and be marked `available: true`.

### 3.2 List Reservations
Dynamic multi-field filtering, pagination, and sorting.
- **RBAC Policy**: `ADMIN` users see all reservations; `USER` role only retrieves their own reservations via direct SQL JOIN.

- **URL**: `/api/reservations`
- **Method**: `GET`
- **Access**: `ADMIN` or `USER`
- **Query Parameters**:
  - `status`: `PENDING`, `CONFIRMED`, `CANCELLED` (optional)
  - `minPrice`: Minimum reservation price (optional)
  - `maxPrice`: Maximum reservation price (optional)
  - `page`: Page index (default: `0`)
  - `size`: Page size (default: `10`)
  - `sort`: Safe sort fields: `id`, `startTime`, `endTime`, `price`, `status`, `createdAt`, `updatedAt`
- **Success Response (200 OK)**:
  ```json
  {
    "content": [
      {
        "id": 1,
        "resourceId": 1,
        "resourceName": "Conference Room A",
        "resourceType": "CONFERENCE_ROOM",
        "userId": 2,
        "username": "user",
        "userEmail": "user@example.com",
        "startTime": "2026-10-01T09:00:00",
        "endTime": "2026-10-01T11:00:00",
        "price": 120.00,
        "status": "PENDING",
        "createdAt": "2026-09-19T18:05:00",
        "updatedAt": "2026-09-19T18:05:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "last": true,
    "first": true
  }
  ```

### 3.3 Get Reservation by ID
- **URL**: `/api/reservations/{id}`
- **Method**: `GET`
- **Access**: `ADMIN` (any reservation) or `USER` (own reservation only)
- **Success Response (200 OK)**: Returns the reservation details.
- **Error Responses**:
  - `403 Forbidden`: If a standard user attempts to access another user's reservation.
  - `404 Not Found`: Reservation ID does not exist.

### 3.4 Update Reservation
- **URL**: `/api/reservations/{id}`
- **Method**: `PUT`
- **Access**: `ADMIN` (any reservation) or `USER` (own reservation only)
- **Request Body**:
  ```json
  {
    "startTime": "2026-10-01T10:00:00",
    "endTime": "2026-10-01T12:30:00",
    "price": 150.00,
    "status": "CONFIRMED"
  }
  ```
- **Success Response (200 OK)**: Returns the updated reservation.
- **Error Responses**: `400 Bad Request`, `403 Forbidden`, `404 Not Found`.

### 3.5 Delete Reservation
- **URL**: `/api/reservations/{id}`
- **Method**: `DELETE`
- **Access**: `ADMIN` (any reservation) or `USER` (own reservation only)
- **Success Response (204 No Content)**
- **Error Responses**: `403 Forbidden`, `404 Not Found`.

---

## 4. Example cURL Commands

### Login as Admin
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "Admin@123"}'
```

### Create Resource (Admin)
```bash
curl -X POST http://localhost:8080/api/resources \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name": "Meeting Pod Beta", "description": "Soundproof pod", "type": "ROOM", "available": true}'
```

### Book Reservation (User)
```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"resourceId": 1, "startTime": "2026-10-01T10:00:00", "endTime": "2026-10-01T12:00:00", "price": 100.00, "status": "PENDING"}'
```

### Filter & Paginate Reservations
```bash
curl -X GET "http://localhost:8080/api/reservations?status=PENDING&minPrice=50&maxPrice=200&page=0&size=10&sort=price,desc" \
  -H "Authorization: Bearer <USER_TOKEN>"
```
