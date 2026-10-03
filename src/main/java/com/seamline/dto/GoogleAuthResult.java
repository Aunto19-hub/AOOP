package com.seamline.dto;

/**
 * What {@code POST /api/auth/google} hands back. Either the Google email already
 * matches a Seamline employee — {@code login} is populated, same as a password
 * sign-in — or it doesn't, in which case the front end shows the one-time
 * "create your account" form using the verified {@code email}/{@code suggestedName}.
 */
public record GoogleAuthResult(boolean needsSignup, String email, String suggestedName, LoginResponse login) {

    public static GoogleAuthResult signedIn(LoginResponse login) {
        return new GoogleAuthResult(false, null, null, login);
    }

    public static GoogleAuthResult needsSignup(String email, String suggestedName) {
        return new GoogleAuthResult(true, email, suggestedName, null);
    }
}
