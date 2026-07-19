package com.calendarapp.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Phase 3A: security infrastructure only — no endpoints are secured yet.
 *
 * <p>There is no authentication filter, no {@code UserDetailsService}, and JWTs
 * are not enforced anywhere in this chain. All of that belongs to a later phase.
 * This class only wires up the pieces a stateless, token-based REST API will
 * eventually need:
 *
 * <ul>
 *   <li>CSRF is disabled. CSRF protection exists to stop a malicious site from
 *       making a victim's browser "ride along" on an automatically-attached
 *       session cookie. This API never authenticates via cookies — every future
 *       request will carry its own JWT explicitly in an {@code Authorization}
 *       header — so there is no ambient credential for CSRF to protect.</li>
 *   <li>Sessions are stateless. No {@code HttpSession} is created or read; the
 *       server keeps no per-user server-side state between requests.</li>
 *   <li>Form login and HTTP Basic are disabled — neither fits a JSON REST API
 *       that will authenticate via JWT.</li>
 *   <li>All requests are permitted for now. Locking down individual endpoints
 *       is deferred until authentication (login/JWT filter) actually exists.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    /**
     * Not used yet — registration (a later phase) will hash passwords with this
     * before saving {@link com.calendarapp.entity.User#getPasswordHash()}.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
