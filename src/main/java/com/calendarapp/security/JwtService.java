package com.calendarapp.security;

import java.util.Date;

import org.springframework.stereotype.Service;

import com.calendarapp.configuration.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

/**
 * Standalone JWT utility: generates tokens, validates them, and extracts claims.
 *
 * <p>This class is intentionally <b>not</b> wired into Spring Security yet — there
 * is no authentication filter, no {@code UserDetailsService}, and nothing here
 * enforces JWTs on any request. That is later-phase work.
 *
 * <p>Tokens only ever carry non-sensitive identifiers: the user's email as the
 * subject and their user ID as a claim. Passwords or other sensitive data are
 * never placed in a token.
 */
@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "userId";

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Generates a signed JWT for the given user.
     *
     * @param email  the user's email — used as the token subject
     * @param userId the user's ID — stored as a custom claim
     */
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

    /**
     * Parses and verifies the token's signature, returning all claims.
     *
     * @throws JwtException            if the token is malformed, expired, or the
     *                                  signature does not match
     * @throws IllegalArgumentException if the token is null/blank
     */
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

    /**
     * Returns {@code true} only if the signature is valid and the token is not
     * expired and is otherwise well-formed. Never throws.
     */
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Returns whether the token's expiration claim is in the past. Requires a
     * valid signature to trust the claim; propagates {@link JwtException} if the
     * token cannot be parsed/verified at all for any other reason (e.g. bad
     * signature, malformed token).
     *
     * <p>Note: the underlying parser already rejects expired tokens by throwing
     * {@link ExpiredJwtException} instead of returning claims, so that case is
     * handled explicitly here.
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
}
