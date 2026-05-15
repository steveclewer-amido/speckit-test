# Data Model: Login Page

**Feature**: 002-login-page
**Date**: 2026-05-14

---

## Entities

### UserCredential (in-memory, not persisted)

Represents a single authenticated user account. Stored in Spring Security's `InMemoryUserDetailsManager` at application startup. Not stored in any database or file.

| Field | Type | Constraints | Source |
|---|---|---|---|
| `username` | `String` | Non-null, non-empty | `application.properties` → `app.security.username` |
| `password` | `String` (BCrypt hash) | Non-null; `{bcrypt}` prefix for delegating encoder | `application.properties` → `app.security.password` |
| `roles` | `String[]` | Fixed `["USER"]` at startup | Hardcoded in `SecurityConfig` |
| `enabled` | `boolean` | `true` | Hardcoded in `SecurityConfig` |

**Lifecycle**: Created once at application startup. Lives in JVM heap. Reset on application restart.

---

### Session

Represents an authenticated HTTP session. Managed entirely by Spring Security and the servlet container. Not directly modelled in application code.

| Field | Type | Notes |
|---|---|---|
| `JSESSIONID` | Cookie (browser) | Set by Spring after successful authentication |
| `XSRF-TOKEN` | Cookie (browser) | CSRF token; `httpOnly=false` so JS can read it |
| Lifetime | Browser session | Expires when browser is closed (no explicit `maxAge` configured) |

**State transitions**:

```
[Unauthenticated]
      │
      │  POST /login (valid credentials)
      ▼
[Authenticated]  ──── POST /logout ────▶  [Unauthenticated]
      │
      │  Browser closed / server restart
      ▼
[Expired / Gone]
```

---

## No database schema

This feature introduces **no database tables, schema migrations, or persistent storage**. All credential and session data exists in-memory only.

---

## Impact on existing entities

No changes to `CalculationRequest`, `CalculationResponse`, or `Operation`. The existing data model is untouched.
