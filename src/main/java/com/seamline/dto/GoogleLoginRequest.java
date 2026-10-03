package com.seamline.dto;

import jakarta.validation.constraints.NotBlank;

/** The ID token Google Identity Services hands the front end after the user picks an account. */
public record GoogleLoginRequest(

        @NotBlank(message = "Google credential is required")
        String credential) {
}
