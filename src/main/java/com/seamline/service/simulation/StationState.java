package com.seamline.service.simulation;

/**
 * What a station is doing at a point in time.
 *
 * <p>Only WORKING earns minutes. BLOCKED means the next station has no room,
 * STARVED means nothing arrived: both are the losses the dashboard visualises.</p>
 */
public enum StationState {

    WORKING("Working"),
    BLOCKED("Blocked"),
    STARVED("Starved"),
    IDLE("Idle");

    private final String label;

    StationState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
