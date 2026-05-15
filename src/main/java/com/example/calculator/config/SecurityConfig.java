package com.example.calculator.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Security configuration for the calculator application.
 *
 * <p>Configures in-memory authentication with credentials sourced from application properties,
 * form-based login, logout, CSRF protection via cookie, and a custom entry point that returns
 * a 401 JSON response for unauthenticated API requests.</p>
 *
 * @security Passwords stored with a {bcrypt} delegating-encoder prefix. CSRF tokens are
 *           delivered as an {@code XSRF-TOKEN} cookie (readable by JS) and validated on
 *           all mutating requests except {@code /api/**}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    /**
     * Maps the clean {@code /login} URL to the static {@code login.html} file so Spring
     * Security's {@code loginPage("/login")} works without requiring Thymeleaf.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("forward:/login.html");
    }

    @Value("${app.security.username}")
    private String username;

    @Value("${app.security.password}")
    private String password;

    /**
     * Registers a single in-memory user whose credentials are injected from application
     * properties. The password value must already contain the delegating-encoder prefix
     * (e.g. {@code {bcrypt}} or {@code {noop}}).
     */
    @Bean
    public InMemoryUserDetailsManager userDetailsManager() {
        UserDetails user = User.withUsername(username)
                .password(password)
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    /**
     * Configures the security filter chain:
     * <ul>
     *   <li>{@code /login} and {@code /error} are publicly accessible.</li>
     *   <li>All other routes require authentication.</li>
     *   <li>Form login uses a custom {@code /login} page with redirect to {@code /} on success.</li>
     *   <li>Logout invalidates the session and redirects to {@code /login}.</li>
     *   <li>CSRF tokens are delivered via a cookie; {@code /api/**} is exempt from CSRF.</li>
     *   <li>Unauthenticated {@code /api/**} requests receive a 401 JSON error body.</li>
     *   <li>All other unauthenticated requests are redirected to {@code /login}.</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AntPathRequestMatcher apiMatcher = new AntPathRequestMatcher("/api/**");

        http
            .authorizeHttpRequests(auth -> auth
                // Allow FORWARD and ERROR dispatches (e.g. view controller forward to login.html)
                // without triggering the authentication check — prevents redirect loops.
                .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                .requestMatchers("/login", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            .csrf(csrf -> {
                // Eager handler: always resolves and writes the XSRF-TOKEN cookie so
                // static HTML pages can read it via JS without server-side rendering.
                CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
                handler.setCsrfRequestAttributeName(null);
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(handler)
                    .ignoringRequestMatchers("/api/**");
            })
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    (request, response, authException) -> {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write("{\"error\":\"Authentication required.\"}");
                    },
                    apiMatcher
                )
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new AntPathRequestMatcher("/**")
                )
            );

        return http.build();
    }
}
