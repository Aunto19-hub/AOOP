package com.seamline.dto;

import com.seamline.domain.Employee;

/** The signed-in employee, as the sidebar needs it. */
public record UserResponse(String employeeId,
                           String email,
                           String phone,
                           String fullName,
                           String jobTitle,
                           String role,
                           String roleLabel,
                           String assignedLine,
                           String caption) {

    public static UserResponse from(Employee employee) {
        return new UserResponse(
                employee.getEmployeeId(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getFullName(),
                employee.getJobTitle(),
                employee.getRole().name(),
                employee.getRole().getLabel(),
                employee.getAssignedLine() == null ? null : employee.getAssignedLine().getCode(),
                employee.displayCaption());
    }
}
