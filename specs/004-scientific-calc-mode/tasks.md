---
description: "Task list for Scientific Calculator Mode (004)"
---

# Tasks: Scientific Calculator Mode

**Feature**: `004-scientific-calc-mode` | **Branch**: `004-scientific-calc-mode`

**Input**: Design documents from `specs/004-scientific-calc-mode/`

**Prerequisites**: plan.md ✅ | spec.md ✅ | data-model.md ✅ | contracts/openapi.yaml ✅ | quickstart.md ✅

**Approach**: TDD — test tasks (T007, T008) are written first and must fail before implementation begins.

**Scope**: 8 existing files modified; 0 new production files; no new Maven dependencies, modules, or Spring components required.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies between them)
- **[Story]**: Which user story this task belongs to ([US1], [US2])

---

## Phase 1: Setup

**Purpose**: Confirm no new infrastructure is required. This feature is a pure in-place modification of the existing Maven project.

- [X] T001 Confirm no new Maven dependencies required — verify `pom.xml` is unchanged; existing stack (spring-boot-starter-web, spring-boot-starter-validation, spring-boot-starter-security, spring-boot-starter-data-jpa, H2) fully supports all six scientific operations via `java.lang.Math`

**Checkpoint**: Confirmed — all subsequent work proceeds by modifying 8 existing files only

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Two minimal changes that MUST be in place before any user story work begins. The `Operation` enum extension is the critical gate — without it, any code referencing `SQRT`, `POWER`, `LN`, `SIN`, `COS`, or `TAN` will not compile.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T002 [P] Make `operandB` nullable in `src/main/java/com/example/calculator/model/CalculationHistory.java` — change `nullable = false` (or remove the explicit constraint) on the `operandB` `@Column` annotation so unary operation results can be persisted with a null second operand; H2 `ddl-auto=update` applies the schema alteration on next startup
- [X] T003 [P] Extend `Operation` enum with six new values in `src/main/java/com/example/calculator/service/Operation.java` — add `SQRT`, `POWER`, `LN`, `SIN`, `COS`, `TAN` after the existing `DIVIDE` value (all names ≤ 5 chars to satisfy the `VARCHAR(20)` `OPERATION` column constraint)

**Checkpoint**: Foundation ready — new enum values compile, schema accepts null `operandB`; user story phases can begin

---

## Phase 3: User Story 1 — Toggle Calculator Mode (Priority: P1) 🎯 MVP

**Goal**: A clearly labelled mode toggle lets the user switch between basic and scientific modes instantly (no page reload); scientific controls show on activation and hide on deactivation; basic mode is the default on page load; the toggle and controls match the existing card design language.

**Independent Test**: Load `http://localhost:8080`, click the mode toggle, and verify interface transitions per quickstart manual steps US1-AC1 through US1-AC4 (purely client-side DOM behaviour — no automated test tasks for this story).

> **No automated test tasks for US1**: All four acceptance scenarios are verified via manual quickstart steps (US1-AC1 through US1-AC4). There is no API surface or Spring component introduced by the toggle.

### Implementation for User Story 1

- [X] T004 [US1] Add mode toggle button (with `aria-pressed="false"` initial state) and two section wrapper elements (`id="basic-section"` visible by default, `id="scientific-section"` hidden by default) to the calculator card in `src/main/resources/static/index.html`
- [X] T005 [US1] Add scientific operation controls to the `#scientific-section` in `src/main/resources/static/index.html` — operation selector options (SQRT, POWER, LN, SIN, COS, TAN), second-input visibility logic (hide second input for unary operations; show for POWER), and `aria-hidden`/`aria-required` attribute updates when selected operation changes
- [X] T006 [US1] Implement the mode toggle JavaScript event handler in `src/main/resources/static/index.html` — on each activation: toggle `aria-pressed`, show/hide `#basic-section` and `#scientific-section`, clear all input fields, and clear the result display area

**Checkpoint**: US1 complete — toggle visible on load (Basic default), activating shows scientific controls and hides basic, deactivating reverses; visual style matches existing card; verify with quickstart US1-AC1 through US1-AC4

---

## Phase 4: User Story 2 — Perform Scientific Calculations (Priority: P2)

**Goal**: All six scientific operations return mathematically correct results via `POST /api/calculate`; domain-invalid inputs return 400 with descriptive error messages; successful results are persisted to `CalculationHistory`; the frontend wires each scientific button to the API and renders history entries in correct notation.

**Independent Test**: Switch to scientific mode; select each operation; enter required inputs; verify correct result per quickstart US2-AC1 through US2-AC8. Automated coverage: `CalculatorServiceTest` (11 cases) and `CalculatorControllerTest` (scientific payloads + domain errors).

### Tests for User Story 2 ⚠️

> **NOTE: Write these tests FIRST — they MUST FAIL before any of T009–T012 are started**

