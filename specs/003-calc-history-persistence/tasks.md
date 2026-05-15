---
description: "Task list for Calculation History with Persistence implementation"
---

# Tasks: Calculation History with Persistence

**Input**: Design documents from `specs/003-calc-history-persistence/`

**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/openapi.yaml ✅, quickstart.md ✅

**Organization**: Tasks grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no competing dependencies)
- **[Story]**: Which user story this task belongs to (US1 = View History, US2 = Persistence Across Restarts, US3 = Clear History)
- Exact file paths included in all descriptions

---

## Phase 1: Setup (Dependencies & Configuration)

**Purpose**: Add JPA + H2 to the project and wire the datasource before any feature code can be written.

- [X] T001 Add `spring-boot-starter-data-jpa` and `h2` (runtime scope) dependencies to `pom.xml`
- [X] T002 [P] Add H2 file-based datasource and JPA properties to `src/main/resources/application.properties`: `spring.datasource.url=jdbc:h2:file:./data/calchistory`, `spring.datasource.driver-class-name=org.h2.Driver`, `spring.jpa.hibernate.ddl-auto=update`, `spring.jpa.database-platform=org.hibernate.dialect.H2Dialect`, `spring.jpa.open-in-view=false`, `spring.h2.console.enabled=false`
- [X] T003 [P] Add `data/` entry to `.gitignore` at the project root to exclude the H2 database files from version control

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create the JPA entity, repository, and DTO that all three user stories depend on. No user story work can begin until this phase is complete.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T004 Create `CalculationHistory` JPA entity in `src/main/java/com/example/calculator/model/CalculationHistory.java` with fields: `id` (Long, `@Id @GeneratedValue`), `username` (String, `@Column(nullable=false)`), `operandA` (Double), `operandB` (Double), `operation` (String, max 20 chars), `result` (Double), `calculatedAt` (Instant); annotate the class with `@Entity @Table(name="CALCULATION_HISTORY", indexes=@Index(name="idx_history_username_at", columnList="USERNAME, CALCULATED_AT DESC"))`
- [X] T005 [P] Create `CalculationHistoryRepository` Spring Data JPA interface in `src/main/java/com/example/calculator/repository/CalculationHistoryRepository.java` extending `JpaRepository<CalculationHistory, Long>`; declare method `List<CalculationHistory> findTop20ByUsernameOrderByCalculatedAtDesc(String username)` and `void deleteAllByUsername(String username)`
- [X] T006 [P] Create `HistoryEntryResponse` DTO record in `src/main/java/com/example/calculator/dto/HistoryEntryResponse.java` with fields: `double operandA`, `double operandB`, `String operation`, `double result`, `String calculatedAt` (ISO-8601 formatted from `Instant`); add a static factory method `HistoryEntryResponse from(CalculationHistory h)` that formats `calculatedAt` via `DateTimeFormatter.ISO_INSTANT`

**Checkpoint**: Run `mvn test` — all 24 existing tests must still pass before continuing.

---

## Phase 3: User Story 1 — View Calculation History (Priority: P1) 🎯 MVP

**Goal**: Authenticated users see their last 20 calculations below the calculator. History is fetched on page load and refreshed after each successful calculation. An empty-state message is shown when no history exists.

**Independent Test**: Start app, log in, perform one or two calculations — history list appears below the calculator. Refresh the page — history remains. Unauthenticated `GET /api/history` returns 401 JSON.

### Tests for User Story 1 ⚠️

> **Write these tests FIRST — they MUST fail before implementation begins**

