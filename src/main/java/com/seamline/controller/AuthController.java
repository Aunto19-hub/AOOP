package com.seamline.controller;

import com.seamline.dto.AuthConfigResponse;
import com.seamline.dto.ForgotPasswordRequest;
import com.seamline.dto.GoogleAuthResult;
import com.seamline.dto.GoogleLoginRequest;
import com.seamline.dto.GoogleSignupRequest;
import com.seamline.dto.LoginRequest;
import com.seamline.dto.LoginResponse;
import com.seamline.dto.ResetPasswordRequest;
import com.seamline.dto.SignupRequest;
import com.seamline.dto.UserResponse;
import com.seamline.security.SeamlineUserDetails;
import com.seamline.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sign-in, current user, sign-out. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signUp(request);
        return ResponseEntity.ok(Map.of("message",
                "Account request submitted. An administrator needs to approve it before you can sign in."));
    }

    @PostMapping("/google")
    public ResponseEntity<GoogleAuthResult> google(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request));
    }

    /** The one-time "create your account" form a first-time Google sign-in submits. */
    @PostMapping("/google/signup")
    public ResponseEntity<Map<String, String>> googleSignup(@Valid @RequestBody GoogleSignupRequest request) {
        authService.signUpWithGoogle(request);
        return ResponseEntity.ok(Map.of("message",
                "Account request submitted. An administrator needs to approve it before you can sign in."));
    }

    /** So the sign-in screen knows whether to render the Google button, and for which client ID. */
    @GetMapping("/config")
    public ResponseEntity<AuthConfigResponse> config() {
        return ResponseEntity.ok(authService.config());
    }

    /** "Forgot password" step 1: emails a code to the address on file, if the account exists. */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message",
                "If that account exists, we've emailed a verification code to the address on file."));
    }

    /** "Forgot password" step 2: the emailed code plus the new password. */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password updated. Sign in with your new password."));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal SeamlineUserDetails principal) {
        return ResponseEntity.ok(UserResponse.from(principal.getEmployee()));
    }

    /**
     * Tokens are stateless, so signing out is the client dropping the token.
     * The endpoint exists so the front end has one thing to call.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Signed out. Discard the access token on the client."));
    }
}
