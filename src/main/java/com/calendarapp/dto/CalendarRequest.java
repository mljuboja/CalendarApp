package com.calendarapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// What the client sends to create or update a calendar. Same color rule as the
// Calendar entity and the Flyway CHECK constraint: a 6-digit hex color like #4A90E2.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CalendarRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @NotBlank(message = "Color is required")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a hex value like #4A90E2")
    private String color;
}
