# Quickstart: Login Page

**Feature**: 002-login-page
**Date**: 2026-05-14

---

## 1. Prerequisites

- Java 17 installed and on `$PATH` (`java -version`)
- Maven 3.9+ installed (`mvn -version`)
- Feature 001 (calculator-ui) fully implemented and tests passing (`mvn test`)

---

## 2. New dependencies added to `pom.xml`

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Spring Security test support -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 3. Configure credentials in `application.properties`

Add to `src/main/resources/application.properties`:

```properties
app.security.username=admin
app.security.password={bcrypt}$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
```

> The hash above encodes the password `password`. **Change this before any non-local deployment.**
> Generate a new hash: `htpasswd -bnBC 12 "" yourpassword | tr -d ':\n' | sed 's/$2y/$2a/'`
> or use Spring's `BCryptPasswordEncoder.encode("yourpassword")` in a scratch test.

---

## 4. Run the tests

```bash
mvn test
```

Expected output: **BUILD SUCCESS** — all existing tests continue to pass (with `@WithMockUser` added to `CalculatorControllerTest`). New `SecurityConfigTest` tests pass.

---

## 5. Start the application

```bash
mvn spring-boot:run
```

---

## 6. Acceptance Scenarios

Validate each scenario manually in a browser at `http://localhost:8080`.

### Section 6.1 — Authentication Gate

| ID | Scenario | Steps | Expected result |
|---|---|---|---|
| AC-1 | Unauthenticated access blocked | Open a fresh browser / incognito window. Navigate to `http://localhost:8080` | Redirected to `http://localhost:8080/login`. Calculator not visible. |
| AC-2 | Login page style match | View the login page | Card layout, fonts, button colour, and spacing match the calculator page. |

### Section 6.2 — Successful Login

| ID | Scenario | Steps | Expected result |
|---|---|---|---|
| AC-3 | Valid credentials | On the login page, enter `admin` / `password`. Click **Log in**. | Redirected to `http://localhost:8080/`. Calculator is visible and functional. Redirect and page load MUST complete within 3 seconds (SC-001). |
| AC-4 | Direct URL after login | While authenticated, navigate to `http://localhost:8080/login` | Redirected to `/` (already authenticated). |

### Section 6.3 — Failed Login

| ID | Scenario | Steps | Expected result |
|---|---|---|---|
| AC-5 | Wrong password | Enter `admin` and an incorrect password. Click **Log in**. | Login page shown again with message: **"Invalid username or password."** |
| AC-6 | Empty username | Leave username blank, enter any password. Click **Log in**. | Inline validation message: **"Username is required."** No API call made. |
| AC-7 | Empty password | Enter a valid username, leave password blank. Click **Log in**. | Inline validation message: **"Password is required."** No API call made. |
| AC-8 | Error message does not reveal which field | Try AC-5 above | Error message is "Invalid username or password." — does not say "wrong password" or "user not found". |

### Section 6.4 — Logout

| ID | Scenario | Steps | Expected result |
|---|---|---|---|
| AC-9 | Logout from calculator | Log in (AC-3), then click **Log out** on the calculator page. | Redirected to login page. Session terminated. |
| AC-10 | Back-navigation after logout | After AC-9, press the browser Back button. | Login page shown — not the calculator. |
| AC-11 | Direct URL after logout | After AC-9, navigate directly to `http://localhost:8080` | Redirected to `/login`. |

---

## 7. Keyboard accessibility validation

| Check | Action |
|---|---|
| Tab through login form | All fields and the submit button are reachable by `Tab` key in document order |
| Enter submits form | Pressing `Enter` in any field submits the login form |
| Error announced | Submit an empty form; confirm the inline error is announced by a screen reader (check `role="alert"` present on error spans) |
| Logout reachable | Tab to the logout button on the calculator page and activate with `Enter` or `Space` |

## 9. API behaviour when unauthenticated

```bash
# Without a session — should return 401 JSON
curl -s -X POST http://localhost:8080/api/calculate \
  -H 'Content-Type: application/json' \
  -d '{"a":6,"b":7,"operation":"MULTIPLY"}' | jq .
# Expected: {"error":"Authentication required."}
```
