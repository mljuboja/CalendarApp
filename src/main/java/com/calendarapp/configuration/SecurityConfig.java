package com.calendarapp.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// Basic Spring Security setup.
// There's no login filter checking JWTs yet, so every endpoint is still open for now.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Not using cookies for auth, so we don't need CSRF protection.
                .csrf(csrf -> csrf.disable())
                // No server-side sessions - each request should carry its own auth info (JWT, later).
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // This is a REST API, so skip the default login page and basic auth popup.
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                // Allow everything for now until login/JWT checking is added.
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    // Used to hash passwords when saving users and to check them on login.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
