# Implementation Plan: Scientific Calculator Mode

**Branch**: `004-scientific-calc-mode` | **Date**: 2026-05-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/004-scientific-calc-mode/spec.md`

## Summary

Extend the existing Spring Boot + HTML5 calculator with six scientific operations (SQRT, POWER, LN, SIN, COS, TAN) and a client-side basic/scientific mode toggle. The `Operation` enum is extended in-place; the existing `POST /api/calculate` endpoint is reused without URL or authentication changes. `b` is made optional in `CalculationRequest` to support unary operations; service-level validation guards binary operations. All scientific results are persisted to `CalculationHistory` identically to basic results. The toggle and scientific controls are rendered in the same card UI with no page reload.

## Technical Context

**Language/Version**: Java 17 (LTS; same as features 001–003)

**Primary Dependencies**: Spring Boot 3.4.x; existing stack: spring-boot-starter-web, spring-boot-starter-validation, spring-boot-starter-security, spring-boot-starter-data-jpa, H2 runtime

**Storage**: H2 file-based database at `jdbc:h2:file:./data/calchistory`; `CalculationHistory.operandB` column annotation changed to `nullable=true` to support unary operations; `ddl-auto=update` applies the schema alteration on next startup

**Testing**: JUnit 5 + MockMvc + `@WithMockUser`; `CalculatorServiceTest` extended for all 6 new operations and 5 domain-error cases; `CalculatorControllerTest` updated for scientific mode payloads and revised `b`-validation behaviour

**Target Platform**: Desktop browser (same as features 001–003)

**Project Type**: Existing single Maven project — additive/minimal-modification change only

**Performance Goals**: `POST /api/calculate` ≤ 200 ms p95 (unchanged; `Math.*` operations are nanosecond-range); mode toggle UI response < 16 ms (synchronous DOM show/hide, no network call); `GET /api/history` ≤ 100 ms p95 (unchanged)

**Constraints**: No Thymeleaf; all API responses are JSON; frontend is static HTML5+JS; CSRF-exempt for `/api/**` (unchanged); unary operations send `b: null` or omit `b`; degree-to-radian conversion server-side for trig functions; `CalculationHistory.operation` is `VARCHAR(20)` — all new enum names ≤ 5 chars; `tan(90°)` must return an error (Java's `Math.tan(Math.PI/2)` does not return `Infinity`)

**Scale/Scope**: 6 files modified (0 new production files); 2 test classes updated; ~100 lines backend, ~80 lines frontend JS + HTML delta

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | ✅ PASS | Extending the existing `Operation` enum is the simplest design. No new abstractions or classes required. Making `b` optional with service-level validation is minimal and cohesive. Dead-code risk: none — the existing `calculate(a, b, op)` signature is extended, not duplicated. |
| II. Testing Standards | ✅ PASS | `CalculatorServiceTest` extended for 6 new operations + 5 domain-error cases (sqrt<0, ln≤0, tan@90°, binary-without-b); `CalculatorControllerTest` updated for scientific payloads and the revised null-b validation path. All new behaviour has automated coverage. |
| III. UX Consistency & Accessibility | ✅ PASS | Mode toggle reuses existing card styles, typography, input fields, button patterns, and colour palette. Second input is hidden (not removed) for unary ops to preserve DOM stability; `aria-pressed` on toggle; `aria-hidden` / `aria-required` updated dynamically. |
| IV. Performance Budgets | ✅ PASS | `Math.*` operations are CPU-trivial (nanosecond range). Mode toggle is synchronous DOM — < 1 ms. No new I/O paths added. |
| V. Definition of Done | ✅ PASS | All 14 acceptance scenarios (US1: 4, US2: 8, edge cases: 2 explicitly in quickstart) map to automated tests or numbered manual steps in `quickstart.md`. |

**Post-design re-check**: ✅ Phase 1 design (enum extension, optional `b`, DOM toggle, `operandB` nullable) introduces no additional violations. `CalculationHistory.operandB` nullable change is backwards-compatible via `ddl-auto=update`.

## Project Structure

### Documentation (this feature)

```text
specs/004-scientific-calc-mode/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── openapi.yaml     # Phase 1 output — full updated API contract (supersedes feature-003 for calculate endpoint)
└── tasks.md             # Phase 2 output (/speckit.tasks command — NOT created by /speckit.plan)
```

### Source Code (repository root)

Minimal-modification change to the existing single Maven project. All changes are to existing files; no new production files required.

```text
src/
├── main/
│   ├── java/com/example/calculator/
│   │   ├── controller/
│   │   │   └── CalculatorController.java          # MODIFY: pass null b for unary ops when saving to history
│   │   ├── dto/
│   │   │   └── CalculationRequest.java            # MODIFY: remove @NotNull from field b
│   │   ├── model/
│   │   │   └── CalculationHistory.java            # MODIFY: operandB column → nullable=true
│   │   └── service/
│   │       ├── CalculatorService.java             # MODIFY: extend calculate() for 6 new operations + unary support
│   │       └── Operation.java                     # MODIFY: add SQRT, POWER, LN, SIN, COS, TAN
│   └── resources/
│       └── static/
│           └── index.html                         # MODIFY: add mode toggle + scientific UI; update formatEntry
└── test/
    └── java/com/example/calculator/
        ├── controller/
        │   └── CalculatorControllerTest.java      # MODIFY: add scientific tests; update b-validation test
        └── service/
            └── CalculatorServiceTest.java         # MODIFY: add 6 operation tests + 5 domain-error tests
```

**Structure Decision**: Single existing Maven project with additive modifications. No new modules, no new packages, no new Spring components needed.

## Key Design Decisions

See [research.md](research.md) for full rationale. Summary:

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Operation extension | Extend existing `Operation` enum in-place | Reuses entire request/response/history pipeline; zero new API surface |
| Unary arity | Remove `@NotNull` from `b` in DTO; service validates binary ops | Minimal DTO change; existing error-handler serves the new validation error |
| Trig units | Degrees (server converts to radians) | Spec-mandated; conversion in service keeps frontend simple |
| tan(90°) | Explicit modulo check before `Math.tan()` | `Math.tan(Math.PI/2)` returns ~1.633e16, not `Infinity`; spec requires an error |
| `operandB` nullability | `nullable=true` on `CalculationHistory.operandB` | Accurate representation; H2 `ddl-auto=update` handles schema alter |
| Mode toggle | `aria-pressed` button; CSS `display` toggling | No framework dependency; synchronous DOM; keyboard-accessible |
| History format | Update `formatEntry` JS with operation-specific templates | Correct notation for unary/scientific entries without API changes |
