# Research: Calculator UI

**Feature**: 001-calculator-ui  
**Date**: 2026-05-14  
**Status**: Complete — no NEEDS CLARIFICATION items remain

---

## Decision 1: Spring Boot version

**Decision**: Spring Boot 3.4.x (latest stable as of May 2026)

**Rationale**: 3.4.x is the current Spring Boot production release. It requires Java 17+ (we use Java 21 LTS). It ships with Spring Framework 6.2, Jackson 2.18, and Hibernate Validator 8 — all stable and widely used.

**Alternatives considered**:
- Spring Boot 2.x — EOL; not eligible for new projects.
- Spring Boot 3.3.x — still supported but superseded by 3.4.x.

---

## Decision 2: Java version

**Decision**: Java 21 (LTS)

**Rationale**: Java 21 is the current LTS release and is supported as a first-class target by Spring Boot 3.4.x. Virtual threads (Project Loom) are available if latency headroom is ever needed, though they are not required for this project.

**Alternatives considered**:
- Java 17 — previous LTS, still supported, but 21 is the current recommendation.
- Java 22/23 — non-LTS; not appropriate for a stable project baseline.

---

## Decision 3: Build tool

**Decision**: Maven (pom.xml)

**Rationale**: Maven is the default for Spring Initializr-generated projects and is universally understood. Produces an executable fat JAR via `spring-boot-maven-plugin`. No additional configuration required for a single-module project.

**Alternatives considered**:
- Gradle — equally valid but adds tooling complexity for a simple single-module project.

---

## Decision 4: Frontend approach

**Decision**: Standalone HTML5 page (`src/main/resources/static/index.html`) — vanilla HTML/CSS/JavaScript, no build step

**Rationale**:
- The user explicitly asked for a "standalone HTML5 application".
- Spring Boot auto-serves anything in `src/main/resources/static/` at the context root. No separate web server, reverse proxy, or CORS configuration is needed.
- Vanilla JS is sufficient for two inputs, one select, one button, and one result area. No framework needed.
- Zero frontend build toolchain means fewer dependencies, simpler CI, and no `node_modules`.

**Alternatives considered**:
- React / Vue / Angular — heavyweight for a 4-operation calculator; requires a frontend build step and either separate deployment or Webpack/Vite integration.
- Thymeleaf server-side rendering — valid, but the user asked for HTML5 + a separate backend, implying the UI calls the backend API via fetch (REST), not server-side page rendering.

---

## Decision 5: API design

**Decision**: Single REST endpoint `POST /api/calculate` accepting JSON `{ "a": number, "b": number, "operation": "ADD|SUBTRACT|MULTIPLY|DIVIDE" }` and returning `{ "result": number }` on success or `{ "error": "message" }` on failure.

**Rationale**:
- A single endpoint keeps the surface area minimal for this scope.
- JSON request/response pairs naturally with the Fetch API in the HTML5 frontend.
- Using an enum for `operation` makes validation explicit and type-safe on the backend.

**Alternatives considered**:
- Individual endpoints per operation (`POST /api/add`, etc.) — more discoverable but more boilerplate; a single endpoint is idiomatic for a calculation service.
- Query parameters — less conventional for mutation/computation operations.

---

## Decision 6: Input validation strategy

**Decision**: Bean Validation (`@NotNull`, `@NotBlank`) on the request DTO, plus explicit divide-by-zero check in the service layer.

**Rationale**:
- `spring-boot-starter-validation` is included and handles structural validation (missing/non-numeric values) automatically via `@Valid` on the controller.
- Division by zero is a domain rule (not a structural constraint), so it belongs in the service layer and returns a descriptive error rather than an HTTP 400.

---

## Decision 7: Error response format

**Decision**: Return HTTP 400 with `{ "error": "human-readable message" }` for validation failures and domain errors (divide by zero). HTTP 200 with `{ "result": value }` for success.

**Rationale**: Simple, consistent, and easy for the vanilla-JS frontend to branch on. No need for a full RFC 7807 Problem Details structure at this scope.

---

## Decision 8: CORS

**Decision**: No CORS configuration needed.

**Rationale**: The HTML5 page is served by the same Spring Boot origin on the same port. All `/api/*` requests are same-origin, so no CORS headers are required.

---

## Decision 9: Testing stack

**Decision**: JUnit 5 + `@SpringBootTest` + `MockMvc` for controller tests; plain JUnit 5 for `CalculatorService` unit tests.

**Rationale**:
- `spring-boot-starter-test` bundles JUnit 5, MockMvc, AssertJ, and Mockito — zero additional dependencies.
- Controller tests use `MockMvc` to exercise the full request-processing stack (validation, serialisation, HTTP status) without a running server.
- Service tests are pure unit tests: fast, deterministic, no Spring context.
