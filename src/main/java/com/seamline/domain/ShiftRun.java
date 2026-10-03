package com.seamline.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The stored result of one simulated 480-minute shift. The dashboard never
 * recomputes anything: it reads the most recent run for the line.
 */
@Entity
@Table(name = "shift_runs")
public class ShiftRun extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "line_id")
    private ProductionLine line;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "style_id")
    private Style style;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "triggered_by_id")
    private Employee triggeredBy;

    @Column(nullable = false, length = 40)
    private String algorithm;

    @Column(name = "team_size", nullable = false)
    private int teamSize;

    @Column(name = "buffer_per_station", nullable = false)
    private int bufferPerStation;

    @Column(nullable = false)
    private double variability;

    @Column(name = "random_seed", nullable = false)
    private long seed;

    @Column(name = "run_at", nullable = false)
    private Instant runAt = Instant.now();

    @Column(name = "pieces_produced", nullable = false)
    private int piecesProduced;

    @Column(name = "output_rate_per_hour", nullable = false)
    private double outputRatePerHour;

    @Column(name = "line_efficiency", nullable = false)
    private double lineEfficiency;

    @Column(name = "balance_loss", nullable = false)
    private double balanceLoss;

    @Column(name = "cycle_time_minutes", nullable = false)
    private double cycleTimeMinutes;

    @Column(name = "working_minutes", nullable = false)
    private double workingMinutes;

    @Column(name = "blocked_minutes", nullable = false)
    private double blockedMinutes;

    @Column(name = "starved_minutes", nullable = false)
    private double starvedMinutes;

    @Column(name = "idle_minutes", nullable = false)
    private double idleMinutes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "shift_run_hourly", joinColumns = @JoinColumn(name = "run_id"))
    @OrderColumn(name = "hour_index")
    @Column(name = "pieces")
    private List<Integer> hourlyOutput = new ArrayList<>();

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @OrderBy("stationIndex ASC")
    private List<StationResult> stationResults = new ArrayList<>();

    protected ShiftRun() {
        // required by JPA
    }

    public ShiftRun(ProductionLine line, Style style, String algorithm,
                    int teamSize, int bufferPerStation, double variability, long seed) {
        this.line = line;
        this.style = style;
        this.algorithm = algorithm;
        this.teamSize = teamSize;
        this.bufferPerStation = bufferPerStation;
        this.variability = variability;
        this.seed = seed;
    }

    public void addStationResult(StationResult result) {
        stationResults.add(result);
        result.setRun(this);
    }

    /** The station that sets the pace of the whole line. */
    public StationResult bottleneck() {
        return stationResults.stream()
                .max((a, b) -> Double.compare(a.getMinutesPerPiece(), b.getMinutesPerPiece()))
                .orElse(null);
    }

    public int stationCount() {
        return stationResults.size();
    }

    public ProductionLine getLine() {
        return line;
    }

    public Style getStyle() {
        return style;
    }

    public Employee getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(Employee triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public int getTeamSize() {
        return teamSize;
    }

    public int getBufferPerStation() {
        return bufferPerStation;
    }

    public double getVariability() {
        return variability;
    }

    public long getSeed() {
        return seed;
    }

    public Instant getRunAt() {
        return runAt;
    }

    public int getPiecesProduced() {
        return piecesProduced;
    }

    public void setPiecesProduced(int piecesProduced) {
        this.piecesProduced = piecesProduced;
    }

    public double getOutputRatePerHour() {
        return outputRatePerHour;
    }

    public void setOutputRatePerHour(double outputRatePerHour) {
        this.outputRatePerHour = outputRatePerHour;
    }

    public double getLineEfficiency() {
        return lineEfficiency;
    }

    public void setLineEfficiency(double lineEfficiency) {
        this.lineEfficiency = lineEfficiency;
    }

    public double getBalanceLoss() {
        return balanceLoss;
    }

    public void setBalanceLoss(double balanceLoss) {
        this.balanceLoss = balanceLoss;
    }

    public double getCycleTimeMinutes() {
        return cycleTimeMinutes;
    }

    public void setCycleTimeMinutes(double cycleTimeMinutes) {
        this.cycleTimeMinutes = cycleTimeMinutes;
    }

    public double getWorkingMinutes() {
        return workingMinutes;
    }

    public void setWorkingMinutes(double workingMinutes) {
        this.workingMinutes = workingMinutes;
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

    public double getIdleMinutes() {
        return idleMinutes;
    }

    public void setIdleMinutes(double idleMinutes) {
        this.idleMinutes = idleMinutes;
    }

    public List<Integer> getHourlyOutput() {
        return hourlyOutput;
    }

    public void setHourlyOutput(List<Integer> hourlyOutput) {
        this.hourlyOutput = hourlyOutput;
    }

    public List<StationResult> getStationResults() {
        return stationResults;
    }
}
