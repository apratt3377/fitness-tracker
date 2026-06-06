# Requirements Document

## Introduction

The Workout Management Service (WMS) is a Spring Boot 3.4 / Java 21 microservice that owns the full lifecycle of user workouts within the Fitness Tracker platform. It exposes a REST API for creating, reading, updating, completing, and soft-deleting workouts, as well as managing the exercise entries within each workout and providing read-only access to the shared exercise catalog. The service publishes domain events (`workout_completed`, `workout_deleted`) to the `Workout-Domain-Events` SNS topic after each state-changing operation using a fire-and-forget pattern. Per-user data isolation is enforced at the data layer using the `X-User-Id` header injected by the API Gateway.

## Glossary

- **WMS**: Workout Management Service — the system described by this document.
- **Workout**: A user-owned entity representing a single training session, stored in `fitness.workouts`.
- **WorkoutStatus**: An enumeration with exactly two values: `PLANNED` (initial state) and `COMPLETED` (terminal state).
- **WorkoutExercise**: An entry linking a Workout to an Exercise catalog item, stored in `fitness.workout_exercises`.
- **Exercise**: A read-only catalog entry stored in `fitness.exercises`; WMS never writes to this table.
- **ExerciseCatalog**: The complete set of `Exercise` records in `fitness.exercises`.
- **API_Gateway**: The Spring Cloud Gateway component upstream of WMS that validates JWTs and injects `X-User-Id`.
- **X-User-Id**: An HTTP request header carrying the authenticated user's UUID, injected by the API_Gateway on every request to `/api/v1/workouts/**`.
- **WorkoutController**: The `@RestController` component that handles all HTTP requests for workout lifecycle operations.
- **ExerciseController**: The `@RestController` component that handles read-only requests to the exercise catalog.
- **WorkoutService**: The `@Service` component that contains all business logic, user isolation enforcement, and event orchestration.
- **WorkoutRepository**: The Spring Data JPA `@Repository` for `fitness.workouts` and `fitness.workout_exercises`.
- **ExerciseRepository**: The read-only Spring Data JPA `@Repository` for `fitness.exercises`.
- **WorkoutEventPublisher**: The domain-level port interface (`publishWorkoutCompleted`, `publishWorkoutDeleted`) that decouples `WorkoutService` from SNS infrastructure. Mockable in tests without LocalStack.
- **SnsWorkoutEventPublisher**: The production `@Service` implementation of `WorkoutEventPublisher`. Lives inside WMS. Holds the `SnsClient` and handles all SNS-specific logic — serialization, `MessageAttribute` construction, and exception handling.
- **SNS_Topic**: The `Workout-Domain-Events` AWS SNS topic (`arn:aws:sns:us-east-1:000000000000:Workout-Domain-Events`).
- **GlobalExceptionHandler**: The `@RestControllerAdvice` component that maps domain and framework exceptions to structured JSON error responses.
- **Soft-delete**: The mechanism of setting `deleted_at` to a non-null `TIMESTAMPTZ` value to logically remove a record without physically deleting the row.
- **Fire-and-forget**: An async publish pattern where the HTTP response is returned to the caller before SNS publish completes, and SNS failures do not affect the HTTP response or database state.
- **Actuator**: Spring Boot Actuator, which exposes health, readiness, and metrics endpoints.

---

## Requirements

### Requirement 1: Create Workout

**User Story:** As a fitness tracker user, I want to create a new workout, so that I can record and plan my training sessions.

#### Acceptance Criteria

1. WHEN a `POST /api/v1/workouts` request is received with a valid `X-User-Id` header, THE WMS SHALL create a new Workout with `status = PLANNED`, assign it the `userId` from the `X-User-Id` header, and return `201 Created` with the full `WorkoutResponse` body.
2. WHEN a Workout is created, THE WMS SHALL set `deleted_at` to `NULL` and set `created_at` and `updated_at` to the current timestamp.
3. IF the `X-User-Id` header is absent from a `POST /api/v1/workouts` request, THEN THE WMS SHALL return `400 Bad Request`.
4. IF the `X-User-Id` header value cannot be parsed as a valid UUID (8-4-4-4-12 hexadecimal format), THEN THE WMS SHALL return `400 Bad Request`.
5. IF the `name` field in the `CreateWorkoutRequest` exceeds 255 characters, THEN THE WMS SHALL return `400 Bad Request` with a validation error body identifying the field.

