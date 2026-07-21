package com.calendarapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response payload for {@code POST /api/auth/register}. Deliberately excludes
 * {@code passwordHash} and anything JWT-related (no login/token issuance in
 * this phase).
 */
@Getter
@AllArgsConstructor
public class RegistrationResponse {

    private final Long userId;
    private final String firstName;
    private final String lastName;
    private final String email;
}
