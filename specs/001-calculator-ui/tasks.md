---
description: "Task list for Calculator UI implementation"
---

# Tasks: Calculator UI

**Input**: Design documents from `specs/001-calculator-ui/`

**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/openapi.yaml ✅, quickstart.md ✅

**Organization**: Tasks grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no competing dependencies)
- **[Story]**: Which user story this task belongs to (US1 = Calculate a result, US2 = Validation & errors)
- Exact file paths included in all descriptions

---

## Phase 1: Setup (Project Initialization)

**Purpose**: Establish the Maven project skeleton before any feature code can be written.

- [X] T001 Initialize Maven project at repository root with Spring Boot 3.4.x, Java 17, dependencies: spring-boot-starter-web, spring-boot-starter-validation — produces `pom.xml` (follow quickstart.md curl command or Spring Initializr)
- [X] T002 [P] Create main application entry point in `src/main/java/com/example/calculator/CalculatorApplication.java`
- [X] T003 [P] Configure `src/main/resources/application.properties` — set `server.port=8080` and `server.error.whitelabel.enabled=false`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared DTOs and enum that every user story depends on. No story work can begin until this phase is complete.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T004 Create `Operation` enum (`ADD`, `SUBTRACT`, `MULTIPLY`, `DIVIDE`) in `src/main/java/com/example/calculator/service/Operation.java`
- [X] T005 [P] Create `CalculationRequest` DTO with `@NotNull Double a`, `@NotNull Double b`, `@NotNull Operation operation` in `src/main/java/com/example/calculator/dto/CalculationRequest.java`
- [X] T006 [P] Create `CalculationResponse` DTO with `Double result` and `String error` fields (plus static factory methods `success(Double)` and `error(String)`) in `src/main/java/com/example/calculator/dto/CalculationResponse.java`

**Checkpoint**: Foundation ready — user story phases can now begin.

---

## Phase 3: User Story 1 — Calculate a result (Priority: P1) 🎯 MVP

**Goal**: User enters two numbers, chooses an operation, receives the correct numeric result.

**Independent Test**: Start app (`./mvnw spring-boot:run`), open http://localhost:8080, enter `6` and `7`, select Multiply, click Calculate — result `42` appears.

### Tests for User Story 1 ⚠️

> **Write these tests FIRST — they MUST fail before implementation begins**

- [X] T007 [P] [US1] Write `CalculatorServiceTest` covering all four operations: add(2,3)=5, subtract(10,4)=6, multiply(6,7)=42, divide(8,2)=4 in `src/test/java/com/example/calculator/service/CalculatorServiceTest.java`
- [X] T008 [P] [US1] Write `CalculatorControllerTest` with `MockMvc` verifying `POST /api/calculate` returns HTTP 200 and correct `{"result":...}` for all four operations in `src/test/java/com/example/calculator/controller/CalculatorControllerTest.java`

### Implementation for User Story 1

- [X] T009 [US1] Implement `CalculatorService.calculate(Double a, Double b, Operation operation)` returning a `Double` result in `src/main/java/com/example/calculator/service/CalculatorService.java`
- [X] T010 [US1] Implement `CalculatorController` — `@RestController`, `POST /api/calculate`, inject `CalculatorService`, return `CalculationResponse` in `src/main/java/com/example/calculator/controller/CalculatorController.java`
- [X] T011 [US1] Create standalone HTML5 UI with two `<input type="text">` fields (numeric validation enforced client-side before `fetch`), an operation `<select>`, a Calculate `<button>`, and a result/error display area — calls `POST /api/calculate` via `fetch` and renders `response.result`; client-side pre-validation shows "must be a valid number" inline if a field is non-numeric or empty, without making an API call in `src/main/resources/static/index.html`

**Checkpoint**: User Story 1 is fully functional and independently testable. Run `./mvnw test` — all T007/T008 tests pass. Validate quickstart.md scenarios AC-1 through AC-4.

---

## Phase 4: User Story 2 — Input validation and errors (Priority: P2)

**Goal**: User receives clear, actionable feedback for missing inputs, non-numeric values, and division by zero.

**Independent Test**: Submit form with empty first input — validation error appears. Submit `5 ÷ 0` — divide-by-zero error appears. No crash or blank screen in either case.

### Tests for User Story 2 ⚠️

> **Write these tests FIRST — they MUST fail before implementation begins**

