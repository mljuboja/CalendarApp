package com.calendarapp.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Verifies that missing or obviously insecure JWT configuration fails fast at
 * startup with a clear message, rather than letting the app run with a weak key.
 */
class JwtPropertiesTest {

    private static final String VALID_SECRET =
            "f4a1c9e27b6d3081f5a9c4e6b2d7108f3c5a8e1b4d6f9c2a7e0b3d5f8a1c4e6b";

    @Test
    void missingSecretFailsStartup() {
        assertThatThrownBy(() -> new JwtProperties("", "3600000"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void tooShortSecretFailsStartup() {
        assertThatThrownBy(() -> new JwtProperties("short", "3600000"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too short");
    }

    @Test
    void obviouslyInsecureSecretFailsStartup() {
        assertThatThrownBy(() -> new JwtProperties("this-is-a-changeme-secret-value-padded-out", "3600000"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder");
    }

    @Test
    void missingExpirationFailsStartup() {
        assertThatThrownBy(() -> new JwtProperties(VALID_SECRET, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_EXPIRATION_MILLISECONDS");
    }

    @Test
    void nonPositiveExpirationFailsStartup() {
        assertThatThrownBy(() -> new JwtProperties(VALID_SECRET, "0"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void nonNumericExpirationFailsStartup() {
        assertThatThrownBy(() -> new JwtProperties(VALID_SECRET, "not-a-number"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("whole number");
    }

    @Test
    void validConfigurationIsAccepted() {
        JwtProperties properties = new JwtProperties(VALID_SECRET, "3600000");

        assertThat(properties.getExpirationMilliseconds()).isEqualTo(3600000L);
        assertThat(properties.getSigningKey()).isNotNull();
    }
}
