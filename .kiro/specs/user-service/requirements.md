# Requirements Document

## Introduction

The User Service is the authoritative identity store in the Fitness Tracker microservices ecosystem. It manages the complete lifecycle of user accounts — registration, profile retrieval, and soft-deletion — and provides the internal credential-lookup interface that the Auth Service depends on to authenticate users and issue JWTs.

This document captures the functional and non-functional requirements derived from the approved design, covering the three API surfaces (user-facing, internal, admin), the security model, error handling, observability, and quality constraints.

## Glossary

- **User_Service**: The Spring Boot 3.4 / Java 21 microservice described in this document
- **Auth_Service**: The internal service responsible for issuing and validating JWTs; the sole consumer of the internal API surface
- **API_Gateway**: The Spring Cloud Gateway that acts as the single entry point for all external traffic; validates JWTs and injects the `X-User-Id` header
- **UserController**: The REST controller exposing the user-facing `/api/v1/users` endpoints
- **InternalController**: The REST controller exposing the internal `/internal/v1/identity` endpoints (cluster-only)
- **AdminController**: Removed — admin operations (list users, delete user) are now exposed via `InternalController` and delegated through the Auth Service
- **UserService**: The business-logic layer that orchestrates all user operations
- **UserRepository**: The Spring Data JPA repository that persists and queries `UserEntity` records
- **UserMapper**: The component that converts `UserEntity` to safe response DTOs, suppressing sensitive fields
- **UserEntity**: The JPA entity mapped to the `accounts.accounts` PostgreSQL table
- **UserResponse**: The public-safe DTO containing only `id` and `username`
- **UserPrincipalResponse**: The internal DTO returned to the Auth Service after successful authentication, containing `id`, `username`, and `role` (singular string)
- **UserCreateRequest**: The request DTO used by the Auth Service to create a new user account
- **AuthRequest**: The request DTO used by the Auth Service to verify credentials
- **BCrypt_Encoder**: `BCryptPasswordEncoder` configured at strength 10; the only component that may hash or verify passwords
- **Soft_Delete**: Setting `deleted = 'T'` on a `UserEntity` row; Hibernate automatically excludes such rows from all queries
- **X-User-Id**: The HTTP header injected by the API Gateway carrying the authenticated user's UUID
- **Role**: The `UserEntity.Role` enum with values `USER` and `ADMIN`
- **JaCoCo**: The Maven plugin enforcing the 70% code-coverage threshold
- **Actuator**: Spring Boot Actuator, which exposes health and metrics endpoints
- **OpenAPI**: The OpenAPI 3.x specification surfaced via `springdoc-openapi-starter-webmvc-ui`

---

## Requirements

### Requirement 1: User Registration via Internal Endpoint

**User Story:** As the Auth Service, I want to create a new user account through an internal endpoint, so that user credentials are securely persisted when a client registers.

#### Acceptance Criteria

1. WHEN the Auth Service sends a `POST /internal/v1/identity/users` request with a valid `UserCreateRequest` body, THE User_Service SHALL create a new `UserEntity` and return `201 Created` with a `UserResponse` containing the new user's `id` and `username`; the `UserResponse` SHALL NOT contain a password hash or any credential data.
2. WHEN a `UserCreateRequest` is received, THE BCrypt_Encoder SHALL hash the plaintext password before the `UserEntity` is persisted; the plaintext password SHALL NOT be stored in the database, logged, or included in any response.
3. WHEN a `UserCreateRequest` contains a `roles` field that is `null`, blank, or any value other than `"USER"` or `"ADMIN"`, THE UserService SHALL assign `Role.USER` to the resulting `UserEntity`; the persisted `roles` field SHALL NOT be `null`.
4. WHEN a `UserCreateRequest` contains a `roles` field with value `"USER"` or `"ADMIN"`, THE UserService SHALL assign the corresponding `Role` enum value to the `UserEntity`.
5. WHEN a `UserCreateRequest` contains a `username` that already exists among active or soft-deleted users in the database, THE User_Service SHALL return `409 Conflict` without creating a new record.
6. WHEN a `UserCreateRequest` contains a `username` or `password` that is `null` or composed entirely of whitespace characters, THE User_Service SHALL return `400 Bad Request` and SHALL NOT persist any record.
7. WHEN a `UserCreateRequest` contains a `username` exceeding 50 characters or a `password` exceeding 72 characters, THE User_Service SHALL return `400 Bad Request` and SHALL NOT persist any record. The 72-character limit matches BCrypt's maximum input length to prevent silent hash truncation.

