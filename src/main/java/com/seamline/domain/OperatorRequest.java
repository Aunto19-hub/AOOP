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
 * One thing an operator has flagged or asked for — a broken machine, a time-off
 * request, a shift swap, and so on. {@link RequestType} tells the two portals
 * how to label it; the resolution workflow is the same for every type: a
 * supervisor or admin closes it out with an optional note.
 */
@Entity
@Table(name = "operator_requests")
public class OperatorRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private Employee requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestType type;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private Employee resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected OperatorRequest() {
        // required by JPA
    }

    public OperatorRequest(Employee requester, RequestType type, String description) {
        this.requester = requester;
        this.type = type;
        this.description = description;
    }

    public void resolve(Employee resolver, String note, Instant at) {
        this.resolved = true;
        this.resolvedBy = resolver;
        this.resolutionNote = note;
        this.resolvedAt = at;
    }

    public Employee getRequester() {
        return requester;
    }

    public RequestType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public boolean isResolved() {
        return resolved;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public Employee getResolvedBy() {
        return resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