---

### Requirement 2: List User Workouts

**User Story:** As a fitness tracker user, I want to list my active workouts, so that I can see all my ongoing and planned training sessions.

#### Acceptance Criteria

1. WHEN a `GET /api/v1/workouts` request is received with a valid `X-User-Id` header, THE WMS SHALL return `200 OK` with a JSON array of `WorkoutSummaryResponse` objects containing only Workouts where `user_id` matches the `X-User-Id` and `deleted_at IS NULL`; if no active workouts exist, the response SHALL be an empty array, not a `404`.
2. WHILE a Workout has a non-null `deleted_at`, THE WMS SHALL exclude it from all `GET /api/v1/workouts` list responses regardless of the requesting `userId`.
3. WHEN the `GET /api/v1/workouts` request is received, THE WMS SHALL exclude Workouts belonging to any `userId` other than the one present in the `X-User-Id` header.
4. WHEN the `GET /api/v1/workouts` list is returned, THE WMS SHALL include an `exerciseCount` field in each `WorkoutSummaryResponse` reflecting the current number of exercises in that Workout.
5. WHEN the `GET /api/v1/workouts` list is returned, THE WMS SHALL order results by `created_at` descending so that the most recently created Workout appears first.
6. IF the `X-User-Id` header is absent or cannot be parsed as a valid UUID, THEN THE WMS SHALL return `400 Bad Request`.

---

### Requirement 3: Get Single Workout

**User Story:** As a fitness tracker user, I want to retrieve the full details of a specific workout including its exercises, so that I can review my training session.

#### Acceptance Criteria

1. WHEN a `GET /api/v1/workouts/{id}` request is received with a valid `X-User-Id` header and the Workout exists, is not soft-deleted, and is owned by the requesting user, THE WMS SHALL return `200 OK` with the full `WorkoutResponse` including the complete list of `WorkoutExerciseResponse` entries.
2. IF the Workout identified by `{id}` does not exist or has a non-null `deleted_at`, THE WMS SHALL return `404 Not Found` regardless of which user made the request.
3. IF the Workout identified by `{id}` exists, has a null `deleted_at`, but belongs to a different `userId` than the `X-User-Id` header, THEN THE WMS SHALL return `403 Forbidden`.
4. IF the `X-User-Id` header is absent, THEN THE WMS SHALL return `400 Bad Request`.
5. IF the `{id}` path variable or the `X-User-Id` header value cannot be parsed as a valid UUID, THEN THE WMS SHALL return `400 Bad Request`.

---

### Requirement 4: Update Workout Metadata

**User Story:** As a fitness tracker user, I want to update my workout's name, description, and date, so that I can correct or refine my session details.

#### Acceptance Criteria

1. WHEN a `PUT /api/v1/workouts/{id}` request is received with a valid `X-User-Id` header and the Workout exists, is not soft-deleted, and is owned by the requesting user, THE WMS SHALL update the `name`, `description`, and `workout_date` fields and return `200 OK` with the updated `WorkoutResponse`.
2. WHEN a Workout is updated, THE WMS SHALL advance `updated_at` to the current timestamp and leave all other fields (`status`, `user_id`, `created_at`, `deleted_at`) unchanged.
3. WHEN a `PUT /api/v1/workouts/{id}` request omits the `name` field, THE WMS SHALL treat `name` as unchanged; `workout_date` is also nullable and may be omitted or set to `null`.
4. IF the `name` field in the `UpdateWorkoutRequest` exceeds 255 characters, THEN THE WMS SHALL return `400 Bad Request`.
5. IF the `X-User-Id` header is absent or cannot be parsed as a valid UUID, THEN THE WMS SHALL return `400 Bad Request`.
6. IF the Workout identified by `{id}` does not exist or has a non-null `deleted_at`, THEN THE WMS SHALL return `404 Not Found`.
7. IF the Workout identified by `{id}` belongs to a different `userId`, THEN THE WMS SHALL return `403 Forbidden`.

