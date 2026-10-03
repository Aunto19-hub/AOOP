package com.seamline.domain;

/** Application roles. Mapped to Spring Security authorities as ROLE_<NAME>. */
public enum Role {

    ADMIN("Administrator"),
    INDUSTRIAL_ENGINEER("Industrial Engineer"),
    SUPERVISOR("Line Supervisor"),
    OPERATOR("Sewing Operator");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String authority() {
        return "ROLE_" + name();
    }
}
