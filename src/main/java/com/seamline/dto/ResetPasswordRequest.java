package com.seamline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** "Forgot password" step 2: the texted code, plus the new password. */
public record ResetPasswordRequest(

        @NotBlank(message = "Email or mobile number is required")
        @Size(max = 160)
        String identifier,

        @NotBlank(message = "Verification code is required")
        @Size(max = 10)
        String code,

        @NotBlank(message = "New password is required")
        @Size(min = 4, max = 100)
        String newPassword) {
}
