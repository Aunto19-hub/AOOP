package com.seamline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** "Forgot password" step 1: which account, identified by email or mobile number. */
public record ForgotPasswordRequest(

        @NotBlank(message = "Email or mobile number is required")
        @Size(max = 160)
        String identifier) {
}
