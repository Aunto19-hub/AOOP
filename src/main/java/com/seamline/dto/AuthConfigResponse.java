package com.seamline.dto;

/** What the sign-in screen needs before it renders: whether Google Sign-In is available. */
public record AuthConfigResponse(String googleClientId) {
}