---

### Requirement 5: Complete Workout

**User Story:** As a fitness tracker user, I want to mark a workout as completed, so that it can be processed for analytics and I can track my training history.

#### Acceptance Criteria

1. WHEN a `POST /api/v1/workouts/{id}/complete` request is received with a valid `X-User-Id` header and the Workout is in `PLANNED` status and is owned by the requesting user, THE WMS SHALL set `status = COMPLETED` in the database and return `202 Accepted` with an empty response body.
2. AFTER returning `202 Accepted`, THE WMS SHALL asynchronously publish a `workout_completed` event to the SNS_Topic with a `MessageAttribute` named `action` of type `String` and value `"workout_completed"`.
3. IF the Workout is already in `COMPLETED` status, THEN THE WMS SHALL return `409 Conflict` without modifying database state or publishing any SNS event.
4. IF the `X-User-Id` header is absent or cannot be parsed as a valid UUID, THEN THE WMS SHALL return `400 Bad Request`.
5. IF the Workout does not exist or has a non-null `deleted_at`, THEN THE WMS SHALL return `404 Not Found`.
6. IF the Workout belongs to a different `userId`, THEN THE WMS SHALL return `403 Forbidden`.
7. WHEN the SNS publish for `workout_completed` fails, THE WMS SHALL log the failure without modifying the committed `status = COMPLETED` database state; the `202 Accepted` response has already been sent and is not affected.

---

### Requirement 6: Soft-Delete Workout

**User Story:** As a fitness tracker user, I want to delete a workout, so that it is removed from my active list and downstream services can clean up associated data.

#### Acceptance Criteria

1. WHEN a `DELETE /api/v1/workouts/{id}` request is received with a valid `X-User-Id` header and the Workout exists, has a null `deleted_at`, and is owned by the requesting user, THE WMS SHALL set `deleted_at` to the current `TIMESTAMPTZ`, return `204 No Content`, and then asynchronously publish a `workout_deleted` event to the SNS_Topic.
2. AFTER returning `204 No Content`, THE WMS SHALL asynchronously publish a `workout_deleted` event with a `MessageAttribute` named `action` of type `String` and value `"workout_deleted"`.
3. IF the Workout does not exist or already has a non-null `deleted_at`, THEN THE WMS SHALL return `404 Not Found` regardless of which user made the request.
4. IF the Workout exists, has a null `deleted_at`, but belongs to a different `userId`, THEN THE WMS SHALL return `403 Forbidden`.
5. IF the `X-User-Id` header is absent or cannot be parsed as a valid UUID, THEN THE WMS SHALL return `400 Bad Request`.
6. WHEN the SNS publish for `workout_deleted` fails, THE WMS SHALL log the failure without reversing the committed `deleted_at` database state; the `204 No Content` response has already been sent and is not affected.

---

### Requirement 7: Add Exercise to Workout

**User Story:** As a fitness tracker user, I want to add exercises to my workout, so that I can track the specific movements I performed.

#### Acceptance Criteria

