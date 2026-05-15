# Research: Scientific Calculator Mode (004)

**Phase**: 0 | **Date**: 2026-05-15 | **Plan**: [plan.md](plan.md)

---

## 1. Operation Extension Strategy

**Decision**: Extend the existing `Operation` enum with six new values — `SQRT`, `POWER`, `LN`, `SIN`, `COS`, `TAN`.

**Rationale**: The existing `POST /api/calculate` endpoint, `CalculatorController`, and `CalculationHistory` persistence all key off `Operation.name()` as a String. Adding to the enum reuses the full request/response/persistence pipeline without introducing a new endpoint, a second DTO hierarchy, or a new controller method. The `CalculationHistory.operation` column is `VARCHAR(20)`; the longest new name is `SUBTRACT` (8 chars, already present) — all six new names are ≤ 5 chars, well within budget.

**Alternatives considered**:
- *New `/api/scientific/calculate` endpoint* — rejected: duplicates authentication wiring, CSRF configuration, history-save logic, and error handlers with no benefit at current scale.
- *Polymorphic `OperationRequest` union type* — rejected: adds a Jackson discriminator + multiple DTO classes for zero functional gain; the spec requires no new endpoints.

---

## 2. Unary Operation Arity (optional `b`)

**Decision**: Remove `@NotNull` from `CalculationRequest.b`; validate that `b != null` for binary operations inside `CalculatorService`, throwing `IllegalArgumentException` if absent.

**Rationale**: Five of six scientific operations (SQRT, LN, SIN, COS, TAN) are unary — they require only one input. The existing DTO marks `b` with `@NotNull`, so unary requests would fail Bean Validation before reaching the service. Making `b` optional at the DTO level is the minimal, cohesive change. Service-level validation for binary ops (ADD, SUBTRACT, MULTIPLY, DIVIDE, POWER) preserves the same user-facing error surface — the `IllegalArgumentException` handler already returns `400 { "error": "..." }`.

**Alternatives considered**:
- *Always send `b=0` from the frontend for unary ops* — rejected: semantically wrong; `b=0` could accidentally trigger division-by-zero errors or produce silently wrong results.
- *Overloaded service method `calculate(double a, Operation op)`* — workable, but requires controller branching logic and does not address the DTO validation issue; rejected in favour of making `b` nullable end-to-end.

---

## 3. Trigonometric Input Unit

**Decision**: Accept input in **degrees**; convert to radians server-side using `Math.toRadians(a)` before calling `Math.sin()`, `Math.cos()`, `Math.tan()`.

**Rationale**: The spec explicitly mandates degrees (FR-004). Conversion in the service layer keeps the frontend free of unit-conversion logic and makes the behaviour consistent regardless of how the API is called.

---

## 4. tan(90°) — Undefined Value Handling

**Decision**: Explicitly detect angles where `tan` is undefined (90° + k·180° for any integer k) and throw `IllegalArgumentException("tan(90°) is undefined.")` before calling `Math.tan()`.

**Rationale**: Java's `Math.tan(Math.PI / 2)` returns approximately `1.633e16` due to IEEE 754 floating-point rounding — it does not return `Infinity`. Without an explicit guard, the server would return a large but finite number rather than an error, violating FR-008. The check `Math.abs(a % 180) == 90.0` works for integer-degree inputs but is fragile for floating-point. A tolerance-based check — `Math.abs(Math.abs(a % 180.0) - 90.0) < 1e-9` — is used to catch both exact and near-exact 90° inputs.

**Alternatives considered**:
- *Frontend-only validation* — rejected: FR-008 requires a server-enforced error; the API must be correct independently of the client.
- *Return `Infinity` as JSON* — rejected: JSON does not support `Infinity`; Jackson serialises it as a literal `Infinity` string which is not valid JSON.

---

## 5. SQRT and LN Domain Validation

**Decision**: Validate input domains in `CalculatorService` and throw `IllegalArgumentException` with descriptive messages:
- `SQRT`: if `a < 0` → `"Square root of a negative number is undefined."`
- `LN`: if `a <= 0` → `"Natural logarithm of zero or a negative number is undefined."`

