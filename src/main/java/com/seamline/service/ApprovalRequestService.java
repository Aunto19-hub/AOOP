package com.seamline.service;

import com.seamline.domain.ApprovalRequest;
import com.seamline.domain.ApprovalStatus;
import com.seamline.domain.Employee;
import com.seamline.dto.ApprovalRequestCreateRequest;
import com.seamline.dto.ApprovalRequestResponse;
import com.seamline.exception.ResourceNotFoundException;
import com.seamline.repository.ApprovalRequestRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Line-plan changes an Industrial Engineer proposes and a Supervisor (or
 * Admin) signs off on — submit, then approve or reject. Same submit-then-
 * resolve shape as {@code OperatorRequestService}, but with a three-state
 * outcome instead of a plain resolved flag.
 */
@Service
public class ApprovalRequestService {

    private final ApprovalRequestRepository approvals;

    public ApprovalRequestService(ApprovalRequestRepository approvals) {
        this.approvals = approvals;
    }

    @Transactional
    public ApprovalRequestResponse create(Employee proposer, ApprovalRequestCreateRequest body) {
        ApprovalRequest saved = approvals.save(
                new ApprovalRequest(proposer, body.title().trim(), body.projectedImpact().trim()));
        return ApprovalRequestResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestResponse> all() {
        return approvals.findAllByOrderByCreatedAtDesc().stream().map(ApprovalRequestResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestResponse> mine(Employee proposer) {
        return approvals.findByProposerOrderByCreatedAtDesc(proposer).stream()
                .map(ApprovalRequestResponse::from).toList();
    }

    @Transactional
    public ApprovalRequestResponse decide(Employee decider, Long id, ApprovalStatus outcome) {
        ApprovalRequest request = approvals.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Approval request", String.valueOf(id)));
        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalArgumentException("That proposal has already been decided");
        }
        request.decide(decider, outcome, Instant.now());
        return ApprovalRequestResponse.from(request);
    }
}
