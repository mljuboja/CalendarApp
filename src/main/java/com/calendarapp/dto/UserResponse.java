package com.calendarapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// What we send back for the current authenticated user. No password/hash included.
@Getter
@AllArgsConstructor
public class UserResponse {

    private final Long userId;
    private final String firstName;
    private final String lastName;
    private final String email;
}
