package com.calendarapp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.calendarapp.configuration.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

// Tests for JwtService: making tokens, reading claims back out, and catching
// expired/invalid/tampered tokens.
class JwtServiceTest {

    private static final String TEST_SECRET =
            "f4a1c9e27b6d3081f5a9c4e6b2d7108f3c5a8e1b4d6f9c2a7e0b3d5f8a1c4e6b";
    private static final String OTHER_SECRET =
            "9b3d6f1a4c7e0b2d5f8a1c4e6b9d3f0a7c2e5b8d1f4a7c0e3b6d9f2a5c8e1b4d";
    private static final long ONE_HOUR_MS = 60L * 60 * 1000;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(TEST_SECRET, ONE_HOUR_MS);
        jwtService = new JwtService(jwtProperties);
    }

    @Test
    void generatesTokenAndExtractsEmailAndUserIdClaim() {
        String token = jwtService.generateToken("user@example.com", 42L);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    void tokenCarriesIssuedAtAndExpirationClaims() {
        String token = jwtService.generateToken("user@example.com", 1L);

        Claims claims = jwtService.extractAllClaims(token);

        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void validFreshlyIssuedTokenIsValidAndNotExpired() {
        String token = jwtService.generateToken("user@example.com", 1L);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    void expiredTokenIsDetectedAsInvalid() throws InterruptedException {
        JwtProperties shortLivedProperties = new JwtProperties(TEST_SECRET, 1); // 1ms expiration
        JwtService shortLivedService = new JwtService(shortLivedProperties);

        String token = shortLivedService.generateToken("user@example.com", 1L);
        Thread.sleep(25); // ensure real clock time passes the 1ms expiration window

        assertThat(shortLivedService.isTokenExpired(token)).isTrue();
        assertThat(shortLivedService.isTokenValid(token)).isFalse();
    }

    @Test
    void tamperedTokenFailsSignatureValidation() {
        String token = jwtService.generateToken("user@example.com", 1L);
        // Flip the last two characters of the signature segment to corrupt it.
        String tamperedToken = token.substring(0, token.length() - 2)
                + (token.endsWith("A") ? "B" : "A") + "A";

        assertThat(jwtService.isTokenValid(tamperedToken)).isFalse();
        assertThatThrownBy(() -> jwtService.extractAllClaims(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithADifferentKeyIsRejected() {
        JwtProperties otherKeyProperties = new JwtProperties(OTHER_SECRET, ONE_HOUR_MS);
        JwtService otherKeyService = new JwtService(otherKeyProperties);

        String tokenFromOtherKey = otherKeyService.generateToken("user@example.com", 1L);

        assertThat(jwtService.isTokenValid(tokenFromOtherKey)).isFalse();
        assertThatThrownBy(() -> jwtService.extractAllClaims(tokenFromOtherKey))
                .isInstanceOf(JwtException.class);
    }
}
