# Auth Flow Contracts

**Feature**: 002-login-page
**Date**: 2026-05-14

These contracts define the HTTP interface for all authentication-related interactions. They supplement the existing [openapi.yaml](../../001-calculator-ui/contracts/openapi.yaml) for `/api/calculate`.

---

## GET /login

Serves the login page.

### Request

```
GET /login HTTP/1.1
Host: localhost:8080
```

### Responses

| Condition | Status | Body | Notes |
|---|---|---|---|
| User not authenticated | `200 OK` | `login.html` (HTML) | Sets `XSRF-TOKEN` cookie on first response |
| User already authenticated | `302 Found` | — | Redirects to `/` |

### Side-effects

- Spring Security sets (or refreshes) the `XSRF-TOKEN` cookie in the response. The cookie is `HttpOnly=false` so that page JavaScript can read it.

---

## POST /login

Processes the login form submission. Handled entirely by Spring Security's `UsernamePasswordAuthenticationFilter`.

### Request

```
POST /login HTTP/1.1
Host: localhost:8080
Content-Type: application/x-www-form-urlencoded

username=admin&password=secret&_csrf=<token>
```

| Parameter | Required | Description |
|---|---|---|
| `username` | Yes | The account username |
| `password` | Yes | The account password (plaintext in transit; HTTPS in production) |
| `_csrf` | Yes | CSRF token read from `XSRF-TOKEN` cookie by page JavaScript |

### Responses

| Condition | Status | Location header | Notes |
|---|---|---|---|
| Valid credentials | `302 Found` | `/` | Session cookie (`JSESSIONID`) set |
| Invalid credentials | `302 Found` | `/login?error` | No session created |
| Missing CSRF token | `403 Forbidden` | — | CSRF protection rejects request |

---

## POST /logout

Terminates the authenticated session. Handled by Spring Security's `LogoutFilter`.

### Request

```
POST /logout HTTP/1.1
Host: localhost:8080
Content-Type: application/x-www-form-urlencoded
Cookie: JSESSIONID=<session>; XSRF-TOKEN=<token>

_csrf=<token>
```

| Parameter | Required | Description |
|---|---|---|
| `_csrf` | Yes | CSRF token from `XSRF-TOKEN` cookie, injected by logout button JS |

### Responses

| Condition | Status | Location header | Notes |
|---|---|---|---|
| Authenticated session | `302 Found` | `/login` | Session invalidated; `JSESSIONID` cookie cleared |
| No active session | `302 Found` | `/login` | Treated as a no-op |
| Missing CSRF token | `403 Forbidden` | — | CSRF protection rejects request |

---

## GET / (calculator page — protected)

### Responses

| Condition | Status | Body | Notes |
|---|---|---|---|
| Authenticated | `200 OK` | `index.html` | Calculator page, includes logout button |
| Unauthenticated | `302 Found` → `/login` | — | Spring Security intercepts |

---

## POST /api/calculate (protected — updated behaviour)

The API endpoint now requires authentication. Error response for unauthenticated callers is JSON (not an HTML redirect).

### Responses — auth-related changes only

| Condition | Status | Body |
|---|---|---|
| Unauthenticated request | `401 Unauthorized` | `{"error": "Authentication required."}` |
| Authenticated — existing behaviour | `200 OK` / `400 Bad Request` | Unchanged (see `openapi.yaml`) |

---

## CSRF token flow (summary)

```
Browser                         Spring Security
  │                                    │
  │  GET /login                        │
  │──────────────────────────────────▶ │
  │                                    │  Set-Cookie: XSRF-TOKEN=abc123; Path=/
  │ ◀────────────────────────────────  │
  │                                    │
  │  JS reads document.cookie → "abc123"
  │                                    │
  │  POST /login  _csrf=abc123         │
  │──────────────────────────────────▶ │
  │                                    │  Validates _csrf matches session CSRF token
  │  302 → /  Set-Cookie: JSESSIONID   │
  │ ◀────────────────────────────────  │
```