1. WHEN a `POST /api/v1/workouts/{id}/exercises` request is received with a valid `X-User-Id`, a valid `exerciseId` that exists in the ExerciseCatalog, and all numeric fields pass validation, THE WMS SHALL persist a new `WorkoutExercise` entry and return `201 Created` with the `WorkoutExerciseResponse`.
2. IF the `exerciseId` in the `AddExerciseRequest` does not exist in the ExerciseCatalog, THEN THE WMS SHALL return `404 Not Found` and not persist any `WorkoutExercise` entry.
3. IF `sets` is present and less than or equal to zero, THEN THE WMS SHALL return `400 Bad Request` before issuing any database write.
4. IF `reps` is present and less than or equal to zero, THEN THE WMS SHALL return `400 Bad Request` before issuing any database write.
5. IF `weight` is present and less than zero, THEN THE WMS SHALL return `400 Bad Request` before issuing any database write.
6. IF the `exerciseId` field is absent from the `AddExerciseRequest`, THEN THE WMS SHALL return `400 Bad Request`.
7. IF the Workout does not exist or has a non-null `deleted_at`, THEN THE WMS SHALL return `404 Not Found`; IF the Workout exists but belongs to a different `userId`, THEN THE WMS SHALL return `403 Forbidden`.
8. IF the `X-User-Id` header is absent or cannot be parsed as a valid UUID, THEN THE WMS SHALL return `400 Bad Request`.

---

### Requirement 8: Update Exercise Entry

**User Story:** As a fitness tracker user, I want to update the sets, reps, weight, and other details of an exercise in my workout, so that I can correct entries or record actual performance.

#### Acceptance Criteria

1. WHEN a `PUT /api/v1/workouts/{workoutId}/exercises/{exerciseId}` request is received with valid fields and the exercise entry exists within the specified Workout owned by the requesting user, THE WMS SHALL update the exercise entry fields and return `200 OK` with the updated `WorkoutExerciseResponse`.
2. IF `sets` is less than or equal to zero, THEN THE WMS SHALL return `400 Bad Request` before issuing any database write.
3. IF `reps` is less than or equal to zero, THEN THE WMS SHALL return `400 Bad Request` before issuing any database write.
4. IF `weight` is less than zero, THEN THE WMS SHALL return `400 Bad Request` before issuing any database write.
5. IF the Workout or exercise entry does not exist, THEN THE WMS SHALL return `404 Not Found`.
6. IF the Workout belongs to a different `userId`, THEN THE WMS SHALL return `403 Forbidden`.
7. IF the `X-User-Id` header is absent, THEN THE WMS SHALL return `400 Bad Request`.

---

### Requirement 9: Remove Exercise from Workout

**User Story:** As a fitness tracker user, I want to remove an exercise from my workout, so that I can correct mistakes or reorganize my session.

#### Acceptance Criteria

1. WHEN a `DELETE /api/v1/workouts/{workoutId}/exercises/{exerciseId}` request is received and the exercise entry exists within the specified Workout owned by the requesting user, THE WMS SHALL remove the `WorkoutExercise` entry and return `204 No Content`.
2. IF the exercise entry or Workout does not exist, THEN THE WMS SHALL return `404 Not Found`.
3. IF the Workout belongs to a different `userId`, THEN THE WMS SHALL return `403 Forbidden`.
4. IF the `X-User-Id` header is absent, THEN THE WMS SHALL return `400 Bad Request`.

---

### Requirement 10: List Exercise Catalog

**User Story:** As a fitness tracker user, I want to browse the exercise catalog with optional filters, so that I can find exercises relevant to my training goals.

#### Acceptance Criteria

1. WHEN a `GET /api/v1/exercises` request is received, THE WMS SHALL return `200 OK` with a list of `ExerciseResponse` objects representing all exercises in the ExerciseCatalog.
2. WHEN a `GET /api/v1/exercises` request includes a `type` query parameter, THE WMS SHALL return only exercises whose `type` field matches the provided value.
3. WHEN a `GET /api/v1/exercises` request includes a `targetArea` query parameter, THE WMS SHALL return only exercises whose `target_area` field matches the provided value.
4. WHEN a `GET /api/v1/exercises` request includes a `difficulty` query parameter, THE WMS SHALL return only exercises whose `difficulty` field matches the provided value.
5. WHEN a `GET /api/v1/exercises` request includes an `equipment` query parameter, THE WMS SHALL return only exercises whose `equipment` field matches the provided value.
6. WHEN multiple filter parameters are provided, THE WMS SHALL return only exercises matching all provided filters simultaneously.
7. THE ExerciseRepository SHALL operate in read-only mode; WMS SHALL never issue any INSERT, UPDATE, or DELETE against the `fitness.exercises` table.

