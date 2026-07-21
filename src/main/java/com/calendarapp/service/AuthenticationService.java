package com.calendarapp.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.calendarapp.dto.RegistrationRequest;
import com.calendarapp.dto.RegistrationResponse;
import com.calendarapp.entity.User;
import com.calendarapp.exception.DuplicateEmailException;
import com.calendarapp.repository.UserRepository;

/**
 * Phase 3B: registration only. Login, JWT issuance, and authenticated
 * endpoints are explicitly out of scope here.
 */
@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
