package com.calendarapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.calendarapp.configuration.JwtProperties;
import com.calendarapp.dto.RegistrationRequest;
import com.calendarapp.dto.RegistrationResponse;
import com.calendarapp.entity.User;
import com.calendarapp.exception.DuplicateEmailException;
import com.calendarapp.repository.UserRepository;
import com.calendarapp.security.JwtService;

// Tests for AuthenticationService.register(). UserRepository is mocked, but we use
// the real BCryptPasswordEncoder so the hash assertions are testing real BCrypt output.
// JwtService/JwtProperties are just mocked here since register() doesn't use them -
// login is covered separately in AuthenticationServiceLoginTest.
class AuthenticationServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        authenticationService = new AuthenticationService(
                userRepository, passwordEncoder, mock(JwtService.class), mock(JwtProperties.class));
    }

    @Test
    void registersNewUserAndReturnsResponse() {
        RegistrationRequest request = new RegistrationRequest("Jane", "Doe", "jane@example.com", "secret123");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        RegistrationResponse response = authenticationService.register(request);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getFirstName()).isEqualTo("Jane");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void normalizesEmailByTrimmingAndLowercasingBeforeLookupAndSave() {
        RegistrationRequest request = new RegistrationRequest("Jane", "Doe", "  Jane@Example.COM  ", "secret123");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationResponse response = authenticationService.register(request);

        assertThat(response.getEmail()).isEqualTo("jane@example.com");
        verify(userRepository).findByEmail("jane@example.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void rejectsDuplicateEmailAndNeverSaves() {
        RegistrationRequest request = new RegistrationRequest("Jane", "Doe", "jane@example.com", "secret123");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void duplicateEmailCheckIsAlsoNormalized() {
        RegistrationRequest request = new RegistrationRequest("Jane", "Doe", "  Jane@Example.COM  ", "secret123");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void storesPasswordAsBcryptHashNeverAsPlainText() {
        RegistrationRequest request = new RegistrationRequest("Jane", "Doe", "jane@example.com", "secret123");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        authenticationService.register(request);

        String storedHash = userCaptor.getValue().getPasswordHash();
        assertThat(storedHash).isNotEqualTo("secret123");
        assertThat(storedHash).matches("^\\$2[aby]\\$.{56}$");
        assertThat(passwordEncoder.matches("secret123", storedHash)).isTrue();
    }

    @Test
    void responseNeverExposesPasswordHash() {
        RegistrationRequest request = new RegistrationRequest("Jane", "Doe", "jane@example.com", "secret123");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        RegistrationResponse response = authenticationService.register(request);

        assertThat(response.getUserId()).isNotNull();
        assertThat(response.getEmail()).isEqualTo("jane@example.com");

        // Double-checking RegistrationResponse doesn't even have a field for this.
        assertThat(response.getClass().getDeclaredFields())
                .extracting(Field::getName)
                .doesNotContain("passwordHash", "password");
    }
}
