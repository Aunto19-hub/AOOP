package com.seamline.dto;

/** What the browser stores after a successful sign-in. */
public record LoginResponse(String accessToken,
                            String tokenType,
                            long expiresInSeconds,
                            UserResponse user) {

    public static LoginResponse bearer(String token, long expiresInSeconds, UserResponse user) {
        return new LoginResponse(token, "Bearer", expiresInSeconds, user);
    }
}
