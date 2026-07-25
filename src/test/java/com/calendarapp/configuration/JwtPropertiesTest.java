package com.calendarapp.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    private static final String VALID_SECRET =
            "f4a1c9e27b6d3081f5a9c4e6b2d7108f3c5a8e1b4d6f9c2a7e0b3d5f8a1c4e6b";

    @Test
    void nullSecretThrowsException() {
        assertThatThrownBy(() -> new JwtProperties(null, 3600000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void blankSecretThrowsException() {
        assertThatThrownBy(() -> new JwtProperties("   ", 3600000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void tooShortSecretThrowsException() {
        assertThatThrownBy(() -> new JwtProperties("short", 3600000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void nonPositiveExpirationThrowsException() {
        assertThatThrownBy(() -> new JwtProperties(VALID_SECRET, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT_EXPIRATION_MILLISECONDS");
    }

    @Test
    void validConfigurationIsAccepted() {
        JwtProperties properties = new JwtProperties(VALID_SECRET, 3600000);

        assertThat(properties.getExpirationMilliseconds()).isEqualTo(3600000L);
        assertThat(properties.getSigningKey()).isNotNull();
    }
}
