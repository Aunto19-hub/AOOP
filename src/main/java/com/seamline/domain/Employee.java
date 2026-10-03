package com.seamline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A person who signs in to Seamline. The email address is the login name; the
 * password is never stored, only its BCrypt hash. The employee ID printed on
 * the ID card is kept as a business identifier but is no longer used to sign in.
 */
@Entity
@Table(name = "employees")
public class Employee extends BaseEntity {

    @Column(name = "employee_id", nullable = false, unique = true, length = 30)
    private String employeeId;

    @Column(name = "email", nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    @Column(name = "phone", unique = true, length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_line_id")
    private ProductionLine assignedLine;

    @Column(nullable = false)
    private boolean enabled = true;

    // "Forgot password": a hashed one-time code plus its expiry. Null once unused or spent.
    @Column(name = "reset_otp_hash", length = 100)
    private String resetOtpHash;

    @Column(name = "reset_otp_expires_at")
    private Instant resetOtpExpiresAt;

    protected Employee() {
        // required by JPA
    }

    public Employee(String employeeId, String email, String passwordHash, String fullName, String jobTitle,
                    Role role) {
        this.employeeId = employeeId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.jobTitle = jobTitle;
        this.role = role;
    }

    /** "Farhana Akter · Industrial Engineer · Line-07" for the sidebar. */
    public String displayCaption() {
        String line = assignedLine == null ? "Unassigned" : assignedLine.getCode();
        return jobTitle + " \u00b7 " + line;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public ProductionLine getAssignedLine() {
        return assignedLine;
    }

    public void setAssignedLine(ProductionLine assignedLine) {
        this.assignedLine = assignedLine;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getResetOtpHash() {
        return resetOtpHash;
    }

    public Instant getResetOtpExpiresAt() {
        return resetOtpExpiresAt;
    }

    public void setResetOtp(String hash, Instant expiresAt) {
        this.resetOtpHash = hash;
        this.resetOtpExpiresAt = expiresAt;
    }

    public void clearResetOtp() {
        this.resetOtpHash = null;
        this.resetOtpExpiresAt = null;
    }
}
