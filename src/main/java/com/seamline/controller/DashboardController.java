package com.seamline.controller;

import com.seamline.dto.DashboardResponse;
import com.seamline.dto.RunRequest;
import com.seamline.security.SeamlineUserDetails;
import com.seamline.service.DashboardService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The Dashboard screen. Both endpoints return exactly the same payload shape. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** GET /api/dashboard?line=Line-07 (defaults to the caller's own line). */
    @GetMapping
    public ResponseEntity<DashboardResponse> dashboard(
            @RequestParam(name = "line", required = false) String line,
            @AuthenticationPrincipal SeamlineUserDetails principal) {

        return ResponseEntity.ok(dashboardService.dashboard(line, principal.getEmployee()));
    }

    /** POST /api/dashboard/rerun — the "Re-run simulation" button. */
    @PostMapping("/rerun")
    @PreAuthorize("hasAnyRole('ADMIN', 'INDUSTRIAL_ENGINEER', 'SUPERVISOR')")
    public ResponseEntity<DashboardResponse> rerun(
            @RequestParam(name = "line", required = false) String line,
            @Valid @RequestBody(required = false) RunRequest request,
            @AuthenticationPrincipal SeamlineUserDetails principal) {

        RunRequest effective = request == null ? RunRequest.defaults() : request;
        return ResponseEntity.ok(dashboardService.rerun(line, effective, principal.getEmployee()));
    }
}
