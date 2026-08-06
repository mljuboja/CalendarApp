package com.calendarapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.calendarapp.configuration.JwtProperties;
import com.calendarapp.dto.LoginRequest;
import com.calendarapp.dto.LoginResponse;
import com.calendarapp.entity.User;
import com.calendarapp.exception.InvalidCredentialsException;
import com.calendarapp.repository.UserRepository;
import com.calendarapp.security.JwtService;

// Tests for AuthenticationService.login(), kept in its own file separate from the
// registration tests. UserRepository is mocked, but we use a real PasswordEncoder
// and a real JwtService/JwtProperties pair so the password check and the JWT claims
// are actually verified, not just stubbed out.
class AuthenticationServiceLoginTest {

    private static final String TEST_SECRET =
            "a3d7f1c9e2b4d6f8a1c3e5b7d9f2a4c6e8b0d3f5a7c9e1b3d5f7a9c1e3b5d7f9";
    private static final long EXPIRATION_MS = 60L * 60 * 1000;
    private static final String RAW_PASSWORD = "correct-password";

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthenticationService authenticationService;
    private String storedHash;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        JwtProperties jwtProperties = new JwtProperties(TEST_SECRET, EXPIRATION_MS);
        jwtService = new JwtService(jwtProperties);
        authenticationService = new AuthenticationService(userRepository, passwordEncoder, jwtService, jwtProperties);
        storedHash = passwordEncoder.encode(RAW_PASSWORD);
    }

    private User existingUser() {
        User user = new User();
        user.setId(7L);
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");
        user.setPasswordHash(storedHash);
        return user;
    }

    @Test
    void successfulLoginReturnsTokenAndUserDetails() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existingUser()));

        LoginResponse response = authenticationService.login(new LoginRequest("jane@example.com", RAW_PASSWORD));

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresInMilliseconds()).isEqualTo(EXPIRATION_MS);
        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getFirstName()).isEqualTo("Jane");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void normalizesEmailBeforeRepositoryLookup() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existingUser()));

        authenticationService.login(new LoginRequest("  Jane@Example.COM  ", RAW_PASSWORD));

        verify(userRepository).findByEmail("jane@example.com");
    }

    @Test
    void unknownEmailThrowsInvalidCredentialsExceptionWithGenericMessage() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(new LoginRequest("missing@example.com", RAW_PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void incorrectPasswordThrowsTheSameInvalidCredentialsExceptionWithGenericMessage() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existingUser()));

        assertThatThrownBy(() -> authenticationService.login(new LoginRequest("jane@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void successfulLoginGeneratesJwtWithCorrectEmailSubjectAndUserIdClaim() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existingUser()));

        LoginResponse response = authenticationService.login(new LoginRequest("jane@example.com", RAW_PASSWORD));

        assertThat(jwtService.isTokenValid(response.getToken())).isTrue();
        assertThat(jwtService.extractEmail(response.getToken())).isEqualTo("jane@example.com");
        assertThat(jwtService.extractUserId(response.getToken())).isEqualTo(7L);
    }
}
