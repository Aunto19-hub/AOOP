package com.seamline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The one-time "create your account" form a new Google sign-in fills in. */
public record GoogleSignupRequest(

        @NotBlank(message = "Google credential is required")
        String credential,

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
