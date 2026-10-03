package com.seamline.domain;

/**
 * Machine families available on the sewing floor. An operation can only run on
 * one family, and an operator's speed is measured per family.
 */
public enum MachineType {

    PLAIN("Plain"),
    OVERLOCK("Overlock"),
    FLATLOCK("Flatlock"),
    BUTTON_HOLE("Button hole"),
    BUTTON_ATTACH("Button attach"),
    IRON("Iron"),
    MANUAL("Manual");

    private final String label;

    MachineType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
