package com.seamline.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The "Create account" form: a brand-new employee, signing up with a password. */
public record SignupRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 160)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 4, max = 100)
        String password,

        @NotBlank(message = "Full name is required")
        @Size(max = 120)
        String fullName,

        @Size(max = 30)
        String phone,

        @Size(max = 120)
        String jobTitle,

        @NotBlank(message = "Role is required")
        String role) {
}
