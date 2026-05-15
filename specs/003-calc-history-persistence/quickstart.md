# Quickstart: Calculation History with Persistence

**Feature**: 003-calc-history-persistence
**Date**: 2026-05-15

---

## 1. Prerequisites

- Java 17 installed and on `$PATH` (`java -version`)
- Maven 3.9+ installed (`mvn -version`)
- Features 001 (calculator-ui) and 002 (login-page) fully implemented and tests passing (`mvn test`)

---

## 2. New dependencies added to `pom.xml`

```xml
<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- H2 embedded database (file-based persistence) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## 3. Configure datasource in `application.properties`

Add to `src/main/resources/application.properties`:

```properties
# H2 file-based datasource (persists across restarts)
spring.datasource.url=jdbc:h2:file:./data/calchistory
spring.datasource.driver-class-name=org.h2.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.open-in-view=false

# H2 web console (disabled; enable temporarily for debugging if needed)
spring.h2.console.enabled=false
```

---

## 4. Add `data/` to `.gitignore`

```
# H2 database files
data/
```

---

## 5. Run the tests

```bash
mvn test
```

Expected output: **BUILD SUCCESS** — all existing tests pass; new `CalculationHistoryRepositoryTest` and updated `CalculatorControllerTest` tests pass.

> **Note on H2 in tests**: Tests use an in-memory H2 datasource (`jdbc:h2:mem:testdb`) configured in `src/test/resources/application-test.properties` (or via `@TestPropertySource`), ensuring tests do not read from or write to the development `data/` directory.

---

## 6. Start the application

```bash
mvn spring-boot:run
```

The `data/calchistory.mv.db` file is created automatically on first startup.

---

## 7. Log in

Navigate to [http://localhost:8080](http://localhost:8080). You are redirected to `/login`.

Default credentials (configured in `application.properties`):
| Field | Value |
|---|---|
| Username | `admin` |
| Password | `password` |

---

## 8. Acceptance Scenarios

### Section 8.1 — View Calculation History (User Story 1)

| ID | Scenario | Steps | Expected result |
|---|---|---|---|
| AC-1 | History appears after calculation | Log in. Perform `5 + 3`. | History section below the calculator shows `5 + 3 = 8` as the most recent entry. |
| AC-2 | Multiple calculations — newest first | Perform `2 + 2`, then `10 / 5`. | History shows `10 / 5 = 2` at the top, `2 + 2 = 4` below it. |
| AC-3 | Empty state | Log in with a fresh/cleared account. View the calculator page. | History section shows `"No history yet."` |
| AC-4 | Only 20 entries displayed | Perform 25 calculations. | History section shows exactly 20 entries (the 25 oldest are not shown). |

---

### Section 8.2 — Persistence Across Restarts (User Story 2)

| ID | Scenario | Steps | Expected result |
|---|---|---|---|
| AC-5 | History survives restart | Log in. Perform several calculations. Stop the app (`Ctrl+C`). Restart (`mvn spring-boot:run`). Log in again. | All previous history entries are present, unchanged. |
| AC-6 | User isolation survives restart | Add a second user in `SecurityConfig`. Log in as each user and perform different calculations. Restart. | Each user sees only their own history after login. |

---

### Section 8.3 — Clear History (User Story 3)

| ID | Scenario | Steps | Expected result |
|---|---|---|---|
| AC-7 | Clear history removes all entries | Log in. Perform `3 + 4`. Click "Clear History". | History section shows `"No history yet."` |
| AC-8 | New calculation after clear | Clear history. Perform `1 + 1`. | History section shows only `1 + 1 = 2`. |
| AC-9 | Clear does not affect other users | Log in as user A, clear history. Log in as user B. | User B's history is unchanged. |

---

## 9. Verify API directly (curl)

**Get history** (authenticated session required — obtain cookie from browser dev tools or use the form login via curl):

```bash
# With a valid JSESSIONID cookie value:
curl -s -b "JSESSIONID=<your-session-id>" http://localhost:8080/api/history
```

**Clear history**:

```bash
curl -s -X DELETE -b "JSESSIONID=<your-session-id>" \
     -w "\nHTTP status: %{http_code}\n" \
     http://localhost:8080/api/history
```

Expected: HTTP 204 with empty body.

---

## 10. Database files (reference)

After the first run, inspect the H2 database at the project root:

```
./data/
├── calchistory.mv.db     # Main database file
└── calchistory.trace.db  # Trace log (auto-created; safe to delete)
```

To reset all history during development, stop the application and delete `./data/`.
