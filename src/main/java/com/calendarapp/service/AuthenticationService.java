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

/**
 * Phase 3B: registration. Phase 3C: login + JWT issuance. Request-time JWT
 * authentication (a filter that would enforce tokens on protected endpoints)
 * is still out of scope — nothing here changes how incoming requests are
 * authorized; it only issues a token after verifying credentials.
 */
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

    /**
     * Registers a new user: normalizes the email, rejects duplicates, hashes
     * the password with the existing BCrypt {@link PasswordEncoder} bean, and
     * persists the new {@link User}.
     *
     * @throws DuplicateEmailException if the normalized email already belongs
     *                                  to an existing user
     */
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

    /**
     * Authenticates a user by email and password, issuing a JWT on success.
     *
     * <p>Unknown email and incorrect password are indistinguishable to the
     * caller — both throw {@link InvalidCredentialsException} with the same
     * generic message, so login can never be used to enumerate registered
     * emails. The user is never saved or modified, and no server-side
     * session is created; the JWT is generated only after the submitted
     * password has been verified against the stored hash.
     *
     * @throws InvalidCredentialsException if the email is unknown or the
     *                                       password does not match
     */
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

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