---

### Requirement 2: Credential Lookup via Internal Authentication Endpoint

**User Story:** As the Auth Service, I want to verify a user's credentials through an internal endpoint, so that I can determine whether to issue a JWT.

#### Acceptance Criteria

1. WHEN the Auth Service sends a `POST /internal/v1/identity/authenticate` request with a valid `AuthRequest` containing an existing active username and a matching plaintext password, THE User_Service SHALL return `200 OK` with a `UserPrincipalResponse` containing the user's `id`, `username`, and `roles`; the response SHALL NOT contain a password hash or any credential data.
2. WHEN a `POST /internal/v1/identity/authenticate` request is received with an `AuthRequest` where `username` or `password` is `null` or blank, THE User_Service SHALL return `400 Bad Request`.
3. WHEN the provided plaintext password does not match the stored BCrypt hash for the given username, THE User_Service SHALL return `401 Unauthorized` with a generic error body that does not indicate the reason for failure.
4. WHEN the provided username does not exist among active users in the database, or belongs to a soft-deleted account, THE User_Service SHALL perform an equivalent BCrypt comparison operation to prevent timing-based enumeration and SHALL return `401 Unauthorized` with a response body, status code, and headers identical to the wrong-password case.
5. THE BCrypt_Encoder SHALL perform credential verification via `matches(plaintext, storedHash)`; the plaintext password SHALL NOT be persisted, logged, or included in any response or log entry regardless of outcome.

---

### Requirement 3: User Profile Retrieval via Public Endpoint

**User Story:** As an authenticated user, I want to retrieve my own profile, so that I can view my account information.

#### Acceptance Criteria

1. WHEN an authenticated user sends `GET /api/v1/users/me` with an `X-User-Id` header containing a valid UUID (8-4-4-4-12 hexadecimal format), THE UserController SHALL return `200 OK` with a `UserResponse` containing the user's `id` and `username`.
2. IF the UUID in the `X-User-Id` header does not correspond to any active (non-soft-deleted) user, THE UserController SHALL return `404 Not Found` with an empty response body.
3. IF the `X-User-Id` header is absent or contains a value that is not a valid UUID, THE UserController SHALL return `400 Bad Request`.
4. THE UserMapper SHALL produce a `UserResponse` containing only the `id` and `username` fields; no `passwordHash`, credential data, `roles`, or internal metadata SHALL be present in the response body.

---

### Requirement 4: Admin User Listing

**User Story:** As the Auth Service acting on behalf of an admin, I want to list all registered user accounts via an internal endpoint, so that admin clients can review who has access to the system.

#### Acceptance Criteria

1. WHEN the Auth Service sends `GET /internal/v1/identity/users`, THE InternalController SHALL return `200 OK` with a JSON array of `AdminUserResponse` objects representing all active (non-soft-deleted) users; if no active users exist, the response SHALL be an empty array, not a `404`.
2. THE `AdminUserResponse` objects SHALL contain `id`, `username`, `role`, and `createdAt`; credential fields (password hash and any secret material) SHALL be omitted from every entry in the list.
3. THE InternalController SHALL exclude soft-deleted users from the response regardless of when the soft-delete was performed, so that deactivated accounts are invisible to admin listings.

---

### Requirement 5: Admin Soft-Delete User Account