- [X] T007 [P] [US1] Create `CalculationHistoryRepositoryTest` in `src/test/java/com/example/calculator/repository/CalculationHistoryRepositoryTest.java` using `@DataJpaTest`; cover: saving an entry and retrieving via `findTop20ByUsernameOrderByCalculatedAtDesc` returns it newest-first; saving entries for two different usernames returns only the correct user's entries; saving 25 entries for one user returns only the most recent 20. **Clock determinism** (Constitution Principle II): set explicit `calculatedAt` values on saved entities (e.g., `Instant.parse("2026-01-01T00:00:00Z").plusSeconds(n)`) rather than relying on `Instant.now()` insertion order — this guarantees deterministic ordering under parallel or fast test execution
- [X] T008 [P] [US1] Add history-related tests to `CalculatorControllerTest` in `src/test/java/com/example/calculator/controller/CalculatorControllerTest.java`: `GET /api/history` with `@WithMockUser` returns `200` with a JSON array; `GET /api/history` unauthenticated returns `401` with JSON body `{"error":"Authentication required."}`. **Mock strategy** (`@WebMvcTest` slice — no real DB): add `@MockBean CalculationHistoryRepository historyRepo` to the test class; for a valid `POST /api/calculate`, use `Mockito.verify(historyRepo).save(any(CalculationHistory.class))` to assert the save call occurs (do NOT use a round-trip `GET /api/history` — the mock returns an empty list and would not reflect the save). **FR-011 (no save on failure)**: add a test that sends `POST /api/calculate` with `operandB=0` and `operation=DIVIDE`; assert the response is an error status and `Mockito.verify(historyRepo, never()).save(any())` confirms no history entry is created

### Implementation for User Story 1

- [X] T009 [US1] Add `GET /api/history` endpoint and history-save logic to `CalculatorController` in `src/main/java/com/example/calculator/controller/CalculatorController.java`: inject `CalculationHistoryRepository`; in `calculate()` after a successful result, construct and `save()` a `CalculationHistory` entity using `Principal.getName()` as username and `Instant.now()` as timestamp; add `@GetMapping("/api/history")` method that calls `findTop20ByUsernameOrderByCalculatedAtDesc(principal.getName())` and maps results to `List<HistoryEntryResponse>` returned as `ResponseEntity<List<HistoryEntryResponse>>`
- [X] T010 [US1] Add history panel to `src/main/resources/static/index.html`: add a `<section id="historyPanel">` below the calculator card with a `<h2>` heading "Calculation History", a `<ul id="historyList">` for entries, and a `<p id="historyEmpty">` "No history yet." empty-state paragraph; add JS function `loadHistory()` that calls `fetch('/api/history')` and renders each entry as `<li>` showing the expression (e.g., `"10 ÷ 3 = 3.333..."`) and the timestamp; call `loadHistory()` on page load and after every successful `POST /api/calculate` response

**Checkpoint**: Run `mvn test` — T007 and T008 tests pass. Manually validate quickstart.md AC-1 through AC-3 in a browser.

---

## Phase 4: User Story 2 — History Persists Across Restarts (Priority: P2)

**Goal**: H2 file-based configuration ensures history survives application restarts with no additional code changes beyond Phase 1 setup. This phase validates the configuration is correct and the `./data/` directory is excluded from version control.

**Independent Test**: Perform calculations, stop the app (`Ctrl+C`), restart with `mvn spring-boot:run`, log in — history from before the restart is still displayed.

- [X] T011 [US2] Verify H2 file-based persistence end-to-end: confirm `spring.datasource.url=jdbc:h2:file:./data/calchistory` is set in `src/main/resources/application.properties`; confirm `data/` is in `.gitignore`; confirm `@DataJpaTest` tests in `CalculationHistoryRepositoryTest` use the default in-memory H2 (not the file datasource) by checking no `@AutoConfigureTestDatabase` override is needed — `@DataJpaTest` replaces the datasource with an embedded in-memory DB by default. **Intentionally manual** (Constitution Principle II exception — explicit justification): restart-persistence requires killing and relaunching a JVM process, which cannot be automated within a single Maven Surefire run. T017 (Phase 6 JAR restart) serves as the integration-level restart-persistence validation; this task confirms configuration correctness only

**Checkpoint**: Manually validate quickstart.md AC-4 and AC-5 (restart persistence and per-user isolation) in a browser.

---

## Phase 5: User Story 3 — Clear History (Priority: P3)

**Goal**: An authenticated user can clear their entire history with a single button click. The history section resets to the empty-state message. New calculations start a fresh history.

