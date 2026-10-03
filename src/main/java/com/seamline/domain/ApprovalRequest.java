package com.seamline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A line-plan change an Industrial Engineer proposes — a buffer tweak, an
 * algorithm switch, an operator move — that a Supervisor (or Admin) signs off
 * on before it's real. Mirrors {@link OperatorRequest}'s submit-then-resolve
 * shape, but with three states instead of two: a decision is either still
 * pending, or it was approved, or it was rejected.
 */
@Entity
@Table(name = "approval_requests")
public class ApprovalRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposer_id", nullable = false)
    private Employee proposer;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "projected_impact", nullable = false, length = 300)
    private String projectedImpact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_id")
    private Employee decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected ApprovalRequest() {
        // required by JPA
    }

    public ApprovalRequest(Employee proposer, String title, String projectedImpact) {
        this.proposer = proposer;
        this.title = title;
        this.projectedImpact = projectedImpact;
    }

    public void decide(Employee decider, ApprovalStatus outcome, Instant at) {
        this.decidedBy = decider;
        this.status = outcome;
        this.decidedAt = at;
    }

    public Employee getProposer() {
        return proposer;
    }

    public String getTitle() {
        return title;
    }

    public String getProjectedImpact() {
        return projectedImpact;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public Employee getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
