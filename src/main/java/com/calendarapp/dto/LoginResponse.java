package com.calendarapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response payload for {@code POST /api/auth/login}. Deliberately excludes
 * {@code passwordHash}, the JWT secret, and any internal security
 * configuration — only the issued token and public user details.
 */
@Getter
@AllArgsConstructor
public class LoginResponse {

    private final String token;
    private final String tokenType;
    private final long expiresInMilliseconds;
    private final Long userId;
    private final String firstName;
    private final String lastName;
    private final String email;
}