**Independent Test**: Perform calculations, click "Clear History", confirm history section shows "No history yet.", perform another calculation, confirm only the new entry appears.

### Tests for User Story 3 ⚠️

> **Write these tests FIRST — they MUST fail before implementation begins**

- [X] T012 [P] [US3] Add `DELETE /api/history` tests to `CalculatorControllerTest`: `DELETE /api/history` with `@WithMockUser` returns `204 No Content`; `DELETE /api/history` when history is already empty returns `204` (idempotent); `DELETE /api/history` unauthenticated returns `401` JSON

### Implementation for User Story 3

- [X] T013 [US3] Add `DELETE /api/history` endpoint to `CalculatorController` in `src/main/java/com/example/calculator/controller/CalculatorController.java`: `@DeleteMapping("/api/history")` method that calls `calculationHistoryRepository.deleteAllByUsername(principal.getName())` and returns `ResponseEntity.noContent().build()`
- [X] T014 [US3] Add "Clear History" button to the history panel in `src/main/resources/static/index.html`: a `<button id="clearHistoryBtn">` styled as a secondary/outline button below the history list; JS click handler calls `fetch('/api/history', { method: 'DELETE' })` and on `204` response calls `loadHistory()` to refresh the panel; button is hidden when the history list is empty and shown when entries exist

**Checkpoint**: Run `mvn test` — all tests pass. Manually validate quickstart.md AC-6 through AC-9 in a browser.

---

## Phase 6: Polish & Validation

**Purpose**: Full regression, manual acceptance, and build verification.

- [X] T015 Run `mvn test` and confirm BUILD SUCCESS with all tests passing (24 existing + new repository + new history endpoint tests)
- [X] T016 [P] Manually validate all 9 quickstart.md acceptance scenarios (AC-1 through AC-9) in a browser. **Performance validation** (Constitution Principle IV — budgets declared in plan.md): open browser DevTools → Network tab; (a) confirm `GET /api/history` response time ≤ 100 ms after loading the calculator page; (b) confirm `DELETE /api/history` response time ≤ 100 ms after clicking "Clear History"; (c) compare `mvn spring-boot:run` startup log timestamps before and after JPA/H2 auto-config lines to confirm JPA startup overhead ≤ 2 s
- [ ] T017 [P] Build executable JAR with `mvn package -DskipTests` and verify `java -jar target/calculator-0.0.1-SNAPSHOT.jar` starts, history is accessible after login, and persists across JAR restarts

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion — **BLOCKS all user stories**
- **User Story 1 (Phase 3)**: Depends on Foundational phase
- **User Story 2 (Phase 4)**: Depends on Phase 3 (H2 file config + `.gitignore` are already done in Phase 1; this phase is a validation checkpoint)
- **User Story 3 (Phase 5)**: Depends on Phase 3 (extends `CalculatorController` and `index.html`); independent of Phase 4
- **Polish (Phase 6)**: Depends on all user story phases

### Within Each User Story

1. Tests written first (T007+T008, T012) — MUST fail before implementation
2. Implementation follows: entity → repository → DTO (Phase 2) → endpoints (T009, T013) → HTML/JS (T010, T014)

### Parallel Opportunities

- Phase 1: T002 and T003 in parallel with T001 (different files)
- Phase 2: T005 and T006 in parallel with T004 (T004 must compile first for T005/T006 to import it — start T004 first, then T005+T006 together)
- Phase 3 tests: T007 and T008 in parallel (different test classes)
- Phase 5 tests: T012 independent (new test method in existing class)
- Phase 6: T016 and T017 in parallel

### Story Independence

- US1 can be delivered and tested on its own (provides visible history after login)
- US2 requires only Phase 1 config to be correct — no additional code; can be validated any time after Phase 3 is complete
- US3 adds two files (controller method + HTML button) that are independent of US2

### Suggested MVP

Implement through Phase 3 only (T001–T010). Delivers: calculations saved, history shown, empty state. Persistence (US2) is automatically included as it depends only on Phase 1 config. Clear history (US3) can follow as a separate increment.
