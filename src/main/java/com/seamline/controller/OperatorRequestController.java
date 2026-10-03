package com.seamline.controller;

import com.seamline.dto.OperatorRequestCreateRequest;
import com.seamline.dto.OperatorRequestResponse;
import com.seamline.dto.ResolveRequest;
import com.seamline.security.SeamlineUserDetails;
import com.seamline.service.OperatorRequestService;
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

/** Problem reports, time-off, shift-swap and other requests any signed-in employee can raise. */
@RestController
@RequestMapping("/api/requests")
public class OperatorRequestController {

    private final OperatorRequestService requestService;

    public OperatorRequestController(OperatorRequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    public ResponseEntity<OperatorRequestResponse> create(@AuthenticationPrincipal SeamlineUserDetails principal,
                                                          @Valid @RequestBody OperatorRequestCreateRequest body) {
        return ResponseEntity.ok(requestService.create(principal.getEmployee(), body));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<OperatorRequestResponse>> mine(@AuthenticationPrincipal SeamlineUserDetails principal) {
        return ResponseEntity.ok(requestService.mine(principal.getEmployee()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<List<OperatorRequestResponse>> all() {
        return ResponseEntity.ok(requestService.all());
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<OperatorRequestResponse> resolve(@AuthenticationPrincipal SeamlineUserDetails principal,
                                                           @PathVariable Long id,
                                                           @Valid @RequestBody ResolveRequest body) {
        return ResponseEntity.ok(requestService.resolve(principal.getEmployee(), id, body));
    }
}
