package com.seamline.dto;

import com.seamline.domain.ApprovalRequest;
import java.time.Instant;

public record ApprovalRequestResponse(Long id,
                                      String proposerEmployeeId,
                                      String proposerName,
                                      String proposerCaption,
                                      String title,
                                      String projectedImpact,
                                      String status,
                                      String statusLabel,
                                      String decidedByName,
                                      Instant createdAt,
                                      Instant decidedAt) {

    public static ApprovalRequestResponse from(ApprovalRequest r) {
        return new ApprovalRequestResponse(
                r.getId(),
                r.getProposer().getEmployeeId(),
                r.getProposer().getFullName(),
                r.getProposer().getJobTitle(),
                r.getTitle(),
                r.getProjectedImpact(),
                r.getStatus().name(),
                r.getStatus().getLabel(),
                r.getDecidedBy() == null ? null : r.getDecidedBy().getFullName(),
                r.getCreatedAt(),
                r.getDecidedAt());
    }
}
