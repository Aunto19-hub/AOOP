package com.seamline.domain;

/** The kinds of thing an operator can ask their supervisor for or flag to them. */
public enum RequestType {

    PROBLEM_REPORT("Report a problem"),
    DEFECT_REPORT("Defect count"),
    TIME_OFF("Time off request"),
    SHIFT_SWAP("Shift swap"),
    STATION_CHANGE("Station change"),
    SUGGESTION("Suggestion");

    private final String label;

    RequestType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
