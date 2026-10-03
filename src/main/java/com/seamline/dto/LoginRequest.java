package com.seamline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Credentials posted by the sign-in ticket. {@code identifier} is either the employee's email or mobile number. */
public record LoginRequest(

        @NotBlank(message = "Email or mobile number is required")
        @Size(max = 160)
        String identifier,

        @NotBlank(message = "Password is required")
        @Size(min = 4, max = 100)
        String password) {
}
