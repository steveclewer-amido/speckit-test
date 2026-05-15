package com.example.calculator.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link SecurityConfig}.
 *
 * <p>Covers authentication, authorisation, and API error response behaviour for all three
 * user stories (US1 successful login, US2 failed login, US3 logout). Credentials are
 * isolated via {@link TestPropertySource} so tests are independent of application.properties.</p>
 *
 * @security Uses {@code {noop}} password encoding for test isolation (never in production).
 *           CSRF tokens are supplied via {@link csrf()} where required.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.username=testuser",
        "app.security.password={noop}testpass"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    // ── US1: Successful Login ─────────────────────────────────────────────

    /**
     * Unauthenticated GET / must redirect to /login.
     */
    @Test
    void unauthenticated_getRoot_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    /**
     * The login page must be publicly accessible.
     */
    @Test
    void unauthenticated_getLogin_returns200() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    /**
     * Unauthenticated POST to /api/calculate must return 401 with JSON error body
     * (not a redirect) — the API contract requires machine-readable errors.
     */
    @Test
    void unauthenticated_postApiCalculate_returns401Json() throws Exception {
        mockMvc.perform(post("/api/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":1,\"b\":2,\"operation\":\"ADD\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("Authentication required.")));
    }

    /**
     * Valid credentials submitted via form POST must redirect to / on success.
     */
    @Test
    void validCredentials_postLogin_redirectsToRoot() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "testuser")
                        .param("password", "testpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    // ── US2: Failed Login ─────────────────────────────────────────────────

    /**
     * Invalid password must redirect to /login?error (no details about which field failed).
     */
    @Test
    void invalidPassword_postLogin_redirectsToLoginError() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "testuser")
                        .param("password", "wrongpassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    /**
     * GET /login?error must return 200 so the client-side JS can display the error message.
     */
    @Test
    void getLoginWithError_returns200() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk());
    }

    // ── US3: Logout ───────────────────────────────────────────────────────

    /**
     * Authenticated POST /logout must invalidate the session and redirect to /login.
     */
    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void authenticated_postLogout_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    /**
     * GET / after logout (no session) must redirect to /login.
     */
    @Test
    void afterLogout_getRoot_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
