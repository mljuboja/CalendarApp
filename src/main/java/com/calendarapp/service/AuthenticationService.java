package com.calendarapp.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.calendarapp.configuration.JwtProperties;
import com.calendarapp.dto.LoginRequest;
import com.calendarapp.dto.LoginResponse;
import com.calendarapp.dto.RegistrationRequest;
import com.calendarapp.dto.RegistrationResponse;
import com.calendarapp.entity.User;
import com.calendarapp.exception.DuplicateEmailException;
import com.calendarapp.exception.InvalidCredentialsException;
import com.calendarapp.repository.UserRepository;
import com.calendarapp.security.JwtService;

// Handles registering new users and logging existing ones in. Login verifies
// the password and hands back a JWT; JwtAuthenticationFilter checks that token
// on later requests.
@Service
public class AuthenticationService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    // Registers a new user. Normalizes the email, checks it isn't already taken,
    // hashes the password, then saves the user.
    public RegistrationResponse register(RegistrationRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        return new RegistrationResponse(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail());
    }

    // Logs a user in: looks them up by (normalized) email and checks the password
    // against the stored hash. If either the email doesn't exist or the password is
    // wrong, we throw the same exception with the same message so we don't give away
    // which one it was. Only generates the JWT after the password check passes.
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.getEmail(), user.getId());

        return new LoginResponse(
                token,
                TOKEN_TYPE,
                jwtProperties.getExpirationMilliseconds(),
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail());
    }

    // Emails are stored lowercase/trimmed, so we normalize here too before any lookup.
    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
