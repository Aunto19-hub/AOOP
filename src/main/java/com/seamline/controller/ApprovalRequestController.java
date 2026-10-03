package com.seamline.controller;

import com.seamline.domain.ApprovalStatus;
import com.seamline.dto.ApprovalRequestCreateRequest;
import com.seamline.dto.ApprovalRequestResponse;
import com.seamline.security.SeamlineUserDetails;
import com.seamline.service.ApprovalRequestService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Line-plan changes an Industrial Engineer proposes; a Supervisor or Admin signs off. */
@RestController
@RequestMapping("/api/approvals")
public class ApprovalRequestController {

    private final ApprovalRequestService approvalService;

    public ApprovalRequestController(ApprovalRequestService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INDUSTRIAL_ENGINEER', 'ADMIN')")
    public ResponseEntity<ApprovalRequestResponse> create(@AuthenticationPrincipal SeamlineUserDetails principal,
                                                          @Valid @RequestBody ApprovalRequestCreateRequest body) {
        return ResponseEntity.ok(approvalService.create(principal.getEmployee(), body));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_ENGINEER', 'ADMIN')")
    public ResponseEntity<List<ApprovalRequestResponse>> mine(@AuthenticationPrincipal SeamlineUserDetails principal) {
        return ResponseEntity.ok(approvalService.mine(principal.getEmployee()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<List<ApprovalRequestResponse>> all() {
        return ResponseEntity.ok(approvalService.all());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApprovalRequestResponse> approve(@AuthenticationPrincipal SeamlineUserDetails principal,
                                                           @PathVariable Long id) {
        return ResponseEntity.ok(approvalService.decide(principal.getEmployee(), id, ApprovalStatus.APPROVED));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApprovalRequestResponse> reject(@AuthenticationPrincipal SeamlineUserDetails principal,
                                                          @PathVariable Long id) {
        return ResponseEntity.ok(approvalService.decide(principal.getEmployee(), id, ApprovalStatus.REJECTED));
    }
}
