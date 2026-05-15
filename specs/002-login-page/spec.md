# Feature Specification: Login Page

**Feature Branch**: `002-login-page`

**Created**: 2026-05-14

**Status**: Draft

**Input**: User description: "Add a login page to the calculator app. Users must authenticate before accessing the calculator. Credentials are stored in an in-memory database. A custom HTML5 login form should match the existing app's visual style. The app uses Spring Boot with Spring Security. Invalid credentials should show an error message on the login page. A logout option should be available from the calculator page."

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Successful Login (Priority: P1)

A user opens the app and is presented with a login page. They enter their username and password, submit the form, and are taken directly to the calculator. The login page matches the visual style of the existing calculator (card layout, same typography and colour palette).

**Why this priority**: Without authentication the calculator is inaccessible — this is the critical path every user must complete.

**Independent Test**: Navigate to `http://localhost:8080`, confirm the calculator page is not shown. Enter valid credentials and submit. Confirm the calculator loads and the login page is no longer visible.

**Acceptance Scenarios**:

1. **Given** the user is not logged in, **When** they navigate to any page in the app, **Then** they are redirected to the login page.
2. **Given** the login page is displayed, **When** the user enters valid credentials and submits, **Then** they are redirected to the calculator page.
3. **Given** the user is on the login page, **When** the page loads, **Then** its visual style (card layout, fonts, colours, button style) matches the existing calculator page.

---

### User Story 2 — Failed Login (Priority: P2)

A user enters incorrect credentials and receives a clear, non-specific error message on the login page (e.g. "Invalid username or password."). The form remains displayed so they can try again. The error does not reveal whether the username or password was wrong.

**Why this priority**: Invalid credentials are the most common user error; the feedback loop must be clear and secure.

**Independent Test**: Enter an incorrect password for a valid username. Confirm an error message appears on the login page and the calculator is not shown. Confirm the message does not specify which field was incorrect.

**Acceptance Scenarios**:

1. **Given** the login page is displayed, **When** the user submits an incorrect username or password, **Then** an error message "Invalid username or password." is shown on the login page.
2. **Given** an authentication failure, **When** the error message is displayed, **Then** it does not indicate whether the username or the password was incorrect.
3. **Given** the user submits an empty username or empty password, **When** the form is submitted, **Then** a validation message is shown prompting them to fill in the missing field before any authentication attempt is made.

---

### User Story 3 — Logout (Priority: P3)

An authenticated user can log out from the calculator page. After logging out their session is fully terminated. Navigating back to the calculator requires signing in again.

**Why this priority**: Session termination is a baseline security requirement; without it a shared machine exposes the app to unauthorised use.

**Independent Test**: Log in successfully, confirm the calculator is visible. Click the logout control. Confirm the login page is shown. Press the browser back button or navigate directly to `/` and confirm the login page is shown again (not the calculator).

**Acceptance Scenarios**:

1. **Given** the user is logged in and viewing the calculator, **When** they click the logout control, **Then** they are redirected to the login page.
2. **Given** the user has logged out, **When** they navigate directly to the calculator URL, **Then** they are redirected to the login page.
3. **Given** the user has logged out, **When** they press the browser back button, **Then** they see the login page and not the calculator.

---

### Edge Cases

- What happens when a user's browser session expires mid-use? The next page action or API call redirects them to the login page.
- What happens if the user navigates directly to `/api/calculate` without a session? The server returns a `401 Unauthorised` response (not a redirect, since it is a JSON endpoint).
- What happens if the user submits the login form with both fields empty? Both field validation messages are shown simultaneously.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST require a valid authenticated session before any calculator page or API endpoint is accessible.
- **FR-002**: The app MUST provide a dedicated login page with a username field, a password field, and a submit button.
- **FR-003**: The login page MUST match the visual style of the existing calculator page (card layout, same colour palette, typography, and button appearance). Reference implementation: `src/main/resources/static/index.html` is the canonical style source — background `#f0f2f5`, card background `#fff`, border-radius `10px`, primary button colour `#0071e3`, font `system-ui`. The login page MUST use these same values.
- **FR-004**: The app MUST authenticate users against credentials held in an in-memory user store configured at application startup.
- **FR-005**: On successful authentication the user MUST be redirected to the calculator page.
- **FR-006**: On failed authentication the login page MUST display the message "Invalid username or password." without disclosing which credential was wrong.
- **FR-007**: If either credential field is empty on submission, the login page MUST show an inline validation message for the empty field before any authentication attempt.
- **FR-008**: The calculator page MUST display a visible logout control.
- **FR-009**: Activating the logout control MUST terminate the user's session and redirect them to the login page.
- **FR-010**: After logout, any attempt to access a protected page or endpoint MUST redirect the user to the login page.

### Key Entities

- **User Credential**: A username (string) and a password, stored in an in-memory user store. No persistent storage; credentials are configured at application startup and reset on restart.
- **Session**: A server-managed record that an authenticated user exists. Created on successful login; destroyed on logout or expiry.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user with valid credentials can reach the calculator within 3 seconds of submitting the login form under normal conditions.
- **SC-002**: Every route in the application is inaccessible without authentication — no page or data endpoint returns content to an unauthenticated request.
- **SC-003**: An authentication failure always shows exactly the message "Invalid username or password." — never a message that identifies which field was wrong.
- **SC-004**: After logout, 100% of attempts to access a protected page redirect to the login page (including browser back-navigation and direct URL entry).
- **SC-005**: The login page is keyboard navigable and uses appropriate ARIA attributes (consistent with the existing calculator page standard).

---

## Assumptions

- A single pre-configured user account is sufficient; no self-registration, user management, or admin interface is in scope.
- "In-memory user store" means credentials are defined in application configuration at startup and are not persisted to disk. They reset when the application restarts.
- Credentials are defined via `application.properties` so they can be changed without modifying source code.
- Session lifetime follows the application server default (session expires when the browser is closed or after a configurable inactivity period).
- No password reset, "forgot password", or account recovery flow is in scope for this feature.
- The existing calculator REST API (`POST /api/calculate`) is also protected and returns `401 Unauthorised` (JSON) to unauthenticated requests rather than performing an HTML redirect.
- CSRF protection is retained for form submissions (login/logout); the `/api/**` endpoints continue to exempt CSRF tokens as established in the existing implementation.
