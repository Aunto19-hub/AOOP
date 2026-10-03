package com.seamline.service;

import com.seamline.domain.Employee;
import com.seamline.dto.PendingRegistrationResponse;
import com.seamline.exception.ResourceNotFoundException;
import com.seamline.repository.EmployeeRepository;
import com.seamline.security.EmailService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The admin's "Registration requests" screen: list, approve, or reject sign-ups awaiting a decision. */
@Service
public class AdminService {

    private final EmployeeRepository employees;
    private final EmailService emailService;

    public AdminService(EmployeeRepository employees, EmailService emailService) {
        this.employees = employees;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<PendingRegistrationResponse> pendingRegistrations() {
        return employees.findByEnabledFalseOrderByCreatedAtAsc().stream()
                .map(PendingRegistrationResponse::from)
                .toList();
    }

    /**
     * Lets the new account sign in, then emails them the good news. The email runs
     * in the background (see {@link EmailService}), so this returns as soon as the
     * database write commits — approving shouldn't feel like it's hanging while
     * Gmail's SMTP does its thing.
     */
    @Transactional
    public void approve(Long id) {
        Employee employee = findPending(id);
        employee.setEnabled(true);
        employees.save(employee);
        emailService.sendAccountApproved(employee.getEmail(), employee.getFullName());
    }

    /** Discards the request outright — there's nothing to "keep" for a sign-up nobody approved. */
    @Transactional
    public void reject(Long id) {
        employees.delete(findPending(id));
    }

    private Employee findPending(Long id) {
        Employee employee = employees.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Registration request", String.valueOf(id)));
        if (employee.isEnabled()) {
            throw new IllegalArgumentException("That account is already approved");
        }
        return employee;
    }
}