**User Story:** As the Auth Service acting on behalf of an admin, I want to soft-delete a user account by UUID via an internal endpoint, so that the account is deactivated without losing historical data and any active tokens are invalidated atomically.

#### Acceptance Criteria

1. WHEN the Auth Service sends `DELETE /internal/v1/identity/users/{id}` with a valid UUID for an existing active user, THE InternalController SHALL return `204 No Content`.
2. AFTER a soft-delete is performed, THE User_Service SHALL retain the user's database row with all original fields intact; the row SHALL be queryable by a database administrator but invisible to all application-level retrieval operations.
3. WHEN a soft-delete has been performed on a user, THE User_Service SHALL exclude that user from all subsequent retrieval operations, including profile lookup, admin listing, and credential authentication.
4. IF the `{id}` path variable does not match any active user — whether because the UUID is absent from the database or because the account was already soft-deleted — THE InternalController SHALL return `404 Not Found`.
5. IF the `{id}` path variable is not a valid UUID format, THE InternalController SHALL return `400 Bad Request`.

---

### Requirement 6: Password Security

**User Story:** As a system operator, I want all passwords to be stored as BCrypt hashes and never exposed through the API, so that user credentials are protected even if the database is compromised.

#### Acceptance Criteria

1. THE BCrypt_Encoder SHALL be configured with a work factor of 10 and registered as a Spring-managed bean so that it can be injected into any component that requires password hashing or verification.
2. WHEN a user account is created, THE User_Service SHALL encode the plaintext password using BCrypt before persisting the `UserEntity`; no plaintext password SHALL reach the database.
3. WHEN credentials are verified successfully, THE User_Service SHALL NOT include the password hash or any credential material in the response body or any response header.
4. IF credential verification fails for any reason, THE User_Service SHALL NOT include the password hash, the stored hash, or any credential material in the error response or any log entry.
5. THE `UserResponse` and `UserPrincipalResponse` DTOs SHALL NOT contain a password hash or any credential field; this invariant SHALL hold for all response paths including success, error, and partial-result responses.

---

### Requirement 7: Role-Based User Model

**User Story:** As a system operator, I want every user account to carry a role of either `USER` or `ADMIN`, defaulting to `USER`, so that the API Gateway can enforce role-based access control.

#### Acceptance Criteria

1. THE `UserEntity` SHALL store a `role` field typed as `UserEntity.Role` with exactly two valid values: `USER` and `ADMIN`; the field SHALL be non-null on every persisted record.
2. WHEN a `UserCreateRequest` is processed and the `role` field is absent, `null`, blank, or not one of the recognized values `"USER"` or `"ADMIN"`, THE UserService SHALL persist the `UserEntity` with `role` set to `Role.USER`.
3. WHEN `POST /internal/v1/identity/authenticate` returns `200 OK`, THE `UserPrincipalResponse` SHALL include a `role` field equal to the stored `UserEntity.role` enum name for that account, so the Auth Service can embed the correct role claim in the JWT.

---

### Requirement 8: Soft-Delete Semantics

**User Story:** As a system operator, I want soft-deleted users to be invisible to all application-level queries, so that deactivated accounts cannot be accessed or authenticated.

#### Acceptance Criteria

1. THE `UserEntity` SHALL use Hibernate's `@SoftDelete` annotation configured with `columnName = "deleted"` and `converter = TrueFalseConverter.class`; a value of `'T'` in `deleted` SHALL mark the record as deleted, and `'F'` SHALL mark it as active.
2. WHILE a user record has `deleted = 'T'`, THE User_Service SHALL exclude that record from all repository query results regardless of which field is queried; no application-level read operation SHALL return a soft-deleted entity.
3. WHEN a soft-deleted user's username is submitted to `POST /internal/v1/identity/authenticate`, THE User_Service SHALL return `401 Unauthorized` with a response body, status code, and headers identical to the response for an incorrect password, so that no information about the account's existence or deletion status is revealed.

---

### Requirement 9: API Versioning