- [X] T007 [P] [US2] Extend `src/test/java/com/example/calculator/service/CalculatorServiceTest.java` with 11 new test methods: 6 valid-result tests (`sqrt(9)=3`, `pow(2,8)=256`, `ln(1)=0`, `sin(90°)=1`, `cos(0°)=1`, `tan(45°)=1`) and 5 domain-error tests (`sqrt` of negative throws, `ln` of zero throws, `ln` of negative throws, `tan(90°)` throws, `POWER` without `b` throws `IllegalArgumentException`)
- [X] T008 [P] [US2] Update `src/test/java/com/example/calculator/controller/CalculatorControllerTest.java` with: unary request (`SQRT` with no `b` field) returns 200 with correct result; `POWER` request with both `a` and `b` returns 200; domain-error requests (`sqrt<0`, `ln≤0`, `tan(90°)`) each return 400 with the exact `error` message from `data-model.md`; binary op (`DIVIDE`) sent without `b` returns 400

### Implementation for User Story 2

- [X] T009 [US2] Remove `@NotNull` from field `b` in `src/main/java/com/example/calculator/dto/CalculationRequest.java` — make `b` a nullable `Double` with no constraint annotation; keep `@NotNull` on `a` and `operation` unchanged
- [X] T010 [US2] Extend `CalculatorService.calculate()` in `src/main/java/com/example/calculator/service/CalculatorService.java` — update method signature to accept boxed `Double b`; add branches for `SQRT` (`Math.sqrt(a)`, guard `a≥0`), `POWER` (`Math.pow(a,b)`, guard `b!=null`), `LN` (`Math.log(a)`, guard `a>0`), `SIN`/`COS`/`TAN` (`Math.toRadians()` conversion, tan guard `Math.abs(a%180)!=90`); throw `IllegalArgumentException` with the exact error messages specified in `specs/004-scientific-calc-mode/data-model.md` for each domain violation
- [X] T011 [US2] Update `CalculatorController` in `src/main/java/com/example/calculator/controller/CalculatorController.java` to pass `request.getB()` (null for unary ops) when constructing and saving the `CalculationHistory` record, replacing any existing primitive `double b` usage that would fail on null
- [X] T012 [US2] Wire scientific operation buttons to `POST /api/calculate` in `src/main/resources/static/index.html` — each button submits the correct `operation` string and omits `b` for unary ops (`SQRT`, `LN`, `SIN`, `COS`, `TAN`); update `formatEntry` function to render unary notation (`√9 = 3`, `ln(1) = 0`, `sin(90°) = 1`, `cos(0°) = 1`, `tan(45°) = 1`) and binary power notation (`2^8 = 256`)

**Checkpoint**: US2 complete — all 6 operations compute and return correct results, 5 domain errors return 400, results appear in result display and history, `formatEntry` renders correct notation per `data-model.md` section 5

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Full regression and manual acceptance verification.

- [X] T013 [P] Run the full Maven test suite from the repository root (`mvn test`) and confirm 0 failures, 0 errors across all four test classes: `CalculatorServiceTest`, `CalculatorControllerTest`, `SecurityConfigTest`, `CalculationHistoryRepositoryTest`
- [X] T014 Perform manual verification of all quickstart scenarios in `specs/004-scientific-calc-mode/quickstart.md`: US1-AC1 through US1-AC4, US2-AC1 through US2-AC8, and edge cases EC-1 (mode switch clears inputs) and EC-2 (mode switch clears displayed result)

---

## Dependencies

```
T001  (confirmatory; no blockers)

T002 [P] ──┐  (both foundational; run in parallel)
T003 [P] ──┘
  └── T003 blocks: T007, T008, T009, T010, T011, T012
  └── T002 blocks: T011

T004 → T005 → T006        (US1 — sequential; all modify index.html)

T007 [P] ──┐  (TDD: write first, must fail; run in parallel with T008)
T008 [P] ──┘  (both require T003)

T009 → T010 → T011 → T012 (US2 implementation — sequential per dependency chain)
  └── T009 requires T003
  └── T010 requires T009
  └── T011 requires T002, T010
  └── T012 requires T006 (US1 scientific section must exist in DOM)

T013 ── after T012
T014 ── after T013
```

## Parallel Execution Examples

**Foundational phase** (run together):
```
T002: CalculationHistory.java (operandB nullable)
 ║
 ║  (in parallel)
 ║
T003: Operation.java (add SQRT POWER LN SIN COS TAN)
```

**US2 TDD test authoring** (after T003, run together):
```
T007: CalculatorServiceTest (11 new test methods)
 ║
 ║  (in parallel)
 ║
T008: CalculatorControllerTest (scientific + error tests)
```

## Implementation Strategy

**MVP scope** (User Story 1 only — Phases 1–3): Delivers the visible mode toggle with show/hide scientific controls. Independently verifiable with no backend changes.

**Full delivery**: All phases. US2 backend tasks (T009–T011) can be developed in parallel with US1 frontend tasks (T004–T006) since they touch non-overlapping files.

**TDD gate**: T007 and T008 must be committed and confirmed-failing (compilation errors on new enum values are acceptable if T002/T003 are not yet merged) before any of T009–T012 begin.
