package com.seamline.domain;

/** Health of a single station after a simulated shift. */
public enum StationStatus {

    BOTTLENECK("Bottleneck"),
    TIGHT("Tight"),
    STARVED("Starved"),
    HEALTHY("Healthy");

    private final String label;

    StationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
