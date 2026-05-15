# Implementation Plan: Calculator UI

**Branch**: `001-build-application-perform` | **Date**: 2026-05-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-calculator-ui/spec.md`

## Summary

Build a standalone HTML5 calculator UI backed by a Spring Boot REST API. The user enters two numbers, selects an operation (add, subtract, multiply, divide), and receives a result or a clear validation/error message. All computation happens server-side; the frontend is a single static HTML5 page served directly by Spring Boot with no build toolchain required.

## Technical Context

**Language/Version**: Java 21 (LTS)

**Primary Dependencies**: Spring Boot 3.4.x (latest stable); spring-boot-starter-web; spring-boot-starter-validation; spring-boot-starter-test (JUnit 5, MockMvc)

**Storage**: N/A (stateless; no persistence required for v1)

**Testing**: JUnit 5 + MockMvc (integration/controller tests); pure JUnit 5 unit tests for service layer

**Target Platform**: Desktop browser (Chrome, Firefox, Safari, Edge — current stable)

**Project Type**: Web service + bundled static UI (single Maven project)

**Performance Goals**: p95 API response ≤ 50 ms on localhost; UI response visible to user ≤ 200 ms (spec SC-001)

**Constraints**: No authentication, no persistence, no build step for the frontend (vanilla HTML5/CSS/JS only). Single deployable JAR.

**Scale/Scope**: Single developer, single feature, ~5 source files + 1 static HTML page + ~5 test files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|---|---|---|
| I. Code Quality | ✅ PASS | Simple, minimal design; no unnecessary abstractions |
| II. Testing Standards | ✅ PASS | Unit tests for service; MockMvc integration tests for controller |
| III. UX Consistency & Accessibility | ✅ PASS | ARIA labels, keyboard-operable form, error/empty/result states all covered |
| IV. Performance Budgets | ✅ PASS | p95 ≤ 50 ms API budget declared; validated via MockMvc timing |
| V. Definition of Done | ✅ PASS | All acceptance scenarios mapped to tests; quickstart validates end-to-end |

**Post-design re-check**: ✅ No violations introduced by Phase 1 design (stateless REST + single static page stays well within all gates).

## Project Structure

### Documentation (this feature)

```text
specs/001-calculator-ui/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── openapi.yaml
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

Single Maven project. The Spring Boot app serves the static HTML5 UI from its own classpath.

```text
src/
├── main/
│   ├── java/com/example/calculator/
│   │   ├── CalculatorApplication.java
│   │   ├── controller/
│   │   │   └── CalculatorController.java
│   │   ├── dto/
│   │   │   ├── CalculationRequest.java
│   │   │   └── CalculationResponse.java
│   │   └── service/
│   │       └── CalculatorService.java
│   └── resources/
│       ├── application.properties
│       └── static/
│           └── index.html          # Standalone HTML5 UI (vanilla JS + CSS)
└── test/
    └── java/com/example/calculator/
        ├── controller/
        │   └── CalculatorControllerTest.java
        └── service/
            └── CalculatorServiceTest.java

pom.xml
```

**Structure Decision**: Single project. Spring Boot auto-serves `src/main/resources/static/` at the root URL, so no separate web server or reverse proxy is needed. Keeps deployment to a single executable JAR.
