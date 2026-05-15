# Implementation Plan: Login Page

**Branch**: `002-login-page` | **Date**: 2026-05-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-login-page/spec.md`

## Summary

Add Spring Security–based authentication to the existing Spring Boot calculator app. An unauthenticated user visiting any page is redirected to a custom HTML5 login page that matches the calculator's visual style. Credentials are held in an `InMemoryUserDetailsManager` configured from `application.properties`. Successful login redirects to the calculator; failed login shows "Invalid username or password." on the login page. The calculator page gains a logout button that terminates the session. The existing `/api/calculate` endpoint is also protected and returns `401` JSON (not an HTML redirect) to unauthenticated API callers.

## Technical Context

**Language/Version**: Java 17 (LTS; installed JDK on this machine)

**Primary Dependencies**: Spring Boot 3.4.x; `spring-boot-starter-security`; `spring-security-test` (test scope); existing: spring-boot-starter-web, spring-boot-starter-validation, spring-boot-starter-test

**Storage**: N/A — credentials held in `InMemoryUserDetailsManager`; passwords BCrypt-hashed at startup; no database or file persistence

**Testing**: JUnit 5 + MockMvc + `@WithMockUser` (spring-security-test); new `SecurityConfigTest` for auth/authz behaviour; existing `CalculatorControllerTest` updated with `@WithMockUser`

**Target Platform**: Desktop browser (same as feature 001)

**Project Type**: Existing single Maven project — additive change only

**Performance Goals**: Spring Security filter chain overhead ≤ 5 ms per request (negligible vs. existing p95 ≤ 50 ms API budget); login page load ≤ 200 ms

**Constraints**: No Thymeleaf or server-side templating — login page is a static HTML5 file; CSRF token delivered via `CookieCsrfTokenRepository.withHttpOnlyFalse()` so JS can read and submit it; `/api/**` endpoints remain CSRF-exempt and return JSON errors on auth failure

**Scale/Scope**: Single user account; additive change to existing project (~3 new files, 2 modified files, 1 new test class, 1 modified test class)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|---|---|---|
| I. Code Quality | ✅ PASS | Minimal new surface: 1 config class, 1 new HTML page, 1 modified HTML page. No unnecessary abstractions. |
| II. Testing Standards | ✅ PASS | New `SecurityConfigTest` covers auth/authz behaviour; `CalculatorControllerTest` updated with `@WithMockUser`; regression tests for all existing endpoints. |
| III. UX Consistency & Accessibility | ✅ PASS | Login page reuses calculator card styles; ARIA labels, keyboard-operable form, error/empty/loading states required by constitution. |
| IV. Performance Budgets | ✅ PASS | Filter-chain overhead declared (≤ 5 ms); login page load budget ≤ 200 ms. Validated via MockMvc. |
| V. Definition of Done | ✅ PASS | All 9 acceptance scenarios from spec map to tests or manual validation steps. |

**Post-design re-check**: ✅ Phase 1 design (static HTML5 + `CookieCsrfTokenRepository` + custom `AuthenticationEntryPoint`) introduces no additional violations.

## Project Structure

### Documentation (this feature)

```text
specs/002-login-page/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── auth-flows.md
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
│   │   │   └── SecurityConfig.java              # NEW
│   │   ├── controller/
│   │   │   └── CalculatorController.java
│   │   ├── dto/
│   │   │   ├── CalculationRequest.java
│   │   │   └── CalculationResponse.java
│   │   └── service/
│   │       ├── CalculatorService.java
│   │       └── Operation.java
│   └── resources/
│       ├── application.properties               # MODIFY (add credential properties)
│       └── static/
│           ├── index.html                       # MODIFY (add logout button)
│           └── login.html                       # NEW
└── test/
    └── java/com/example/calculator/
        ├── config/
        │   └── SecurityConfigTest.java          # NEW
        ├── controller/
        │   └── CalculatorControllerTest.java    # MODIFY (add @WithMockUser)
        └── service/
            └── CalculatorServiceTest.java

pom.xml                                          # MODIFY (add security + security-test deps)
```

**Structure Decision**: Additive to the existing single-project layout. A `config/` package is introduced for `SecurityConfig` — consistent with Spring Boot convention and keeps security concerns separate from controllers and services.

## Complexity Tracking

No constitution violations to justify.
