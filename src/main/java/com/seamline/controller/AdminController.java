package com.seamline.controller;

import com.seamline.dto.PendingRegistrationResponse;
import com.seamline.service.AdminService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only: the "Registration requests" screen. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/registrations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PendingRegistrationResponse>> pendingRegistrations() {
        return ResponseEntity.ok(adminService.pendingRegistrations());
    }

    @PostMapping("/registrations/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        adminService.approve(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/registrations/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        adminService.reject(id);
        return ResponseEntity.noContent().build();
    }
}
