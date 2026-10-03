package com.seamline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A physical sewing line, e.g. "Line-07" in Unit 2. */
@Entity
@Table(name = "production_lines")
public class ProductionLine extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(name = "unit_name", length = 60)
    private String unitName;

    @Column(name = "shift_minutes", nullable = false)
    private int shiftMinutes = 480;

    @Column(name = "target_rate_per_hour", nullable = false)
    private int targetRatePerHour = 78;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_style_id")
    private Style currentStyle;

    protected ProductionLine() {
        // required by JPA
    }

    public ProductionLine(String code, String unitName, int shiftMinutes, int targetRatePerHour) {
        this.code = code;
        this.unitName = unitName;
        this.shiftMinutes = shiftMinutes;
        this.targetRatePerHour = targetRatePerHour;
    }

    /** Takt time in minutes per piece implied by the target rate. */
    public double taktMinutesPerPiece() {
        return targetRatePerHour <= 0 ? 0d : 60d / targetRatePerHour;
    }

    /** Pieces the line is expected to deliver in a full shift. */
    public int targetPiecesPerShift() {
        return (int) Math.round(targetRatePerHour * (shiftMinutes / 60d));
    }

    public String getCode() {
        return code;
    }

    public String getUnitName() {
        return unitName;
    }

    public int getShiftMinutes() {
        return shiftMinutes;
    }

    public void setShiftMinutes(int shiftMinutes) {
        this.shiftMinutes = shiftMinutes;
    }

    public int getTargetRatePerHour() {
        return targetRatePerHour;
    }

    public void setTargetRatePerHour(int targetRatePerHour) {
        this.targetRatePerHour = targetRatePerHour;
    }

    public Style getCurrentStyle() {
        return currentStyle;
    }

    public void setCurrentStyle(Style currentStyle) {
        this.currentStyle = currentStyle;
    }
}
