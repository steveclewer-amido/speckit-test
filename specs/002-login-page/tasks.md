---
description: "Task list for Login Page implementation"
---

# Tasks: Login Page

**Input**: Design documents from `specs/002-login-page/`

**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/auth-flows.md ✅, quickstart.md ✅

**Organization**: Tasks grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no competing dependencies)
- **[Story]**: Which user story this task belongs to (US1 = Successful Login, US2 = Failed Login, US3 = Logout)
- Exact file paths included in all descriptions

---

## Phase 1: Setup (Dependencies & Configuration)

**Purpose**: Add Spring Security to the project and configure credentials before any feature code can be written.

- [X] T001 Add `spring-boot-starter-security` and `spring-security-test` (test scope) dependencies to `pom.xml`
- [X] T002 [P] Add `app.security.username` and `app.security.password` (BCrypt-hashed, `{bcrypt}` prefix) credential properties to `src/main/resources/application.properties`

---

## Phase 2: Foundational (Blocking Prerequisite)

**Purpose**: The `SecurityConfig` class is the single dependency that all three user stories require. It must exist before any story can be implemented or tested end-to-end.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T003 Create `SecurityConfig.java` — `@Configuration @EnableWebSecurity` class in `src/main/java/com/example/calculator/config/SecurityConfig.java` with: `InMemoryUserDetailsManager` bean reading `app.security.username` / `app.security.password` via `@Value` using `PasswordEncoderFactories.createDelegatingPasswordEncoder()`; `SecurityFilterChain` bean configuring: all routes require auth except `/login` and `/error`; form login with `loginPage("/login")` and `defaultSuccessUrl("/", true)`; logout redirecting to `/login`; `CookieCsrfTokenRepository.withHttpOnlyFalse()` for CSRF with `/api/**` exempt; custom `AuthenticationEntryPoint` returning `401` JSON `{"error":"Authentication required."}` for `/api/**` requests and redirecting to `/login` for all other unauthenticated requests
- [X] T004 [P] Add `@WithMockUser` annotation to `CalculatorControllerTest` class-level in `src/test/java/com/example/calculator/controller/CalculatorControllerTest.java` — restores all 16 existing tests to passing after Spring Security is introduced; add `import org.springframework.security.test.context.support.WithMockUser`

**Checkpoint**: Run `mvn test` — all 16 existing tests must still pass before continuing.

---

## Phase 3: User Story 1 — Successful Login (Priority: P1) 🎯 MVP

**Goal**: Unauthenticated users are redirected to the login page. Users with valid credentials can log in and reach the calculator. The login page visually matches the calculator.

**Independent Test**: Start app (`mvn spring-boot:run`), open `http://localhost:8080` in a fresh browser — login page shown. Enter valid credentials, click Log in — calculator page shown.

### Tests for User Story 1 ⚠️

> **Write these tests FIRST — they MUST fail before implementation begins**

- [X] T005 [P] [US1] Create `SecurityConfigTest` using `@SpringBootTest(webEnvironment = MOCK)` and `@AutoConfigureMockMvc` in `src/test/java/com/example/calculator/config/SecurityConfigTest.java`; annotate the class with `@TestPropertySource(properties = {"app.security.username=testuser", "app.security.password={noop}testpass"})` so tests are isolated from `application.properties` values; cover: `GET /` unauthenticated → `302` redirect to `/login`; `GET /login` unauthenticated → `200`; `POST /api/calculate` unauthenticated → `401` with JSON body `{"error":"Authentication required."}`; `POST /login` with `username=testuser` and `password=testpass` → `302` redirect to `/`

### Implementation for User Story 1

- [X] T006 [US1] Create `src/main/resources/static/login.html` — standalone HTML5 login page with: card layout matching the calculator's visual style (same fonts, colours, border-radius, button style); `<form method="POST" action="/login">`; `<input type="text">` for username and `<input type="password">` for password (both with `<label for="...">`, `aria-required="true"`, `aria-describedby` pointing to error spans); submit button labelled "Log in"; JavaScript that reads the `XSRF-TOKEN` cookie and adds a hidden `<input name="_csrf">` field to the form before submission; no error display yet (added in T008)

**Checkpoint**: Run `mvn test` — T005 tests pass. Manually validate quickstart.md AC-1 through AC-4 in a browser.

---

## Phase 4: User Story 2 — Failed Login (Priority: P2)

**Goal**: Users who enter wrong credentials see "Invalid username or password." Users who leave a field empty see an inline validation message. Neither error reveals which field was incorrect.

**Independent Test**: Enter wrong password → error message appears on login page. Leave username empty → inline "Username is required." shown before any server call.

