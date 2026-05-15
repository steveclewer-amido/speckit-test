# Calculator App

A Spring Boot calculator application with authentication and persistent calculation history, built incrementally using [Spec Kit](https://github.com/speckit) — an AI-assisted feature specification and implementation workflow.

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 17 |
| Framework | Spring Boot 3.4.5 |
| Security | Spring Security 6.4.5 |
| Persistence | Spring Data JPA + H2 (file-based) |
| Frontend | HTML5 / CSS / Vanilla JS (static) |
| Build | Maven 3.9+ |
| Tests | JUnit 5 / Spring Boot Test / MockMvc |

---

## Running the App

**Prerequisites**: Java 17 and Maven 3.9+ on `$PATH`.

```bash
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). Log in with:

- **Username**: `admin`
- **Password**: `password`

To run the test suite (39 tests):

```bash
mvn test
```

---

## What It Does

- **Calculator**: Add, subtract, multiply, and divide two numbers via a clean HTML5 UI.
- **Authentication**: Custom login page backed by Spring Security. Access is denied without valid credentials. A logout link is available from the calculator page.
- **Calculation History**: Every successful calculation is saved to an embedded H2 database. The authenticated user sees their last 20 results (newest first) below the calculator. History persists across app restarts. A "Clear History" button removes all entries for the current user.

---

## How It Was Built — The Spec Kit Workflow

This project was built feature-by-feature using **Spec Kit**, an AI-powered workflow that enforces a specification-first approach before any code is written. Each feature went through the same stages:

```
/speckit.specify  →  /speckit.plan  →  /speckit.tasks  →  /speckit.implement
```

| Stage | Command | Output |
|---|---|---|
| Specify | `/speckit.specify` | `spec.md` — user stories, acceptance criteria, constraints |
| Plan | `/speckit.plan` | `plan.md`, `data-model.md`, `contracts/openapi.yaml` |
| Tasks | `/speckit.tasks` | `tasks.md` — dependency-ordered, TDD-first task list |
| Implement | `/speckit.implement` | Code, tests, and documentation committed together |

An optional analysis pass (`/speckit.analyze`) was run after task generation to check consistency across spec, plan, and tasks before implementation began.

---

## Features Built

### Feature 001 — Calculator UI

**Branch**: `001-build-application-perform`

**Spec input**: *"Build an application that can perform the numerical calculation functions add, subtract, divide and multiply. It needs to be based on the latest stable version of Spring Boot and provide a user interface."*

**What was built**:
- Spring Boot REST API (`POST /api/calculate`) accepting `operandA`, `operandB`, and `operation`
- `CalculatorService` implementing the four operations with division-by-zero guard
- Static HTML5 single-page UI using `fetch()` — no server-side templating
- CSRF cookie handling for secure API calls
- 24 unit and integration tests

---

### Feature 002 — Login Page

**Branch**: `002-login-page`

**Spec input**: *"Add a login page to the calculator app. Users must authenticate before accessing the calculator. Credentials are stored in an in-memory database. A custom HTML5 login form should match the existing app's visual style. The app uses Spring Boot with Spring Security. Invalid credentials should show an error message on the login page. A logout option should be available from the calculator page."*

**What was built**:
- Spring Security configuration (`SecurityConfig`) with `InMemoryUserDetailsManager`
- Custom HTML5 login page matching the existing card layout and colour palette
- Error message shown on invalid credentials
- Logout link on the calculator page
- `401 JSON` response for unauthenticated API calls (used by the calculator JS)
- Security integration tests covering login, logout, and protected routes

---

### Feature 003 — Calculation History with Persistence

**Branch**: `003-calc-history-persistence` (merged into `002-login-page`)

**Spec input**: *"Add calculation history with persistence. After each successful calculation, the result is saved. The authenticated user can see their recent calculation history below the calculator. History is stored in an embedded H2 database and persists across restarts. A clear history option is available."*

**What was built**:
- `CalculationHistory` JPA entity with username index
- `CalculationHistoryRepository` (Spring Data JPA)
- `GET /api/history` — returns top 20 entries for the authenticated user
- `DELETE /api/history` — clears all history for the authenticated user
- History panel in the frontend (reverse-chronological list, empty state, clear button)
- H2 file-based datasource (`./data/calchistory`) — persists across restarts
- 15 new tests (5 repository, 6 controller) — total 39

---

## Project Structure

```
src/
  main/
    java/com/example/calculator/
      CalculatorApplication.java
      config/
        SecurityConfig.java
      controller/
        CalculatorController.java       # REST endpoints
      dto/
        CalculationRequest.java
        CalculationResponse.java
        HistoryEntryResponse.java
      model/
        CalculationHistory.java         # JPA entity
      repository/
        CalculationHistoryRepository.java
      service/
        CalculatorService.java
        Operation.java
    resources/
      application.properties
      static/
        index.html                      # Calculator + history UI
        login.html                      # Custom login page
  test/
    java/com/example/calculator/
      config/SecurityConfigTest.java
      controller/CalculatorControllerTest.java
      repository/CalculationHistoryRepositoryTest.java
      service/CalculatorServiceTest.java
specs/
  001-calculator-ui/                    # spec, plan, data-model, tasks
  002-login-page/
  003-calc-history-persistence/
```

---

## Spec Kit Artefacts

Each feature's specification artefacts are preserved in `specs/<feature>/`:

| File | Purpose |
|---|---|
| `spec.md` | User stories, acceptance criteria, constraints |
| `plan.md` | Architecture decisions, component design |
| `data-model.md` | Entity relationships and field definitions |
| `contracts/openapi.yaml` | REST API contract |
| `tasks.md` | TDD-ordered implementation checklist |
| `quickstart.md` | Step-by-step guide for the feature changes |
| `research.md` | Technology options considered |
| `checklists/requirements.md` | Acceptance checklist |