---

### Requirement 11: Get Single Exercise

**User Story:** As a fitness tracker user, I want to retrieve the details of a specific exercise, so that I can view its description before adding it to my workout.

#### Acceptance Criteria

1. WHEN a `GET /api/v1/exercises/{id}` request is received and the Exercise exists, THE WMS SHALL return `200 OK` with the `ExerciseResponse`.
2. IF the Exercise identified by `{id}` does not exist in the ExerciseCatalog, THEN THE WMS SHALL return `404 Not Found`.

---

### Requirement 12: SNS Event Publishing — Workout Completed

**User Story:** As the analytics pipeline, I want to receive a `workout_completed` event after a workout is marked complete, so that the Analytics Service can trigger AI analysis.

#### Acceptance Criteria

1. WHEN `completeWorkout` succeeds and the database transaction commits, THE `WorkoutEventPublisher` SHALL publish a `workout_completed` event to the SNS_Topic as a fire-and-forget asynchronous operation.
2. THE `SnsWorkoutEventPublisher` SHALL set the SNS `MessageAttribute` named `action` to `dataType = "String"` and `stringValue = "workout_completed"` on every `workout_completed` publish call.
3. WHEN a `workout_completed` event is published, THE `SnsWorkoutEventPublisher` SHALL include `workoutId`, `userId`, `workoutName`, `completedAt`, and the list of exercise summaries in the JSON message body.
4. IF the SNS publish call throws an exception, THEN THE `SnsWorkoutEventPublisher` SHALL catch the exception, log it, and not rethrow it to the caller.
5. WHEN a database error prevents the `status = COMPLETED` commit, THE `WorkoutEventPublisher` SHALL not publish any SNS event.
6. THE `WorkoutService` SHALL depend on the `WorkoutEventPublisher` interface, not on `SnsWorkoutEventPublisher` directly, so that tests can mock the publisher without requiring a LocalStack instance.

---

### Requirement 13: SNS Event Publishing — Workout Deleted

**User Story:** As the cleanup pipeline, I want to receive a `workout_deleted` event after a workout is soft-deleted, so that the Notification Service can purge associated data.

#### Acceptance Criteria

1. WHEN `deleteWorkout` succeeds and the database transaction commits, THE `WorkoutEventPublisher` SHALL publish a `workout_deleted` event to the SNS_Topic as a fire-and-forget asynchronous operation.
2. THE `SnsWorkoutEventPublisher` SHALL set the SNS `MessageAttribute` named `action` to `dataType = "String"` and `stringValue = "workout_deleted"` on every `workout_deleted` publish call.
3. WHEN a `workout_deleted` event is published, THE `SnsWorkoutEventPublisher` SHALL include `workoutId`, `userId`, and `deletedAt` in the JSON message body.
4. IF the SNS publish call throws an exception, THEN THE `SnsWorkoutEventPublisher` SHALL catch the exception, log it, and not rethrow it to the caller.
5. WHEN a database error prevents the `deleted_at` commit, THE `WorkoutEventPublisher` SHALL not publish any SNS event.

---

### Requirement 14: Structured Error Handling

**User Story:** As a client developer, I want all API errors to return a consistent JSON structure, so that I can handle errors uniformly in the frontend.

#### Acceptance Criteria

1. THE GlobalExceptionHandler SHALL return a JSON error body containing `timestamp`, `status`, `error`, `message`, and `path` fields for every non-2xx response.
2. WHEN a `WorkoutNotFoundException` is raised, THE GlobalExceptionHandler SHALL return `404 Not Found` with the structured error body.
3. WHEN an `AccessDeniedException` is raised, THE GlobalExceptionHandler SHALL return `403 Forbidden` with the structured error body.
4. WHEN a `WorkoutAlreadyCompletedException` is raised, THE GlobalExceptionHandler SHALL return `409 Conflict` with the structured error body.
5. WHEN a `MissingRequestHeaderException` is raised, THE GlobalExceptionHandler SHALL return `400 Bad Request` with the structured error body.
6. WHEN a `MethodArgumentNotValidException` is raised, THE GlobalExceptionHandler SHALL return `400 Bad Request` with a message describing the validation failure.
7. WHEN an `ExerciseNotFoundException` is raised, THE GlobalExceptionHandler SHALL return `404 Not Found` with the structured error body.
8. WHEN an unexpected exception occurs, THE GlobalExceptionHandler SHALL return `500 Internal Server Error` with a sanitized message that does not expose stack traces or internal implementation details.

