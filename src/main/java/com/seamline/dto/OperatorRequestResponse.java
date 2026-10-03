package com.seamline.dto;

import com.seamline.domain.OperatorRequest;
import java.time.Instant;

public record OperatorRequestResponse(Long id,
                                      String requesterEmployeeId,
                                      String requesterName,
                                      String type,
                                      String typeLabel,
                                      String description,
                                      boolean resolved,
                                      String resolutionNote,
                                      String resolvedByName,
                                      Instant createdAt,
                                      Instant resolvedAt) {

    public static OperatorRequestResponse from(OperatorRequest r) {
        return new OperatorRequestResponse(
                r.getId(),
                r.getRequester().getEmployeeId(),
                r.getRequester().getFullName(),
                r.getType().name(),
                r.getType().getLabel(),
                r.getDescription(),
                r.isResolved(),
                r.getResolutionNote(),
                r.getResolvedBy() == null ? null : r.getResolvedBy().getFullName(),
                r.getCreatedAt(),
                r.getResolvedAt());
    }
}
