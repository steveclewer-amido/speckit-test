# Quickstart: Scientific Calculator Mode (004)

**Feature**: Scientific Calculator Mode | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

---

## Prerequisites

- JDK 17 installed
- Maven 3.6+ on PATH (or use `./mvnw`)
- The application built and runnable (`mvn spring-boot:run` from repo root)
- An active browser session — log in at `http://localhost:8080/login` with `user` / `password` (or whichever credentials are configured in `SecurityConfig`)

> **Schema note**: `CalculationHistory.operandB` changes from `NOT NULL` to nullable.
> H2 `ddl-auto=update` applies this automatically on next startup.
> If startup fails with a schema error, delete `./data/` and restart — all history will be cleared.

---

## Build & Run

```bash
# From repo root
mvn clean package -q
mvn spring-boot:run
```

Application starts at `http://localhost:8080`.

---

## Run Tests

```bash
mvn test
```

All tests must pass (0 failures, 0 errors). Verify that:
- `CalculatorServiceTest` — covers all 10 operations and domain-error cases
- `CalculatorControllerTest` — covers scientific mode payloads, missing `b` for binary ops, and domain errors via the API

---

## Manual Verification — User Story 1 (Mode Toggle)

**US1-AC1** — Default state on page load:

1. Open `http://localhost:8080` in a browser.
2. Verify the page shows **Basic** as the current mode (toggle is in the "Basic" state).
3. Verify only the basic operation controls are visible (First number, Second number, operation dropdown with ADD/SUBTRACT/MULTIPLY/DIVIDE).
4. Verify the scientific operation controls are **not visible**.

**US1-AC2** — Switch to scientific mode:

1. Click the mode toggle (labelled e.g. "Switch to Scientific").
2. Verify the toggle updates to indicate **Scientific** mode.
3. Verify the scientific operation controls become visible.
4. Verify the basic operation dropdown is replaced or hidden.

**US1-AC3** — Switch back to basic mode:

1. While in scientific mode, click the mode toggle again.
2. Verify the toggle returns to **Basic** mode.
3. Verify scientific controls are hidden.
4. Verify the basic operations are available again and a basic calculation completes successfully.

**US1-AC4** — Visual conformance:

1. Compare the toggle button and scientific controls with the existing card design.
2. Verify matching spacing, font size, input border radius, button style, and colour palette.

---

## Manual Verification — User Story 2 (Scientific Calculations)

Switch to scientific mode before each check below.

**US2-AC1** — Square root:

1. Enter `9` in the first number field.
2. Select **Square Root**.
3. Click **Calculate**.
4. Verify result: `Result: 3`.

**US2-AC2** — Power:

1. Enter base `2`, exponent `8`.
2. Select **Power**.
3. Click **Calculate**.
4. Verify result: `Result: 256`.

**US2-AC3** — Natural log:

1. Enter `1`.
2. Select **Natural Log**.
3. Click **Calculate**.
4. Verify result: `Result: 0`.

**US2-AC4** — Sine:

1. Enter `90`.
2. Select **Sine**.
3. Click **Calculate**.
4. Verify result: `Result: 1`.

**US2-AC5** — Cosine:

1. Enter `0`.
2. Select **Cosine**.
3. Click **Calculate**.
4. Verify result: `Result: 1`.

**US2-AC6** — Tangent:

1. Enter `45`.
2. Select **Tangent**.
3. Click **Calculate**.
4. Verify result: `Result: 1`.

**US2-AC7** — Single input for unary operations:

1. Select **Square Root** (or any trig/ln operation).
2. Verify there is no second input field (or it is hidden/disabled).
3. Enter a value in the first field and calculate successfully.

**US2-AC8** — Invalid input error:

1. Enter `-4`.
2. Select **Square Root**.
3. Click **Calculate**.
4. Verify error message: *"Square root of a negative number is undefined."*
5. Repeat for: `ln(0)` → *"Natural logarithm of zero or a negative number is undefined."*; `tan(90)` → *"tan(90°) is undefined."*

---

## Manual Verification — Edge Cases

**EC-1** — Mode switch clears inputs:

1. Enter `5` in the first number field.
2. Switch to scientific mode.
3. Verify inputs are cleared.

**EC-2** — Mode switch clears result:

1. Perform a basic calculation so a result is displayed.
2. Click the mode toggle.
3. Verify the result display is cleared.

---

## API Verification (curl)

Authenticate and capture session cookie:

```bash
COOKIE=$(curl -si -c - -X POST http://localhost:8080/login \
  -d "username=user&password=password" | grep JSESSIONID | awk '{print $NF}')
```

**Square root**:
```bash
curl -sb "JSESSIONID=$COOKIE" -X POST http://localhost:8080/api/calculate \
  -H "Content-Type: application/json" \
  -d '{"a":9,"operation":"SQRT"}'
# Expected: {"result":3.0}
```

**Power**:
```bash
curl -sb "JSESSIONID=$COOKIE" -X POST http://localhost:8080/api/calculate \
  -H "Content-Type: application/json" \
  -d '{"a":2,"b":8,"operation":"POWER"}'
# Expected: {"result":256.0}
```

**Natural log error (ln(0))**:
```bash
curl -sb "JSESSIONID=$COOKIE" -X POST http://localhost:8080/api/calculate \
  -H "Content-Type: application/json" \
  -d '{"a":0,"operation":"LN"}'
# Expected: {"error":"Natural logarithm of zero or a negative number is undefined."}
```

**Missing b for binary operation**:
```bash
curl -sb "JSESSIONID=$COOKIE" -X POST http://localhost:8080/api/calculate \
  -H "Content-Type: application/json" \
  -d '{"a":10,"operation":"DIVIDE"}'
# Expected: {"error":"Second operand is required for DIVIDE."}
```

**History shows scientific entries**:
```bash
curl -sb "JSESSIONID=$COOKIE" http://localhost:8080/api/history
# Expected: JSON array with scientific entries; operandB is null for unary ops
```

---

## History Panel Verification

After performing at least one scientific calculation:

1. Scroll to the **Calculation History** panel on the calculator page.
2. Verify scientific entries display in the correct format:
   - `√9 = 3` for SQRT
   - `2^8 = 256` for POWER
   - `ln(1) = 0` for LN
   - `sin(90°) = 1` for SIN
   - `cos(0°) = 1` for COS
   - `tan(45°) = 1` for TAN
3. Verify basic calculation entries are still formatted correctly (e.g., `6 + 7 = 13`).

---

## Regression — Basic Mode

Verify **SC-006** (no regressions):

1. Reload the page — confirm basic mode is the default.
2. Perform: `6 + 7` → `13`; `10 ÷ 2` → `5`; `10 ÷ 0` → error "Division by zero is not allowed."
3. Verify history saves and clears correctly.
4. Run `mvn test` — all pre-existing test cases must pass.
