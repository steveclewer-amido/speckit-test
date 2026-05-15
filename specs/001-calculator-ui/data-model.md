# Data Model: Calculator UI

**Feature**: 001-calculator-ui  
**Date**: 2026-05-14

---

## Entities / Data Transfer Objects

This feature is stateless — there are no persisted entities. All data flows through request/response DTOs for a single API call.

---

### CalculationRequest (API input DTO)

Represents the payload sent by the HTML5 UI to `POST /api/calculate`.

| Field | Type | Constraints | Description |
|---|---|---|---|
| `a` | `Double` | `@NotNull` | First operand |
| `b` | `Double` | `@NotNull` | Second operand |
| `operation` | `Operation` (enum) | `@NotNull` | The arithmetic operation to apply |

**Validation rules**:
- `a` and `b` must be present (not null); a missing or non-numeric JSON value triggers a 400 with a descriptive field error.
- `operation` must be one of the valid enum values; an unrecognised string triggers a 400.

---

### Operation (enum)

Enumerates the four supported arithmetic operations.

| Value | Meaning |
|---|---|
| `ADD` | a + b |
| `SUBTRACT` | a − b |
| `MULTIPLY` | a × b |
| `DIVIDE` | a ÷ b (b ≠ 0) |

---

### CalculationResponse (API output DTO)

Returned by `POST /api/calculate`.

| Field | Type | Present when | Description |
|---|---|---|---|
| `result` | `Double` | Success | The computed numeric result |
| `error` | `String` | Error | Human-readable error message |

Exactly one of `result` or `error` is populated in any given response.

---

## State Transitions

The only meaningful state transition is within a single HTTP request/response cycle:

```
[User submits form]
      │
      ▼
POST /api/calculate
      │
      ├─ Validation failure (missing/non-numeric input)
      │       └─ 400 { "error": "..." }
      │
      ├─ Domain error (divide by zero)
      │       └─ 400 { "error": "Division by zero is not allowed." }
      │
      └─ Success
              └─ 200 { "result": <number> }
```

---

## Relationships

None — this is a pure computation service with no entity relationships.
