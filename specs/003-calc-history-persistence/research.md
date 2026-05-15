# Research: Calculation History with Persistence

**Feature**: 003-calc-history-persistence
**Date**: 2026-05-15
**Status**: Complete — no NEEDS CLARIFICATION items remain

---

## Decision 1: Persistence technology — H2 file-based database

**Decision**: Use H2 in file-based mode (`jdbc:h2:file:./data/calchistory`) with Spring Data JPA via `spring-boot-starter-data-jpa`.

**Rationale**: The spec requires history to persist across application restarts. H2 file-based mode achieves this without introducing an external database dependency, which fits the project's embedded/standalone deployment model. Spring Data JPA provides declarative repository support, minimising boilerplate. H2 is already widely used in Spring Boot projects for dev/test and its file-based mode is production-suitable for single-process deployments.

**Alternatives considered**:
- H2 in-memory mode — provides JPA/repository convenience but loses data on restart. Does not satisfy FR-006. Rejected.
- PostgreSQL/MySQL — production-grade but requires an external database server, adds operational complexity, and is disproportionate for this single-user embedded application. Rejected.
- JDBC + manual SQL — avoids JPA startup overhead but significantly more boilerplate for repository operations. Rejected in favour of Spring Data JPA.
- SQLite via `xerial/sqlite-jdbc` — would work but is not a supported Spring Boot auto-configured datasource, requiring more wiring. H2 has first-class Spring Boot support. Rejected.

---

## Decision 2: Entity design — `CalculationHistory`

**Decision**: A single JPA entity `CalculationHistory` (table `CALCULATION_HISTORY`) with the following fields:
- `id` — `Long`, auto-generated primary key
- `username` — `String`, non-null; stores the `Principal.getName()` value at calculation time
- `operandA` — `Double`, non-null
- `operandB` — `Double`, non-null
- `operation` — `String`, non-null; stores the `Operation` enum's `name()`
- `result` — `Double`, non-null
- `calculatedAt` — `Instant`, non-null; set to `Instant.now()` in the service/controller layer

**Rationale**: Storing `operation` as a `String` (enum name) avoids a JPA `@Enumerated` change when adding operations in future and keeps the schema simple. `Instant` for the timestamp ensures timezone-neutrality and sorts correctly. Keeping operands and result as `Double` (boxed) matches the existing `CalculatorService` signature and allows straightforward display in the UI.

**Alternatives considered**:
- Embedding the operation enum directly with `@Enumerated(EnumType.STRING)` — equally valid and slightly more type-safe, but adds a migration concern if enum values are renamed. `String` name storage is simpler for this scope.
- Storing a pre-formatted expression `String` (e.g., `"3.0 + 4.0 = 7.0"`) — simplifies the API response but loses structured data, preventing future filtering by operation type. Rejected.

---

## Decision 3: Repository query — `findTop20ByUsernameOrderByCalculatedAtDesc`

**Decision**: Use a Spring Data derived query method `findTop20ByUsernameOrderByCalculatedAtDesc(String username)` on the `CalculationHistoryRepository` interface.

**Rationale**: Spring Data generates the JPQL query automatically from the method name. `Top20` enforces the FR-010 display limit at the database level (most efficient; no in-memory trimming). `OrderByCalculatedAtDesc` delivers newest-first as required by FR-003. No custom `@Query` annotation is needed for this straightforward query.

**Alternatives considered**:
- `findByUsernameOrderByCalculatedAtDesc` with a `Pageable` parameter — more flexible but adds unnecessary complexity for a fixed-limit query. Rejected.
- Fetch all and trim in Java — wastes database I/O proportional to total history length. Rejected.

---

## Decision 4: API surface — new endpoints on `CalculatorController`

**Decision**: Add two endpoints to the existing `CalculatorController`:
- `GET /api/history` — returns the current user's last 20 history entries as a JSON array
- `DELETE /api/history` — permanently deletes all history entries for the current user; returns `204 No Content`

Inject `Principal` (or `SecurityContextHolder`) for the username; both are available in Spring MVC controllers when `SecurityConfig` requires authentication.

