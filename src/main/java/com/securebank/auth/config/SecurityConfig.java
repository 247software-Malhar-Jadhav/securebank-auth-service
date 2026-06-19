package com.securebank.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 6 configuration for the auth-service REST surface.
 *
 * <p>Key decisions:
 * <ul>
 *   <li><b>Stateless</b>: this service issues JWTs but does not consume them on its own
 *       REST endpoints — register/login/refresh are public by definition. No session,
 *       no CSRF (there is no cookie/session to protect). Token <i>verification</i> is a
 *       gRPC concern, not an HTTP filter here.</li>
 *   <li>The actuator + OpenAPI endpoints stay open so the gateway/k8s probes and docs
 *       work on the internal network. (Services are cluster-internal per spec.)</li>
 *   <li><b>BCryptPasswordEncoder</b> bean is the project's password hasher; injected
 *       wherever hashing/matching is needed.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // No browser sessions / CSRF: pure token-issuing API.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints are intentionally public.
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        // Health/metrics + OpenAPI are open on the internal network.
                        .requestMatchers("/actuator/**", "/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    /** BCrypt with default strength (10). Single source of password hashing. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