- [X] T012 [P] [US2] Add divide-by-zero test case to `CalculatorServiceTest` — verify service throws `IllegalArgumentException` when `b=0` and operation is `DIVIDE` in `src/test/java/com/example/calculator/service/CalculatorServiceTest.java`
- [X] T013 [P] [US2] Add validation error test cases to `CalculatorControllerTest` — verify HTTP 400 with `{"error":"..."}` for: missing `a`, missing `b`, invalid operation string, divide-by-zero, and non-numeric JSON value for `a` or `b` (`HttpMessageNotReadableException` → 400) in `src/test/java/com/example/calculator/controller/CalculatorControllerTest.java`

### Implementation for User Story 2

- [X] T014 [US2] Add divide-by-zero guard in `CalculatorService.calculate` — throw `IllegalArgumentException("Division by zero is not allowed.")` when `operation == DIVIDE && b == 0` in `src/main/java/com/example/calculator/service/CalculatorService.java`
- [X] T015 [US2] Add `@Valid` to request body in `CalculatorController`; add `@ExceptionHandler` methods for `MethodArgumentNotValidException` (field errors → 400 + `{"error":"..."}`), `IllegalArgumentException` (domain errors → 400 + `{"error":"..."}`), and `HttpMessageNotReadableException` (non-numeric JSON value → 400 + `{"error":"Invalid number value."}`) in `src/main/java/com/example/calculator/controller/CalculatorController.java`
- [X] T016 [US2] Add error display state to HTML5 UI — on `response.error` from API show error message in a `role="alert"` region; clear previous result when error is shown in `src/main/resources/static/index.html`

**Checkpoint**: User Stories 1 AND 2 both work independently. Run `./mvnw test` — all tests pass. Validate quickstart.md scenarios AC-5 and AC-6.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Accessibility hardening and final validation across both user stories.

- [X] T017 [P] Add accessibility attributes to `index.html`: `<label for="...">` on all inputs and the select, `aria-label` on the button, `role="alert"` and `aria-live="polite"` on the result/error region in `src/main/resources/static/index.html`
- [X] T018 Run all 6 quickstart.md acceptance scenarios (manual browser validation required) (AC-1 through AC-6) manually in a browser and confirm each passes
- [X] T019 [P] Build executable JAR with `./mvnw package -DskipTests` and verify `java -jar target/calculator-*.jar` starts and serves the UI at http://localhost:8080

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion — **BLOCKS all user stories**
- **User Story 1 (Phase 3)**: Depends on Foundational phase completion
- **User Story 2 (Phase 4)**: Depends on Foundational phase; builds on Phase 3 files (additive changes)
- **Polish (Phase 5)**: Depends on both user story phases

### Within Each User Story

1. Tests written first (T007/T008, T012/T013) — MUST fail before implementation
2. Service before controller (T009 before T010)
3. Controller before UI requires no strict ordering — they are independent files

### Parallel Opportunities

Within Phase 1: T002 and T003 in parallel (different files)  
Within Phase 2: T005 and T006 in parallel (different files); T004 has no dependencies  
Within Phase 3 tests: T007 and T008 in parallel (different files)  
Within Phase 4 tests: T012 and T013 in parallel (different files)  
Within Phase 5: T017 and T019 in parallel (different files)

---

## Parallel Example: User Story 1

```
# Run in parallel:
Task T007: CalculatorServiceTest (all four operations)
Task T008: CalculatorControllerTest (MockMvc, all four operations)

# Then sequentially:
Task T009: implement CalculatorService  (makes T007 pass)
Task T010: implement CalculatorController  (makes T008 pass)
Task T011: implement index.html  (no test dependency; can overlap with T010)
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL)
3. Complete Phase 3: User Story 1 (T007–T011)
4. **STOP and validate**: `./mvnw test` green; quickstart AC-1–AC-4 pass manually
5. Demo: working four-operation calculator at http://localhost:8080

### Incremental Delivery

1. Setup + Foundational → skeleton compiles
2. + User Story 1 → usable calculator (MVP)
3. + User Story 2 → hardened, production-ready
4. + Polish → accessible, fully shippable JAR

---

## Notes

- [P] tasks touch different files — safe to run in parallel
- Tests for each story MUST be written and MUST fail before the matching implementation task begins
- `CalculationResponse` factory methods (`success` / `error`) keep the controller free of null-field branching
- `application.properties` disabling Whitelabel ensures API error responses are clean JSON (not HTML)
- Commit after each checkpoint to keep history clean
