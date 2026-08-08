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
    private static final long ONE_HOUR_MS = 3_600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(TEST_SECRET, ONE_HOUR_MS);
        jwtService = new JwtService(jwtProperties);
    }

    @Test
    void makesTokenAndGetsEmailAndUserId() {
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
        JwtProperties shortProperties = new JwtProperties(TEST_SECRET, 1); // expires at 1 ms
        JwtService shortService = new JwtService(shortProperties);

        String token = shortService.generateToken("user@example.com", 1L);
        Thread.sleep(25); // makes sure clock time passes the 1 ms expiration

        assertThat(shortService.isTokenExpired(token)).isTrue();
        assertThat(shortService.isTokenValid(token)).isFalse();
    }

    @Test
    void messedUpTokenFailsSignatureValidation() {
        String token = jwtService.generateToken("user@example.com", 1L);

        // A JWT is header.payload.signature. Changing a character right at the
        // end of the signature can land on unused Base64URL padding bits and
        // not actually change the decoded signature bytes, so instead we
        // change a character in the middle of the signature segment.
        String[] parts = token.split("\\.");
        String signature = parts[2];
        int middleIndex = signature.length() / 2;
        char originalChar = signature.charAt(middleIndex);
        char replacementChar = originalChar == 'A' ? 'B' : 'A';
        String tamperedSignature =
                signature.substring(0, middleIndex) + replacementChar + signature.substring(middleIndex + 1);

        String tamperedToken = parts[0] + "." + parts[1] + "." + tamperedSignature;

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
