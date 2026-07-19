package com.calendarapp.configuration;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.security.Keys;

/**
 * Reads and validates JWT configuration from environment variables
 * ({@code JWT_SECRET}, {@code JWT_EXPIRATION_MILLISECONDS}). Nothing here is
 * hardcoded. If the configuration is missing or the secret is obviously
 * insecure, application startup fails fast with a clear message rather than
 * silently running with a weak key.
 */
@Component
public class JwtProperties {

    /** 256 bits — the minimum key length recommended for HMAC-SHA256 (HS256). */
    private static final int MIN_SECRET_BYTES = 32;

    private static final List<String> OBVIOUSLY_INSECURE_VALUES = List.of(
            "changeme", "change-me", "changeit", "change-it",
            "secret", "password", "test", "example", "default",
            "your-secret-key", "your_secret_key", "jwt_secret", "jwtsecret",
            "12345678", "replace_me", "replace-me", "placeholder"
    );

    private final long expirationMilliseconds;
    private final SecretKey signingKey;

    public JwtProperties(
            @Value("${JWT_SECRET:}") String secret,
            @Value("${JWT_EXPIRATION_MILLISECONDS:}") String expirationMillisecondsRaw) {
        validateSecret(secret);
        this.expirationMilliseconds = parseExpiration(expirationMillisecondsRaw);
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public long getExpirationMilliseconds() {
        return expirationMilliseconds;
    }

    public SecretKey getSigningKey() {
        return signingKey;
    }

    private static void validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET environment variable is not set. Refusing to start. "
                            + "Set a long, random, high-entropy value (see .env.example). "
                            + "Generate one locally with, e.g.: openssl rand -base64 48");
        }

        int byteLength = secret.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET is too short (" + byteLength + " bytes). It must be at least "
                            + MIN_SECRET_BYTES + " bytes (256 bits) to safely sign tokens with HS256. "
                            + "Generate one locally with, e.g.: openssl rand -base64 48");
        }

        String normalized = secret.trim().toLowerCase(Locale.ROOT);
        for (String insecureValue : OBVIOUSLY_INSECURE_VALUES) {
            if (normalized.contains(insecureValue)) {
                throw new IllegalStateException(
                        "JWT_SECRET looks like a placeholder or an obviously insecure value "
                                + "(contains '" + insecureValue + "'). Refusing to start. "
                                + "Generate a real random secret, e.g.: openssl rand -base64 48");
            }
        }
    }

    private static long parseExpiration(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException(
                    "JWT_EXPIRATION_MILLISECONDS environment variable is not set. Refusing to start. "
                            + "Set it to a positive number of milliseconds, e.g. 3600000 for 1 hour "
                            + "(see .env.example).");
        }

        long value;
        try {
            value = Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "JWT_EXPIRATION_MILLISECONDS must be a whole number of milliseconds, got: '" + raw + "'.");
        }

        if (value <= 0) {
            throw new IllegalStateException(
                    "JWT_EXPIRATION_MILLISECONDS must be a positive number of milliseconds, got: " + value);
        }

        return value;
    }
}
