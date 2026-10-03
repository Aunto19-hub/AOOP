package com.seamline.domain;

/** Where a proposed line-plan change stands in the supervisor's sign-off workflow. */
public enum ApprovalStatus {

    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String label;

    ApprovalStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