**Rationale**: `Math.sqrt(-1)` and `Math.log(-1)` both return `NaN`; `Math.log(0)` returns `-Infinity`. None of these are valid JSON double values in the expected sense, and all violate FR-008. Explicit pre-checks produce the user-friendly error messages required by the spec.

---

## 6. `CalculationHistory.operandB` Schema — Nullable Change

**Decision**: Change `CalculationHistory.operandB` from `nullable=false` to `nullable=true`. Store `null` for unary operations; store the actual second operand for binary operations. The H2 `ddl-auto=update` setting will issue `ALTER TABLE CALCULATION_HISTORY MODIFY COLUMN OPERAND_B DOUBLE NULL` on startup.

**Rationale**: Storing a sentinel value (e.g., `0.0`) for unary operations would be semantically misleading and could corrupt history display. Nullable is the accurate representation. H2 supports column modification via `ddl-auto=update`.

**Migration note**: If the existing H2 data file predates this change and the DDL alter fails, the `./data/` directory can be deleted and the database recreated on next startup (all history is lost — acceptable for a development/test environment).

**Alternatives considered**:
- *Store `0.0` as sentinel* — rejected: ambiguous when `0` is a valid operand value; breaks `formatEntry` display logic.
- *Separate history table for scientific calculations* — rejected: over-engineered; duplicates all history management code.

---

## 7. Frontend Mode Toggle — Implementation Pattern

**Decision**: Implement the toggle as a plain `<button>` with `aria-pressed` toggling between `"false"` (basic) and `"true"` (scientific). Toggle handler uses CSS `display` toggling (`style.display = 'none'` / `''`) on the scientific operation fieldset and the second-number field. No third-party libraries.

**Rationale**: The existing frontend has no build pipeline and no JS framework — it is a single static HTML file using vanilla JS. Adding a framework dependency would be disproportionate. The DOM toggle is synchronous (< 1 ms) and satisfies the SC-001 "no page reload" requirement. `aria-pressed` communicates toggle state to screen readers, satisfying Principle III (accessibility).

**Alternatives considered**:
- *CSS `:checked` on a hidden `<input type="checkbox">`* — workable but requires additional CSS rules and is less legible to screen readers without extra ARIA; rejected.
- *Two separate `<form>` elements swapped via JS* — rejected: duplicates form wiring (validation, submit handler, history refresh) unnecessarily.

---

## 8. History Display Format for Scientific Operations

**Decision**: Update the frontend `formatEntry` function to render scientific operations with operation-specific display strings:

| Operation | Display format |
|-----------|---------------|
| `SQRT`    | `√{a} = {result}` |
| `POWER`   | `{a}^{b} = {result}` |
| `LN`      | `ln({a}) = {result}` |
| `SIN`     | `sin({a}°) = {result}` |
| `COS`     | `cos({a}°) = {result}` |
| `TAN`     | `tan({a}°) = {result}` |

**Rationale**: The existing `formatEntry` function uses the pattern `{a} {symbol} {b} = {result}`, which is correct for binary operations. Unary operations have no `b` and use conventional mathematical notation. Updating `formatEntry` with a switch/map ensures history entries are readable without changing the history API response shape.

---

## 9. `CalculationRequest.b` — Validation Contract Change

**Decision**: The removal of `@NotNull` from `b` changes the Bean Validation error from `"Field 'b' must not be null."` to an `IllegalArgumentException`-sourced error for binary operations missing `b`. `CalculatorControllerTest` must be updated to reflect this:
- Remove the test case that POSTs `{ a, operation }` and expects `"Field 'b' must not be null."`.
- Add a test case that POSTs a binary operation with `b: null` and expects the service-level error message `"Second operand is required for <operation>."` via the 400 response.

**Rationale**: The user-observable effect is identical (400 with `{ "error": "..." }`), but the error message changes. Updating the test is required by Principle II (tests must be deterministic and accurate).
