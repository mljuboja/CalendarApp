package com.calendarapp.security;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.calendarapp.entity.User;
import com.calendarapp.repository.UserRepository;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Runs once per request, before Spring Security decides whether to let the request
// through. Reads the Authorization header, validates the JWT, and (if everything
// checks out) puts an authenticated Authentication into the SecurityContext.
//
// Any problem with the token (missing header, malformed header, expired, tampered,
// deleted user) just leaves the request unauthenticated - this filter never writes
// an error response itself. The JwtAuthenticationEntryPoint takes care of turning
// "not authenticated" into a consistent 401 response for protected endpoints.
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());
            authenticate(token);
        }

        filterChain.doFilter(request, response);
    }

    // Validates the token and, if it checks out, stores an authenticated
    // Authentication in the SecurityContext. Any JWT problem (expired, malformed,
    // bad signature, unsupported, empty/invalid claims) is caught here so it never
    // turns into a 500 - the request is simply left unauthenticated.
    private void authenticate(String token) {
        try {
            String email = jwtService.extractEmail(token);
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                return;
            }

            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException e) {
            // TEMPORARY DIAGNOSTIC - remove after the 401 bug is found.
            System.out.println("JWT validation failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            // Token missing, malformed, expired, or has a bad signature - leave
            // the request unauthenticated.
        }
    }
}