**Rationale**: Co-locating the history endpoints in `CalculatorController` keeps the surface area minimal (no new controller class) and is consistent with the `/api/**` routing already established. The spec does not require pagination, advanced filtering, or admin access — a two-endpoint surface is the simplest design that satisfies all functional requirements.

**Alternatives considered**:
- Separate `HistoryController` — justified only if the controller grows significantly larger. For two endpoints, the overhead is not warranted. Rejected.
- `DELETE /api/history/{id}` for per-entry deletion — not in spec. Out of scope. Rejected.
- Using a `@AuthenticationPrincipal UserDetails` parameter — equivalent to `Principal`; either works. `Principal` is simpler and avoids importing Spring Security types into the controller. Chosen for simplicity.

---

## Decision 5: History save — in `CalculatorController.calculate()` after successful result

**Decision**: After `calculatorService.calculate()` returns a valid result (no exception thrown), the controller constructs a `CalculationHistory` entity and calls `calculationHistoryRepository.save()`.

**Rationale**: Save happens only when a result is returned — exceptions from the service (e.g., division by zero) are caught by `@ExceptionHandler` methods and bypass the save call. This naturally satisfies FR-011 (failed calculations do not create history entries) without any explicit conditional logic.

**Alternatives considered**:
- Save in `CalculatorService` — would require injecting the repository into the service layer, coupling service to persistence. Controllers injecting repositories for secondary concerns (side effects on a primary operation) is acceptable at this scale. Rejected in favour of controller-level save.
- AOP `@AfterReturning` advice — adds framework complexity. Rejected.
- Saving asynchronously — adds complexity and ordering concerns. Not needed for this scale. Rejected.

---

## Decision 6: Frontend integration — history panel in `index.html`

**Decision**: Add a history section below the calculator in `index.html`. JavaScript:
1. Fetches `GET /api/history` on page load (after the existing calculator initialisation).
2. Calls the same fetch and re-renders the list after each successful calculation response.
3. Renders each entry as `"operandA operation operandB = result"` (e.g., `"3 + 4 = 7"`).
4. Shows `"No history yet."` when the array is empty.
5. A "Clear History" button sends `DELETE /api/history` (no request body), then re-fetches and re-renders.

**Rationale**: Reuses the existing `fetch` + CSRF cookie pattern already established in the calculator. No additional JS libraries or build toolchain needed. Immediate re-render after each calculation satisfies SC-001 (history appears without extra user action).

**CSRF note**: `DELETE /api/history` hits `/api/**` which is already CSRF-exempt in `SecurityConfig`. No CSRF token header needed for DELETE requests to this path.

**Alternatives considered**:
- Server-sent events / WebSocket for live updates — far out of scope. Rejected.
- Storing history in `localStorage` — does not satisfy FR-006 (server-side persistence) or FR-007 (user isolation). Rejected.

---

## Decision 7: Graceful degradation on database unavailability

**Decision**: Wrap history fetch/save failures in the controller with a try/catch that logs the error and returns an empty list (for `GET /api/history`) or logs and proceeds silently (for save). The main `calculate` endpoint continues to work even if history save fails.

**Rationale**: Satisfies the edge-case requirement from the spec: "If the persistent store is unavailable at startup, the application starts normally; history retrieval returns an empty list." Spring Boot's auto-configuration will throw at startup if the datasource URL is unreachable, so this is primarily a runtime resilience concern after startup.

**Alternatives considered**:
- Propagate DB errors to the client — would cause the calculator to fail entirely on a DB error. Disproportionate response. Rejected.

---

## Performance Budget

| Concern | Budget | Validation |
|---|---|---|
| `GET /api/history` response time | ≤ 100 ms p95 | MockMvc integration test |
| `DELETE /api/history` response time | ≤ 100 ms p95 | MockMvc integration test |
| History render (JS, 20 entries) | ≤ 50 ms | Manual browser check |
| JPA startup overhead | ≤ 2 s additional startup time | Observable via `mvn spring-boot:run` |