**User Story:** As an API consumer, I want all endpoints to follow the `/api/v1/` prefix convention, so that the service contract is versioned and consistent with the rest of the platform.

#### Acceptance Criteria

1. THE UserController SHALL expose all user-facing endpoints under the path prefix `/api/v1/users/`.
2. THE InternalController SHALL expose all internal endpoints under the path prefix `/internal/v1/identity/`.
3. Admin operations (list users, delete user) SHALL be exposed under `/internal/v1/identity/users/` via `InternalController`; no `/api/v1/admin/` path prefix SHALL be registered.
4. IF a request is made to a legacy path such as `/api/users/**`, `/internal/users/**`, or `/api/admin/users/**`, THE User_Service SHALL return `404 Not Found`; no endpoint SHALL be registered at these paths.

---

### Requirement 10: Internal Endpoint Network Restriction

**User Story:** As a security engineer, I want the internal identity endpoints to be inaccessible from outside the Kubernetes cluster and from unauthorized pods within the cluster, so that only the Auth Service can call them.

#### Acceptance Criteria

1. THE Kubernetes Ingress resource SHALL NOT define any routing rule that matches paths under `/internal/v1/identity/`; requests to those paths arriving at the Ingress SHALL be dropped or rejected before reaching the service.
2. WHEN a request to a path matching `/internal/v1/identity/**` arrives at the cluster boundary from an external network, THE Ingress SHALL NOT forward the request to the User_Service; no application-level HTTP response SHALL be returned to the external caller.
3. WHEN a cluster-internal service sends a request to the User_Service at a path under `/internal/v1/identity/`, THE User_Service SHALL process the request and return the appropriate application-level response.
4. A Kubernetes `NetworkPolicy` SHALL be defined that permits ingress to the User_Service on its application port only from pods labelled `app=auth-service`; all other pod-to-pod traffic to the User_Service application port SHALL be denied by default.
5. THE application SHALL NOT implement IP-based route filtering for internal endpoints, as pod IPs are ephemeral in Kubernetes and such filtering is unreliable; network-level controls (Ingress + NetworkPolicy) are the designated enforcement mechanism for this POC.

---

### Requirement 11: OpenAPI Documentation

**User Story:** As a developer integrating with the User Service, I want an interactive API specification available at `/swagger-ui.html`, so that I can explore and test endpoints without reading source code.

#### Acceptance Criteria

1. THE User_Service SHALL include `springdoc-openapi-starter-webmvc-ui` as a compile-scoped dependency; WHEN a GET request is sent to `/swagger-ui.html`, THE service SHALL return `200 OK` with the Swagger UI HTML page.
2. THE OpenAPI specification served by the User_Service SHALL document all endpoints under `/api/v1/users/` including their HTTP methods, path parameters, request body schemas, and response schemas.
3. THE OpenAPI specification SHALL NOT expose or document endpoints under `/internal/v1/identity/` to prevent accidental public documentation of internal interfaces.

---

### Requirement 12: Code Coverage Enforcement

**User Story:** As a developer, I want the build to fail if code coverage drops below 70%, so that critical business logic and controller behavior remain well-tested.

#### Acceptance Criteria

1. THE JaCoCo Maven plugin SHALL be configured to enforce a minimum instruction-coverage ratio of 70% across all classes, excluding the `config` package and its subpackages.
2. IF the measured instruction-coverage ratio falls below 70% during the Maven `verify` phase, THE build SHALL exit with a non-zero exit code and produce a coverage violation report showing the measured ratio against the required threshold.
3. IF the Maven `verify` phase runs with no test classes present or with zero instructions measured, THE build SHALL fail rather than silently pass with an empty coverage report.

---

### Requirement 13: Kubernetes Health Probes

**User Story:** As a platform engineer, I want the User Service to expose liveness and readiness probes, so that Kubernetes can detect unhealthy pods and route traffic only to ready instances.

#### Acceptance Criteria

