package com.seamline.service.balancing;

import java.util.List;

/**
 * The output of balancing: an ordered list of stations plus the two numbers that
 * describe how good the balance is.
 */
public class LinePlan {

    private final String algorithmKey;
    private final String algorithmLabel;
    private final List<PlannedStation> stations;
    private final double totalSmv;
    private final double cycleTimeMinutes;
    private final double balanceLossPercent;

    LinePlan(String algorithmKey, String algorithmLabel, List<PlannedStation> stations, double totalSmv) {
        this.algorithmKey = algorithmKey;
        this.algorithmLabel = algorithmLabel;
        this.stations = List.copyOf(stations);
        this.totalSmv = totalSmv;
        this.cycleTimeMinutes = stations.stream()
                .mapToDouble(PlannedStation::getMinutesPerPiece)
                .max()
                .orElse(0d);
        double loaded = stations.stream().mapToDouble(PlannedStation::getMinutesPerPiece).sum();
        double capacity = cycleTimeMinutes * stations.size();
        this.balanceLossPercent = capacity <= 0 ? 0d : (capacity - loaded) / capacity * 100d;
    }

    /** Theoretical pieces per hour if nothing ever blocked or starved. */
    public double theoreticalRatePerHour() {
        return cycleTimeMinutes <= 0 ? 0d : 60d / cycleTimeMinutes;
    }

    public String getAlgorithmKey() {
        return algorithmKey;
    }

    public String getAlgorithmLabel() {
        return algorithmLabel;
    }

    public List<PlannedStation> getStations() {
        return stations;
    }

    public int stationCount() {
        return stations.size();
    }

    public double getTotalSmv() {
        return totalSmv;
    }

    public double getCycleTimeMinutes() {
        return cycleTimeMinutes;
    }

    public double getBalanceLossPercent() {
        return balanceLossPercent;
    }
}
