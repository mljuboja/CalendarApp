package com.calendarapp.configuration;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.security.Keys;

// Reads the JWT secret and expiration time from environment variables and builds
// the signing key used by JwtService.
@Component
public class JwtProperties {

    // HS256 requires a key of at least 256 bits (32 bytes).
    private static final int MIN_SECRET_BYTES = 32;

    private final long expirationMilliseconds;
    private final SecretKey signingKey;

    public JwtProperties(
            @Value("${JWT_SECRET}") String secret,
            @Value("${JWT_EXPIRATION_MILLISECONDS}") long expirationMilliseconds) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT_SECRET must be set");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes long for HS256");
        }
        if (expirationMilliseconds <= 0) {
            throw new IllegalArgumentException("JWT_EXPIRATION_MILLISECONDS must be greater than 0");
        }

        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMilliseconds = expirationMilliseconds;
    }

    public long getExpirationMilliseconds() {
        return expirationMilliseconds;
    }

    public SecretKey getSigningKey() {
        return signingKey;
    }
}
