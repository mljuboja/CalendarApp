package com.calendarapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// What we send back after a successful registration. No password/hash included.
@Getter
@AllArgsConstructor
public class RegistrationResponse {

    private final Long userId;
    private final String firstName;
    private final String lastName;
    private final String email;
}
