# Research: Login Page

**Feature**: 002-login-page
**Date**: 2026-05-14
**Status**: Complete — no NEEDS CLARIFICATION items remain

---

## Decision 1: Spring Security version

**Decision**: Use `spring-boot-starter-security` from Spring Boot 3.4.x (Spring Security 6.3.x)

**Rationale**: The project already uses Spring Boot 3.4.x. Adding `spring-boot-starter-security` pulls in a compatible Spring Security 6.x version automatically — no explicit version pinning needed. Spring Security 6.x is the current production-supported release aligned with Spring Framework 6 and Jakarta EE 10.

**Alternatives considered**:
- Spring Security 5.x — requires Spring Boot 2.x, which is EOL. Not applicable.
- Manual JWT / session library — significantly more implementation burden with no benefit at this scale.

---

## Decision 2: Login page delivery (static HTML5, no Thymeleaf)

**Decision**: Serve `login.html` as a static file from `src/main/resources/static/`. JS on the page handles inline validation and reads the CSRF token from the `XSRF-TOKEN` cookie before form submission.

**Rationale**: The existing calculator is a static HTML5 file; introducing Thymeleaf or any server-side templating purely for the login page would add a dependency and build-time concern inconsistent with the project's no-build-toolchain constraint. The only dynamic element the login page needs (CSRF token, error state) can be handled entirely client-side via cookie reading and `URLSearchParams`.

**Alternatives considered**:
- Thymeleaf template — would render the CSRF token server-side and show `?error` messages without JS. Rejected: adds a dependency not used elsewhere; violates the simplicity principle.
- React / SPA framework — completely out of scope for this project.

---

## Decision 3: CSRF token delivery

**Decision**: `CookieCsrfTokenRepository.withHttpOnlyFalse()`. Spring Security writes the CSRF token as the `XSRF-TOKEN` cookie on every response. The login page JavaScript reads this cookie and adds a hidden `_csrf` field to the form before submission. The logout button similarly injects the token. The `/api/**` endpoints retain the existing CSRF exemption (`csrf.ignoringRequestMatchers("/api/**")`).

**Rationale**: This is the standard pattern for Spring Security + single-page / static HTML apps. `withHttpOnlyFalse()` is required so that JavaScript can read the cookie. The cookie is `Secure` in production (HTTPS); on localhost HTTP it is still functional. The `/api/**` exemption avoids breaking the existing `fetch` calls in `index.html`.

**Alternatives considered**:
- Default `HttpSessionCsrfTokenRepository` — requires a Thymeleaf or JSP template to inject `<input type="hidden" name="_csrf">`. Not compatible with static HTML.
- Disable CSRF entirely — violates OWASP CSRF protection requirement and the constitution's security standard.
- GET-based logout — avoids the CSRF issue for logout, but allows CSRF-forced logout. Rejected on security grounds.

---

## Decision 4: Credentials storage and configuration

**Decision**: `InMemoryUserDetailsManager` with a single `UserDetails` entry. Username and BCrypt-hashed password are read from `application.properties` using `@Value`. Password encoder bean is `BCryptPasswordEncoder`.

**Rationale**: Spec FR-004 requires an in-memory user store. Using `application.properties` allows the credential to be changed without modifying source code (spec Assumption). `BCryptPasswordEncoder` is the Spring Security recommended encoder and provides adaptive cost factor; it avoids storing plaintext passwords even in dev config. The deprecated `User.withDefaultPasswordEncoder()` helper is not used.

**application.properties** properties:
```
app.security.username=admin
app.security.password={bcrypt}$2a$12$...
```

The `{bcrypt}` prefix is used with `PasswordEncoderFactories.createDelegatingPasswordEncoder()` so the encoder is determined by the prefix, making future algorithm migration trivial.

**Alternatives considered**:
- `spring.security.user.name` / `spring.security.user.password` auto-config — only works with Spring Boot's auto-configured `UserDetailsService` bean. Once a custom `UserDetailsService` bean is declared, the auto-config backs off and these properties are ignored. Using custom properties avoids this confusion.
- Environment variables — more secure for production secrets but adds operational complexity not needed for this feature scope.

---

## Decision 5: AuthenticationEntryPoint — browser redirect vs. JSON 401

**Decision**: Use a `DelegatingAuthenticationEntryPoint` (or equivalent inline configuration) that:
- For requests matching `/api/**` → returns `401 Unauthorized` with `Content-Type: application/json` and body `{"error":"Authentication required."}`
- For all other requests → redirects to `/login` (Spring's default `LoginUrlAuthenticationEntryPoint`)

**Rationale**: Spec FR-001 requires the API to return `401` (not an HTML redirect) to unauthenticated callers (e.g., a direct `curl` or future client). The calculator's `fetch` calls to `/api/calculate` would currently receive a `302` redirect to `/login` which the browser follows and returns HTML — making the JS error handler receive unexpected HTML. Differentiating by path prefix is the standard Spring Security approach.

**Alternatives considered**:
- Differentiate by `Accept: application/json` header — more correct semantically but less predictable (browser `fetch` sends `Accept: */*` by default). Path-based is simpler and fully deterministic.
- Single entry point that always returns JSON — breaks browser navigation (direct URL visits get JSON instead of a redirect to login).

---

## Decision 6: Spring Security test support

**Decision**: Add `spring-security-test` as a `test`-scoped dependency. Annotate `CalculatorControllerTest` with `@WithMockUser` to restore its existing passing state. New `SecurityConfigTest` uses `MockMvc` with `SecurityMockMvcConfigurer` to test the actual security rules.

**Rationale**: When `spring-boot-starter-security` is on the classpath, `@WebMvcTest` slices load Spring Security auto-configuration. Without `@WithMockUser`, all existing controller tests will fail with `401`. `spring-security-test` provides `@WithMockUser`, `SecurityMockMvcRequestPostProcessors`, and `SecurityMockMvcConfigurers` needed to write these tests correctly. `spring-security-test` is not pulled in transitively by `spring-boot-starter-test` — it must be declared explicitly.

**Alternatives considered**:
- Disable security in test profile — hides real behaviour; tests would not catch security regressions.
- `@SpringBootTest` for all security tests — loads full application context; much slower than `@WebMvcTest`. Only `SecurityConfigTest` uses `@SpringBootTest` for the integration-level auth tests.

---

## Decision 7: Session management

**Decision**: Use Spring Security's default session management (server-side `HttpSession`). No explicit `sessionManagement()` configuration needed. The browser's session cookie (`JSESSIONID`) identifies the authenticated session.

**Rationale**: For a single-user local app, the default session behaviour is fully sufficient. Session fixation protection is enabled by default in Spring Security 6. No custom timeout configuration is added (browser-session lifetime by default, per the spec assumption).

**Alternatives considered**:
- JWT / stateless tokens — overkill for a single-user local app; would require token storage, refresh logic, and more complex frontend code.
- Explicit `sessionCreationPolicy(ALWAYS)` — unnecessary; Spring Security creates a session on login by default.