---

### Requirement 15: User Identity Enforcement

**User Story:** As a system operator, I want WMS to enforce that every workout request carries a valid user identity, so that data isolation is guaranteed and unauthenticated direct-to-service requests are rejected.

#### Acceptance Criteria

1. THE WorkoutController SHALL require the `X-User-Id` header on every handler method mapped to `/api/v1/workouts/**`.
2. WHEN the `X-User-Id` header value cannot be parsed as a valid UUID, THE WMS SHALL return `400 Bad Request`.
3. THE WorkoutService SHALL pass the `userId` derived from `X-User-Id` as a parameter to every WorkoutRepository query.
4. THE WorkoutService SHALL never issue a WorkoutRepository query that omits the `user_id` predicate.

---

### Requirement 16: Per-User Data Isolation

**User Story:** As a fitness tracker user, I want my workout data to be completely isolated from other users, so that my training history is private.

#### Acceptance Criteria

1. WHEN any WorkoutRepository query is executed, THE WMS SHALL include `user_id = :userId` as a filter predicate in the WHERE clause.
2. IF a request targets a Workout that exists but has a `user_id` different from the `X-User-Id` header value, THEN THE WMS SHALL return `403 Forbidden`.
3. WHEN listing workouts, THE WMS SHALL return only Workouts where `user_id` matches the requesting `userId` AND `deleted_at IS NULL`.
4. THE WMS SHALL never expose a workout's data to a `userId` that does not own that Workout, even if the `workoutId` is correctly guessed.

---

### Requirement 17: Workout Status Lifecycle

**User Story:** As a fitness tracker user, I want workout status transitions to be strictly enforced, so that completed workouts cannot be accidentally reset.

#### Acceptance Criteria

1. WHEN a Workout is created, THE WMS SHALL set its initial `status` to `PLANNED`.
2. THE WMS SHALL allow only the `PLANNED → COMPLETED` status transition via the complete-workout operation.
3. IF a request attempts to complete a Workout that is already `COMPLETED`, THEN THE WMS SHALL return `409 Conflict`.
4. THE WMS SHALL provide no API operation that transitions a Workout from `COMPLETED` back to `PLANNED`.

---

### Requirement 18: Soft-Delete Data Integrity

**User Story:** As a system, I want soft-deleted workouts to be permanently invisible to all active queries, so that deleted data does not surface to users or downstream operations.

#### Acceptance Criteria

1. WHEN a Workout is soft-deleted, THE WMS SHALL set `deleted_at` to a non-null `TIMESTAMPTZ` value representing the deletion time.
2. WHILE a Workout has a non-null `deleted_at`, THE WMS SHALL return `404 Not Found` for any `getWorkout`, `completeWorkout`, or `deleteWorkout` request targeting that Workout.
3. WHILE a Workout has a non-null `deleted_at`, THE WMS SHALL exclude it from all `listWorkouts` results.
4. WHILE a Workout has a non-null `deleted_at`, THE WMS SHALL return `404 Not Found` for any add, update, or remove exercise operation targeting that Workout.
5. THE WMS SHALL never physically delete rows from `fitness.workouts`; all deletions SHALL be soft-deletes via `deleted_at`.

---

### Requirement 19: Exercise Entry Numeric Validation

**User Story:** As a fitness tracker user, I want exercise entries to enforce valid numeric values, so that my workout data is always meaningful and consistent.

