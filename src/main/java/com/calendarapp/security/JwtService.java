package com.calendarapp.security;

import java.util.Date;

import org.springframework.stereotype.Service;

import com.calendarapp.configuration.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

// Handles creating and reading JWTs. Used by AuthenticationService to issue
// tokens and by JwtAuthenticationFilter to read them on incoming requests.
// Tokens only store the user's email and id, never the password.
@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "userId";

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    // Builds a signed JWT for the given user. Email is the subject, userId is a custom claim.
    public String generateToken(String email, Long userId) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + jwtProperties.getExpirationMilliseconds());

        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_USER_ID, userId)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(jwtProperties.getSigningKey())
                .compact();
    }

    // Checks the token's signature and returns its claims. Throws if the token is
    // malformed, expired, or was signed with a different key.
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtProperties.getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return extractAllClaims(token).get(CLAIM_USER_ID, Long.class);
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    // True only if the token has a valid signature and hasn't expired. Never throws.
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Checks if the token's expiration date is in the past.
    // The JWT library throws ExpiredJwtException instead of just returning the claims
    // for an expired token, so we catch that case here.
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
}
