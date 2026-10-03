package com.seamline.service;

import com.seamline.config.SeamlineProperties;
import com.seamline.domain.Employee;
import com.seamline.domain.Role;
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
import com.seamline.exception.InvalidCredentialsException;
import com.seamline.exception.OtpDeliveryException;
import com.seamline.repository.EmployeeRepository;
import com.seamline.security.EmailService;
import com.seamline.security.GoogleIdentity;
import com.seamline.security.GoogleTokenVerifier;
import com.seamline.security.JwtService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sign-in use case: check the credentials with Spring Security, then hand back a
 * signed token plus the profile the sidebar renders. A new account — whether from
 * the plain sign-up form or a first-time Google sign-in — starts disabled and
 * gets no token; an admin has to approve it on the "Registration requests" screen
 * before its owner can sign in.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final EmployeeRepository employees;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final EmailService emailService;
    private final SeamlineProperties properties;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       EmployeeRepository employees,
                       JwtService jwtService,
                       GoogleTokenVerifier googleTokenVerifier,
                       EmailService emailService,
                       SeamlineProperties properties,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.employees = employees;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.emailService = emailService;
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.identifier(), request.password()));
        } catch (DisabledException ex) {
            throw new InvalidCredentialsException("This account is pending admin approval or has been disabled.");
        } catch (AuthenticationException ex) {
            // Deliberately vague: never reveal whether the identifier or the password was wrong.
            throw new InvalidCredentialsException();
        }

        Employee employee = findByEmailOrPhone(request.identifier())
                .orElseThrow(InvalidCredentialsException::new);

        return issueToken(employee);
    }

    /**
     * Google has already proven the person owns that email. If it matches an
     * existing, enabled Employee row, sign them straight in. Otherwise hand back
     * the verified email/name so the front end can offer the one-time sign-up form
     * — we never invent a role for a stranger.
     */
    @Transactional(readOnly = true)
    public GoogleAuthResult loginWithGoogle(GoogleLoginRequest request) {
        GoogleIdentity identity = googleTokenVerifier.verify(request.credential());

        Optional<Employee> existing = employees.findByEmailIgnoreCase(identity.email());
        if (existing.isPresent()) {
            Employee employee = existing.get();
            if (!employee.isEnabled()) {
                throw new InvalidCredentialsException("This account is pending admin approval or has been disabled.");
            }
            return GoogleAuthResult.signedIn(issueToken(employee));
        }

        return GoogleAuthResult.needsSignup(identity.email(), identity.name());
    }

    /**
     * Creates the Employee row a first-time Google sign-in just described, disabled
     * until an admin approves it — see {@link #signUp} for why.
     */
    @Transactional
    public void signUpWithGoogle(GoogleSignupRequest request) {
        GoogleIdentity identity = googleTokenVerifier.verify(request.credential());
        requireEmailFree(identity.email(), "Use Sign in with Google instead.");
        requirePhoneFree(request.phone());

        Role role = resolveRole(request.role());
        requireAdminSlotFree(role);
        // Google is the only way this account ever signs in; the password is never
        // shared with anyone and only exists because the column is not nullable.
        String unusablePassword = passwordEncoder.encode(UUID.randomUUID().toString());
        Employee employee = new Employee(generateEmployeeId(role), identity.email(), unusablePassword,
                request.fullName(), request.jobTitle(), role);
        employee.setPhone(request.phone());
        employee.setEnabled(false);
        employees.save(employee);
    }

    /**
     * The regular "Create account" form: a new employee picks their own email and
     * password. The row is created disabled — no token is issued — until an admin
     * approves it on the "Registration requests" screen.
     */
    @Transactional
    public void signUp(SignupRequest request) {
        requireEmailFree(request.email(), "Sign in instead.");
        requirePhoneFree(request.phone());

        Role role = resolveRole(request.role());
        requireAdminSlotFree(role);
        String passwordHash = passwordEncoder.encode(request.password());
        Employee employee = new Employee(generateEmployeeId(role), request.email(), passwordHash,
                request.fullName(), request.jobTitle(), role);
        employee.setPhone(request.phone());
        employee.setEnabled(false);
        employees.save(employee);
    }

    private static final long OTP_VALID_MINUTES = 10;

    /**
     * Emails a one-time code to the address already on file for that account —
     * never to an address the caller just typed in, or this would let anyone
     * hijack any account by supplying their own email. Responds the same way
     * whether or not an account exists, so this endpoint can't be used to check
     * who's registered; the send itself runs in the background (see
     * {@link EmailService}), so a per-address delivery failure can't leak
     * anything here either. A mail misconfiguration is not account-specific, so
     * that alone is allowed to fail loudly and immediately.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        if (!emailService.isConfigured()) {
            throw new OtpDeliveryException("Email verification is not configured on this server");
        }
        findByEmailOrPhone(request.identifier()).ifPresent(employee -> {
            String code = emailService.generateCode();
            employee.setResetOtp(passwordEncoder.encode(code), Instant.now().plus(OTP_VALID_MINUTES, ChronoUnit.MINUTES));
            employees.save(employee);
            emailService.sendOtpCode(employee.getEmail(), code);
        });
    }

    /** "Forgot password" step 2: the code just emailed, checked against the account's stored hash. */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Employee employee = findByEmailOrPhone(request.identifier())
                .filter(candidate -> candidate.getResetOtpHash() != null
                        && candidate.getResetOtpExpiresAt() != null
                        && candidate.getResetOtpExpiresAt().isAfter(Instant.now())
                        && passwordEncoder.matches(request.code(), candidate.getResetOtpHash()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired code"));

        employee.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        employee.clearResetOtp();
        employees.save(employee);
    }

    /** Whether the sign-in screen should offer the Google button, and for which client ID. */
    public AuthConfigResponse config() {
        String clientId = properties.google() == null ? null : properties.google().clientId();
        return new AuthConfigResponse(clientId == null || clientId.isBlank() ? null : clientId);
    }

    private LoginResponse issueToken(Employee employee) {
        String token = jwtService.generateToken(employee);
        return LoginResponse.bearer(token, jwtService.expiresInSeconds(), UserResponse.from(employee));
    }

    private void requireEmailFree(String email, String hint) {
        if (employees.existsByEmailIgnoreCase(email)) {
            throw new InvalidCredentialsException("An account already exists for " + email + ". " + hint);
        }
    }

    private void requirePhoneFree(String phone) {
        if (phone != null && !phone.isBlank() && employees.existsByPhone(phone)) {
            throw new InvalidCredentialsException("An account already exists for that mobile number.");
        }
    }

    /** Seamline runs one administrator, period — the seeded demo account already fills the slot. */
    private void requireAdminSlotFree(Role role) {
        if (role == Role.ADMIN && employees.existsByRole(Role.ADMIN)) {
            throw new InvalidCredentialsException("An administrator account already exists. Only one admin account is allowed.");
        }
    }

    private Optional<Employee> findByEmailOrPhone(String identifier) {
        return employees.findByEmailIgnoreCase(identifier).or(() -> employees.findByPhone(identifier));
    }

    private Role resolveRole(String name) {
        return Arrays.stream(Role.values())
                .filter(candidate -> candidate.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + name));
    }

    private String generateEmployeeId(Role role) {
        String prefix = switch (role) {
            case ADMIN -> "ADM";
            case INDUSTRIAL_ENGINEER -> "IE";
            case SUPERVISOR -> "SUP";
            case OPERATOR -> "MO";
        };
        String suffix = Long.toHexString(System.nanoTime()).toUpperCase();
        return prefix + "-" + suffix.substring(suffix.length() - 6);
    }
}