#### Acceptance Criteria

1. THE WMS SHALL reject any `AddExerciseRequest` where `sets` is not a positive integer (i.e., `sets ≤ 0`) with `400 Bad Request`.
2. THE WMS SHALL reject any `AddExerciseRequest` where `reps` is not a positive integer (i.e., `reps ≤ 0`) with `400 Bad Request`.
3. THE WMS SHALL reject any `AddExerciseRequest` where `weight` is negative (i.e., `weight < 0`) with `400 Bad Request`.
4. THE WMS SHALL reject any `UpdateExerciseRequest` where `sets` is not a positive integer with `400 Bad Request`.
5. THE WMS SHALL reject any `UpdateExerciseRequest` where `reps` is not a positive integer with `400 Bad Request`.
6. THE WMS SHALL reject any `UpdateExerciseRequest` where `weight` is negative with `400 Bad Request`.
7. WHEN validation fails on an exercise entry request, THE WMS SHALL not issue any database write for the rejected request.

---

### Requirement 20: OpenAPI Documentation

**User Story:** As an API consumer, I want interactive API documentation available at a well-known URL, so that I can explore and test endpoints without reading source code.

#### Acceptance Criteria

1. THE WMS SHALL expose OpenAPI/Swagger UI at `/swagger-ui.html` via the `springdoc-openapi-starter-webmvc-ui` dependency.
2. THE WMS SHALL expose the OpenAPI specification in JSON format at `/v3/api-docs`.

---

### Requirement 21: Test Coverage Enforcement

**User Story:** As a software engineer, I want a minimum code coverage threshold enforced at build time, so that untested code paths cannot be merged without explicit acknowledgment.

#### Acceptance Criteria

1. THE WMS build SHALL fail if the JaCoCo code coverage falls below 70% on any measured metric (line or branch coverage).
2. THE WMS SHALL exclude configuration classes from JaCoCo coverage measurement.

---

### Requirement 22: Kubernetes Health Probes

**User Story:** As a platform operator, I want WMS to expose liveness and readiness endpoints compatible with Kubernetes probes, so that the cluster can manage pod lifecycle safely.

#### Acceptance Criteria

1. THE WMS SHALL expose a liveness probe endpoint at `/actuator/health/liveness` via Spring Boot Actuator.
2. THE WMS SHALL expose a readiness probe endpoint at `/actuator/health/readiness` via Spring Boot Actuator.
3. WHEN the WMS is shutting down, THE WMS SHALL complete in-flight requests before terminating (graceful shutdown via `server.shutdown: graceful`).

---

### Requirement 23: Exercise Catalog Read-Only Enforcement

**User Story:** As a database administrator, I want the exercise catalog to be protected from writes by WMS, so that the shared catalog data remains authoritative and unmodified.

#### Acceptance Criteria

1. THE ExerciseRepository SHALL be annotated with Hibernate `@Immutable` on the `ExerciseEntity` to prevent accidental writes.
2. THE WMS SHALL not expose any API endpoint that creates, modifies, or deletes records in `fitness.exercises`.
3. WHEN `addExercise` is called, THE WMS SHALL write only to `fitness.workout_exercises`; it SHALL not write to `fitness.exercises`.

---

### Requirement 24: SNS Fire-and-Forget Isolation

**User Story:** As a backend engineer, I want SNS publish failures to be fully isolated from the HTTP response, so that transient messaging failures do not degrade the user-facing API.

#### Acceptance Criteria

1. WHEN an SNS publish fails after a successful database commit, THE WMS SHALL return the appropriate success HTTP response (`202 Accepted` for complete-workout or `204 No Content` for soft-delete) to the caller without modification.
2. WHEN an SNS publish fails, THE WMS SHALL log the exception with sufficient detail for diagnosis.
3. THE WMS SHALL invoke SNS publish asynchronously after the database transaction has committed, ensuring the HTTP response is not blocked by SNS latency.
4. THE WMS SHALL not roll back or otherwise modify committed database state due to an SNS publish failure.
