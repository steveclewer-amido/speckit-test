# Data Model: Calculation History with Persistence

**Feature**: 003-calc-history-persistence
**Date**: 2026-05-15

---

## Entities

### CalculationHistory (persisted)

Represents a single saved calculation result associated with an authenticated user.
Maps to the `CALCULATION_HISTORY` table in the H2 file-based database.

| Field | Java Type | Column | Constraints | Notes |
|---|---|---|---|---|
| `id` | `Long` | `ID` | Primary key, auto-generated (`IDENTITY`) | Never exposed in API responses |
| `username` | `String` | `USERNAME` | `NOT NULL`, max 255 chars | Value of `Principal.getName()` at calculation time |
| `operandA` | `Double` | `OPERAND_A` | `NOT NULL` | Left-hand operand of the calculation |
| `operandB` | `Double` | `OPERAND_B` | `NOT NULL` | Right-hand operand of the calculation |
| `operation` | `String` | `OPERATION` | `NOT NULL`, max 20 chars | `Operation` enum name (e.g., `"ADD"`, `"SUBTRACT"`) |
| `result` | `Double` | `RESULT` | `NOT NULL` | Computed result returned to the user |
| `calculatedAt` | `Instant` | `CALCULATED_AT` | `NOT NULL` | Set to `Instant.now()` in the controller at save time |

**JPA annotations**:
```java
@Entity
@Table(name = "CALCULATION_HISTORY",
       indexes = @Index(name = "idx_history_username_at",
                        columnList = "USERNAME, CALCULATED_AT DESC"))
```

**Lifecycle**:
- Created: immediately after a successful calculation in `CalculatorController.calculate()`
- Read: via `GET /api/history` (up to 20 most recent for the current user)
- Deleted: all rows for a user via `DELETE /api/history`
- Never updated after creation

---

### HistoryEntryResponse (API DTO)

Returned by `GET /api/history`. Does not map directly to the JPA entity — excludes `id` and `username` (internal / security-sensitive fields).

| Field | JSON Key | Java Type | Notes |
|---|---|---|---|
| `operandA` | `"operandA"` | `Double` | Left-hand operand |
| `operandB` | `"operandB"` | `Double` | Right-hand operand |
| `operation` | `"operation"` | `String` | Enum name, e.g. `"ADD"` |
| `result` | `"result"` | `Double` | Computed result |
| `calculatedAt` | `"calculatedAt"` | `String` (ISO-8601) | UTC instant, e.g. `"2026-05-15T10:30:00Z"` |

Example response body from `GET /api/history`:
```json
[
  {
    "operandA": 10.0,
    "operandB": 3.0,
    "operation": "DIVIDE",
    "result": 3.3333333333333335,
    "calculatedAt": "2026-05-15T10:32:01.456Z"
  },
  {
    "operandA": 5.0,
    "operandB": 4.0,
    "operation": "ADD",
    "result": 9.0,
    "calculatedAt": "2026-05-15T10:31:45.123Z"
  }
]
```

Empty history:
```json
[]
```

---

## Database Schema

```sql
CREATE TABLE CALCULATION_HISTORY (
    ID            BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    USERNAME      VARCHAR(255)    NOT NULL,
    OPERAND_A     DOUBLE          NOT NULL,
    OPERAND_B     DOUBLE          NOT NULL,
    OPERATION     VARCHAR(20)     NOT NULL,
    RESULT        DOUBLE          NOT NULL,
    CALCULATED_AT TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_history_username_at
    ON CALCULATION_HISTORY (USERNAME, CALCULATED_AT DESC);
```

*Schema managed by Spring Boot's `spring.jpa.hibernate.ddl-auto=update` (dev) or Flyway/Liquibase (if migrated to production).*

---

## Datasource Configuration

| Property | Value | Notes |
|---|---|---|
| `spring.datasource.url` | `jdbc:h2:file:./data/calchistory` | Relative to the working directory (project root when run via Maven) |
| `spring.datasource.driver-class-name` | `org.h2.Driver` | Auto-detected by Spring Boot |
| `spring.h2.console.enabled` | `false` | Disabled in production; can be enabled in a dev profile |
| `spring.jpa.hibernate.ddl-auto` | `update` | Creates/updates schema on startup; safe for H2 file-based dev |
| `spring.jpa.database-platform` | `org.hibernate.dialect.H2Dialect` | Auto-detected; explicit for clarity |

---

## Persistence File Location

```
<working-directory>/
└── data/
    ├── calchistory.mv.db    # H2 main database file
    └── calchistory.trace.db # H2 trace log (auto-created)
```

When run via `mvn spring-boot:run` from the project root, files are created at `./data/calchistory.mv.db` relative to the project root. Add `data/` to `.gitignore`.

---

## State Transitions

```
[Calculation Submitted]
        │
        │  CalculatorService.calculate() — success
        ▼
[CalculationHistory saved]  ──── User calls DELETE /api/history ────▶  [History deleted]
        │
        │  GET /api/history
        ▼
[Returned in response (newest first, max 20)]
```

Failed calculations (division by zero, invalid input) do NOT produce a `CalculationHistory` row; the exception path bypasses the save call entirely.

---

## Impact on existing entities

No changes to `CalculationRequest`, `CalculationResponse`, or `Operation`. These are read-only from this feature's perspective. `CalculatorController` gains a `CalculationHistoryRepository` dependency injection — the only structural change to an existing class.