### Tests for User Story 2 ⚠️

> **Write these tests FIRST — they MUST fail before implementation begins**

- [X] T007 [P] [US2] Add failed-authentication test cases to `SecurityConfigTest` in `src/test/java/com/example/calculator/config/SecurityConfigTest.java`: `POST /login` with invalid password → `302` redirect to `/login?error`; `GET /login?error` → `200` (page renders, error message present in response or handled client-side)

### Implementation for User Story 2

- [X] T008 [US2] Add error display and inline field validation to `src/main/resources/static/login.html`: JavaScript reads `window.location.search` for `?error` parameter and shows "Invalid username or password." in a `role="alert" aria-live="polite"` status region; client-side submit handler validates both fields are non-empty before allowing form submission — empty username → "Username is required.", empty password → "Password is required." shown in `role="alert"` spans with `aria-invalid="true"` on the field; if both empty, both errors shown simultaneously

**Checkpoint**: Run `mvn test` — all tests pass. Manually validate quickstart.md AC-5 through AC-8 in a browser.

---

## Phase 5: User Story 3 — Logout (Priority: P3)

**Goal**: An authenticated user can terminate their session from the calculator page. All post-logout navigation returns to the login page.

**Independent Test**: Log in, click Log out — login page shown. Press browser back — login page shown (not calculator).

### Tests for User Story 3 ⚠️

> **Write these tests FIRST — they MUST fail before implementation begins**

- [X] T009 [P] [US3] Add logout test cases to `SecurityConfigTest` in `src/test/java/com/example/calculator/config/SecurityConfigTest.java`: `POST /logout` with active session (use `SecurityMockMvcRequestPostProcessors.csrf()`) → `302` redirect to `/login`; `GET /` after logout → `302` redirect to `/login`

### Implementation for User Story 3

- [X] T010 [US3] Add logout button to `src/main/resources/static/index.html`: positioned top-right of the calculator card; implemented as a `<form method="POST" action="/logout">` with a submit button; JavaScript reads the `XSRF-TOKEN` cookie and injects a hidden `<input name="_csrf">` field before submission; button styled consistently with the existing calculator button (secondary/outline style to distinguish from Calculate)

**Checkpoint**: Run `mvn test` — all tests pass. Manually validate quickstart.md AC-9 through AC-11 in a browser.

---

## Phase 6: Polish & Validation

**Purpose**: Full regression, manual acceptance, and build verification.

- [X] T011 Run `mvn test` and confirm BUILD SUCCESS with all tests passing (16 existing calculator tests + new security tests)
- [X] T012 [P] Manually validate all 11 quickstart.md acceptance scenarios (AC-1 through AC-11) in a browser, including: (a) Section 7 keyboard accessibility checks; (b) observe AC-3 login redirect completes within 3 seconds (SC-001)
- [X] T013 [P] Build executable JAR with `mvn package -DskipTests` and verify `java -jar target/calculator-0.0.1-SNAPSHOT.jar` starts, redirects unauthenticated `/` to login, and allows login with configured credentials

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion — **BLOCKS all user stories**
- **User Story 1 (Phase 3)**: Depends on Foundational phase
- **User Story 2 (Phase 4)**: Depends on Phase 3 (extends SecurityConfigTest, extends login.html)
- **User Story 3 (Phase 5)**: Depends on Phase 3; independent of Phase 4
- **Polish (Phase 6)**: Depends on all user story phases

### Within Each User Story

1. Tests written first (T005, T007, T009) — MUST fail before implementation
2. Implementation follows: SecurityConfig (T003) → login.html (T006) → error state (T008) → logout (T010)

### Parallel Opportunities

Within Phase 1: T002 in parallel with T001 (different files)
Within Phase 2: T003 and T004 in parallel (different files: SecurityConfig.java vs CalculatorControllerTest.java)
Within Phase 3 Tests: T005 is independent (new file)
Phases 4 and 5 test tasks: T007 and T009 extend the same file — sequential; but T007/T009 extend SecurityConfigTest while T008/T010 implement HTML — tests and impl within a phase can overlap on different files

### Story independence

- US1 can be delivered and tested on its own (login redirects, successful auth, API 401)
- US2 adds to login.html and SecurityConfigTest — US1 must be complete first
- US3 adds to index.html and SecurityConfigTest — US1 must be complete; US2 is independent

### Suggested MVP

Implement through Phase 3 only (T001–T006). This delivers the core authentication gate: unauthenticated users cannot access the calculator; valid credentials grant access. Error handling (US2) and logout (US3) can follow as separate increments.
