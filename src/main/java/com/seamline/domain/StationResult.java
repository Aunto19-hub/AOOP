package com.seamline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One row of the "Station performance" table on the dashboard. */
@Entity
@Table(name = "station_results")
public class StationResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id")
    private ShiftRun run;

    @Column(name = "station_index", nullable = false)
    private int stationIndex;

    @Column(name = "station_code", nullable = false, length = 20)
    private String stationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "machine_type", nullable = false, length = 30)
    private MachineType machineType;

    @Column(name = "operator_code", length = 20)
    private String operatorCode;

    @Column(name = "operator_name", nullable = false, length = 120)
    private String operatorName;

    /** Operation codes joined with "+", e.g. "S03" or "BH1+BA1". */
    @Column(name = "operation_codes", nullable = false, length = 200)
    private String operationCodes;

    @Column(name = "minutes_per_piece", nullable = false)
    private double minutesPerPiece;

    @Column(nullable = false)
    private double utilisation;

    @Column(name = "blocked_minutes", nullable = false)
    private double blockedMinutes;

    @Column(name = "starved_minutes", nullable = false)
    private double starvedMinutes;

    @Column(name = "pieces_done", nullable = false)
    private int piecesDone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StationStatus status = StationStatus.HEALTHY;

    protected StationResult() {
        // required by JPA
    }

    public StationResult(int stationIndex, String stationCode, MachineType machineType, String operatorCode,
                         String operatorName, String operationCodes, double minutesPerPiece) {
        this.stationIndex = stationIndex;
        this.stationCode = stationCode;
        this.machineType = machineType;
        this.operatorCode = operatorCode;
        this.operatorName = operatorName;
        this.operationCodes = operationCodes;
        this.minutesPerPiece = minutesPerPiece;
    }

    /**
     * Classifies the station from the simulated shift. Encapsulating the rule here
     * keeps the dashboard mapper free of business logic.
     */
    public void classify(double cycleTimeMinutes) {
        if (minutesPerPiece >= cycleTimeMinutes - 1e-9) {
            status = StationStatus.BOTTLENECK;
        } else if (utilisation >= 85d) {
            status = StationStatus.TIGHT;
        } else if (starvedMinutes >= 60d) {
            status = StationStatus.STARVED;
        } else {
            status = StationStatus.HEALTHY;
        }
    }

    public ShiftRun getRun() {
        return run;
    }

    void setRun(ShiftRun run) {
        this.run = run;
    }

    public int getStationIndex() {
        return stationIndex;
    }

    public String getStationCode() {
        return stationCode;
    }

    public MachineType getMachineType() {
        return machineType;
    }

    public String getOperatorCode() {
        return operatorCode;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getOperationCodes() {
        return operationCodes;
    }

    public double getMinutesPerPiece() {
        return minutesPerPiece;
    }

    public double getUtilisation() {
        return utilisation;
    }

    public void setUtilisation(double utilisation) {
        this.utilisation = utilisation;
    }

    public double getBlockedMinutes() {
        return blockedMinutes;
    }

    public void setBlockedMinutes(double blockedMinutes) {
        this.blockedMinutes = blockedMinutes;
    }

    public double getStarvedMinutes() {
        return starvedMinutes;
    }

    public void setStarvedMinutes(double starvedMinutes) {
        this.starvedMinutes = starvedMinutes;
    }

    public int getPiecesDone() {
        return piecesDone;
    }

    public void setPiecesDone(int piecesDone) {
        this.piecesDone = piecesDone;
    }

    public StationStatus getStatus() {
        return status;
    }
}
