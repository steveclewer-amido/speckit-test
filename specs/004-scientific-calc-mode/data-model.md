# Data Model: Scientific Calculator Mode (004)

**Phase**: 1 | **Date**: 2026-05-15 | **Plan**: [plan.md](plan.md)

---

## Overview

Feature 004 adds no new entities. All changes are modifications to existing types:
- `Operation` enum — extended with six new values
- `CalculationRequest` DTO — `b` relaxed from required to optional
- `CalculationHistory` entity — `operandB` column becomes nullable

---

## 1. `Operation` Enum (modified)

**File**: `src/main/java/com/example/calculator/service/Operation.java`

| Value      | Arity  | Valid domain of `a`         | Valid domain of `b`    |
|------------|--------|-----------------------------|------------------------|
| `ADD`      | binary | any finite double           | any finite double      |
| `SUBTRACT` | binary | any finite double           | any finite double      |
| `MULTIPLY` | binary | any finite double           | any finite double      |
| `DIVIDE`   | binary | any finite double           | any finite double ≠ 0  |
| `SQRT`     | unary  | `a ≥ 0`                     | not used               |
| `POWER`    | binary | any finite double           | any finite double      |
| `LN`       | unary  | `a > 0`                     | not used               |
| `SIN`      | unary  | any finite double (degrees) | not used               |
| `COS`      | unary  | any finite double (degrees) | not used               |
| `TAN`      | unary  | degrees where `a % 180 ≠ 90`| not used               |

**Arity classification** drives:
- Whether the frontend shows/requires the second input field
- Whether `CalculatorService` validates `b != null` before operating
- How `formatEntry` in the frontend renders history entries

---

## 2. `CalculationRequest` DTO (modified)

**File**: `src/main/java/com/example/calculator/dto/CalculationRequest.java`

| Field       | Type     | Constraint (before) | Constraint (after)       | Notes |
|-------------|----------|---------------------|--------------------------|-------|
| `a`         | `Double` | `@NotNull`          | `@NotNull` (unchanged)   | Always required |
| `b`         | `Double` | `@NotNull`          | no constraint (nullable) | Required for binary operations; null for unary |
| `operation` | `Operation` | `@NotNull`       | `@NotNull` (unchanged)   | Must be a valid enum value |

**Validation flow**:
1. Bean Validation rejects null `a` or null `operation` with 400 before the service is called.
2. `CalculatorService.calculate()` checks `b != null` for all binary operations and throws `IllegalArgumentException("Second operand is required for [operation].")` if absent.
3. The `@ExceptionHandler(IllegalArgumentException.class)` in `CalculatorController` returns 400 `{ "error": "..." }`.

---

## 3. `CalculatorService` — Operation Logic (modified)

**File**: `src/main/java/com/example/calculator/service/CalculatorService.java`

Updated `calculate(double a, Double b, Operation operation)` signature (note `b` is boxed `Double`):

| Operation  | Implementation                               | Pre-condition check |
|------------|----------------------------------------------|---------------------|
| `ADD`      | `a + b`                                      | `b != null`         |
| `SUBTRACT` | `a - b`                                      | `b != null`         |
| `MULTIPLY` | `a * b`                                      | `b != null`         |
| `DIVIDE`   | `a / b` (guard: b ≠ 0)                       | `b != null`, `b != 0` |
| `SQRT`     | `Math.sqrt(a)`                               | `a >= 0`            |
| `POWER`    | `Math.pow(a, b)`                             | `b != null`         |
| `LN`       | `Math.log(a)`                                | `a > 0`             |
| `SIN`      | `Math.sin(Math.toRadians(a))`                | none                |
| `COS`      | `Math.cos(Math.toRadians(a))`                | none                |
| `TAN`      | `Math.tan(Math.toRadians(a))` (guard: a%180≠90) | `abs(a%180) ≠ 90` |

**Error messages** (thrown as `IllegalArgumentException`, returned as 400 `{ "error": "..." }`):

| Condition | Message |
|-----------|---------|
| Binary op, `b` is null | `"Second operand is required for [OPERATION]."` |
| `DIVIDE`, `b == 0` | `"Division by zero is not allowed."` (unchanged) |
| `SQRT`, `a < 0` | `"Square root of a negative number is undefined."` |
| `LN`, `a <= 0` | `"Natural logarithm of zero or a negative number is undefined."` |
| `TAN`, `a % 180 == 90°` | `"tan(90°) is undefined."` |

---

## 4. `CalculationHistory` Entity (modified)

**File**: `src/main/java/com/example/calculator/model/CalculationHistory.java`

| Column          | Type            | Before       | After        | Notes |
|-----------------|-----------------|--------------|--------------|-------|
| `ID`            | `BIGINT` PK     | unchanged    | unchanged    |       |
| `USERNAME`      | `VARCHAR`       | NOT NULL     | NOT NULL     | unchanged |
| `OPERAND_A`     | `DOUBLE`        | NOT NULL     | NOT NULL     | unchanged |
| `OPERAND_B`     | `DOUBLE`        | **NOT NULL** | **NULL OK**  | null for unary operations |
| `OPERATION`     | `VARCHAR(20)`   | NOT NULL     | NOT NULL     | new values: SQRT, POWER, LN, SIN, COS, TAN (all ≤ 5 chars) |
| `RESULT`        | `DOUBLE`        | NOT NULL     | NOT NULL     | unchanged |
| `CALCULATED_AT` | `TIMESTAMP`     | NOT NULL     | NOT NULL     | unchanged |

**Schema migration**: H2 `ddl-auto=update` will alter `OPERAND_B` to remove `NOT NULL` on next startup. If the alter fails on an existing data file, delete `./data/` and restart.

---

## 5. History Display — Frontend `formatEntry` Update

**File**: `src/main/resources/static/index.html` (inline `<script>`)

The existing `formatEntry` function renders `{a} {symbol} {b} = {result}` for all operations. It must be updated to handle unary operations and the new `POWER` binary format.

| Operation | Display example       | Notes |
|-----------|-----------------------|-------|
| `ADD`     | `6 + 7 = 13`          | unchanged |
| `SUBTRACT`| `10 − 3 = 7`          | unchanged |
| `MULTIPLY`| `4 × 5 = 20`          | unchanged |
| `DIVIDE`  | `10 ÷ 2 = 5`          | unchanged |
| `SQRT`    | `√9 = 3`              | unary; `b` is null |
| `POWER`   | `2^8 = 256`           | binary; uses `^` symbol |
| `LN`      | `ln(1) = 0`           | unary; `b` is null |
| `SIN`     | `sin(90°) = 1`        | unary; degree symbol appended |
| `COS`     | `cos(0°) = 1`         | unary; degree symbol appended |
| `TAN`     | `tan(45°) = 1`        | unary; degree symbol appended |

---

## 6. No New Entities

Feature 004 introduces no new tables, repositories, DTOs, or service classes. The change set is entirely modifications to existing types.
