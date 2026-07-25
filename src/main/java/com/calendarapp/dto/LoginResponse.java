package com.calendarapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// What we send back after a successful login: the JWT plus some basic user info.
// No password/hash or JWT secret included.
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
