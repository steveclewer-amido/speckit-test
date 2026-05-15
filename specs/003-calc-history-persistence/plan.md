# Implementation Plan: Calculation History with Persistence

**Branch**: `003-calc-history-persistence` | **Date**: 2026-05-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/003-calc-history-persistence/spec.md`

## Summary

Add server-side calculation history to the existing Spring Boot calculator application. After each successful calculation, the result is saved to an H2 file-based database (persisting across restarts). The authenticated user sees their 20 most recent calculations in a history panel below the calculator — loaded on page load and refreshed after every calculation. A "Clear History" button deletes all entries for the current user. History is fully per-user; no user sees another user's entries.

## Technical Context

**Language/Version**: Java 17 (LTS; same as features 001 and 002)

**Primary Dependencies**: Spring Boot 3.4.x; `spring-boot-starter-data-jpa`; H2 `runtime` (file-based mode); existing: spring-boot-starter-web, spring-boot-starter-validation, spring-boot-starter-security, spring-security-test

**Storage**: H2 file-based database at `jdbc:h2:file:./data/calchistory`; schema managed by `spring.jpa.hibernate.ddl-auto=update`; single table `CALCULATION_HISTORY`

**Testing**: JUnit 5 + MockMvc + `@WithMockUser` (spring-security-test); in-memory H2 datasource for tests; new `CalculationHistoryRepositoryTest`; updated `CalculatorControllerTest` for new endpoints and history save behaviour

**Target Platform**: Desktop browser (same as features 001 and 002)

**Project Type**: Existing single Maven project — additive change only

**Performance Goals**: `GET /api/history` ≤ 100 ms p95; `DELETE /api/history` ≤ 100 ms p95; history panel JS render (20 entries) ≤ 50 ms; JPA/H2 startup overhead ≤ 2 s additional startup time

**Constraints**: No Thymeleaf; all API responses are JSON; frontend is static HTML5+JS; `/api/**` is CSRF-exempt (already configured); user identity obtained via `Principal` injection in controller; `DELETE /api/history` requires no CSRF token

**Scale/Scope**: Single user account (extensible to multi-user without schema changes); additive change to existing project (~4 new files, 3 modified files, 1 new test class, 1 modified test class)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|---|---|---|
| I. Code Quality | ✅ PASS | New surface: 1 entity, 1 repository interface, 1 DTO, 2 new controller methods. No unnecessary abstractions; repository pattern mandated by Spring Data JPA. |
| II. Testing Standards | ✅ PASS | `CalculationHistoryRepositoryTest` (slice test) covers `findTop20ByUsernameOrderByCalculatedAtDesc`; `CalculatorControllerTest` updated to assert history save on success and no-save on error; new endpoint tests for `GET` and `DELETE /api/history`. |
| III. UX Consistency & Accessibility | ✅ PASS | History panel reuses existing card styles; empty-state message required; "Clear History" button follows existing button patterns; keyboard-operable. |
| IV. Performance Budgets | ✅ PASS | Budgets declared above; H2 indexed query with `Top20` limit is O(1) in practice. |
| V. Definition of Done | ✅ PASS | All 11 acceptance scenarios from spec map to automated or manual validation steps in `quickstart.md`. |

**Post-design re-check**: ✅ Phase 1 design (H2 file-based, `Spring Data JPA`, two new API endpoints, history panel JS) introduces no additional violations.

## Project Structure

### Documentation (this feature)

```text
specs/003-calc-history-persistence/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── openapi.yaml
└── tasks.md             # Phase 2 output (/speckit.tasks command — NOT created by /speckit.plan)
```

### Source Code (repository root)

Additive change to the existing single Maven project. New files shown with `# NEW`; modified files with `# MODIFY`.

```text
src/
├── main/
│   ├── java/com/example/calculator/
│   │   ├── CalculatorApplication.java
│   │   ├── config/
│   │   │   └── SecurityConfig.java
│   │   ├── controller/
│   │   │   └── CalculatorController.java          # MODIFY (add history endpoints + save on calculate)
│   │   ├── dto/
│   │   │   ├── CalculationRequest.java
│   │   │   ├── CalculationResponse.java
│   │   │   └── HistoryEntryResponse.java          # NEW
│   │   ├── model/
│   │   │   └── CalculationHistory.java            # NEW (JPA entity)
│   │   ├── repository/
│   │   │   └── CalculationHistoryRepository.java  # NEW (Spring Data JPA interface)
│   │   └── service/
│   │       ├── CalculatorService.java
│   │       └── Operation.java
│   └── resources/
│       ├── application.properties                 # MODIFY (add datasource + JPA properties)
│       └── static/
│           ├── index.html                         # MODIFY (add history panel + JS)
│           └── login.html
└── test/
    └── java/com/example/calculator/
        ├── config/
        │   └── SecurityConfigTest.java
        ├── controller/
        │   └── CalculatorControllerTest.java      # MODIFY (add history endpoint + save tests)
        ├── repository/
        │   └── CalculationHistoryRepositoryTest.java  # NEW (@DataJpaTest slice)
        └── service/
            └── CalculatorServiceTest.java

pom.xml                                            # MODIFY (add data-jpa + h2 deps)
```

**Structure Decision**: Additive to the existing single-project layout. New packages `model/` and `repository/` introduced following standard Spring Boot layering conventions. `dto/` gains `HistoryEntryResponse` to avoid exposing the JPA entity directly in the API.

## Complexity Tracking

No constitution violations to justify.