1. THE User_Service SHALL expose a liveness probe at `GET /user-service/actuator/health/liveness`; WHEN the JVM and application process are responsive, THE endpoint SHALL return `200 OK`.
2. THE User_Service SHALL expose a readiness probe at `GET /user-service/actuator/health/readiness`; WHEN the service has an active connection to the PostgreSQL database, THE endpoint SHALL return `200 OK`.
3. IF the PostgreSQL database is unreachable, THE readiness probe SHALL return a non-`200` status so that Kubernetes removes the pod from the load-balancer endpoint pool until connectivity is restored.
4. THE User_Service SHALL be configured with `server.shutdown: graceful` and a maximum drain timeout of 30 seconds; WHEN a shutdown signal is received, THE service SHALL complete all in-flight requests within that window before terminating.

---

### Requirement 14: Structured Error Handling

**User Story:** As an API consumer, I want consistent and meaningful HTTP error responses, so that my client can handle failures predictably.

#### Acceptance Criteria

1. WHEN a `GET /api/v1/users/me` request references a UUID with no matching active user, THE UserController SHALL return `404 Not Found`.
2. WHEN a `POST /internal/v1/identity/users` request causes a database unique-constraint violation on the `username` column, THE User_Service SHALL return `409 Conflict`.
3. WHEN a request body fails Bean Validation (`@Valid`), THE User_Service SHALL return `400 Bad Request` with a response body identifying the failing field(s).
4. WHEN a path variable or the `X-User-Id` header contains a value that is not a valid UUID format, THE User_Service SHALL return `400 Bad Request`.
5. WHEN a `DELETE /internal/v1/identity/users/{id}` request references a UUID with no matching active user, THE InternalController SHALL return `404 Not Found`.
6. THE User_Service SHALL include a `@RestControllerAdvice` global exception handler that maps `DataIntegrityViolationException` to `409 Conflict` and `MethodArgumentNotValidException` to `400 Bad Request`, ensuring consistent error response formatting across all controllers.

---

### Requirement 15: SQL Injection Prevention

**User Story:** As a security engineer, I want all database interactions to use parameterized queries, so that user-supplied input cannot be interpreted as SQL.

#### Acceptance Criteria

1. ALL `UserRepository` query methods SHALL use Spring Data JPA derived queries or `@Query` annotations with named parameter binding (`:param` syntax); string concatenation or interpolation into JPQL or native SQL is prohibited.
2. THE `UserRepository` SHALL NOT contain any native SQL query that incorporates user-supplied input via string formatting, concatenation, or `String.format()`.
3. WHEN `findByUsername(String username)` executes, THE underlying SQL SHALL use a parameterized `WHERE username = ?` predicate generated by Hibernate; the `username` value SHALL be bound as a prepared statement parameter.
4. THE `UserService` SHALL pass all user-supplied values (username, password, UUID) to repository methods as typed Java parameters, never as fragments of a query string.

---

### Requirement 16: User Enumeration Protection

**User Story:** As a security engineer, I want authentication failures to return identical responses regardless of whether the username exists, so that attackers cannot enumerate valid usernames.

#### Acceptance Criteria

1. WHEN `POST /internal/v1/identity/authenticate` is called with a username that does not exist among active users, or that belongs to a soft-deleted account, THE InternalController SHALL return `401 Unauthorized` with a generic error body that does not indicate the reason for failure.
2. WHEN `POST /internal/v1/identity/authenticate` is called with an existing active username but an incorrect password, THE InternalController SHALL return `401 Unauthorized` with a generic error body that does not indicate the reason for failure.
3. THE HTTP status code, response body content, and response headers for the cases in criteria 15.1 and 15.2 SHALL be byte-for-byte identical, so that no client can distinguish between the two failure modes by inspecting the response.
4. WHEN the username in an `AuthRequest` does not exist, THE User_Service SHALL perform a BCrypt hash comparison against a dummy value to consume equivalent computation time, preventing timing side-channel enumeration of valid usernames.
