package com.seamline.dto;

import com.seamline.domain.Employee;
import java.time.Instant;

/** One row on the admin's "Registration requests" screen. */
public record PendingRegistrationResponse(Long id,
                                          String employeeId,
                                          String email,
                                          String phone,
                                          String fullName,
                                          String jobTitle,
                                          String role,
                                          String roleLabel,
                                          Instant requestedAt) {

    public static PendingRegistrationResponse from(Employee employee) {
        return new PendingRegistrationResponse(
                employee.getId(),
                employee.getEmployeeId(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getFullName(),
                employee.getJobTitle(),
                employee.getRole().name(),
                employee.getRole().getLabel(),
                employee.getCreatedAt());
    }
}
