package com.seamline.service.simulation;

import java.util.List;

/** Aggregate result of one simulated shift. */
public record SimulationOutcome(double clockMinutes,
                                int piecesProduced,
                                double outputRatePerHour,
                                double lineEfficiencyPercent,
                                double workingMinutes,
                                double blockedMinutes,
                                double starvedMinutes,
                                double idleMinutes,
                                List<Integer> hourlyOutput,
                                List<StationMetrics> stations) {

    /** Total station-minutes available during the shift. */
    public double availableMinutes() {
        return workingMinutes + blockedMinutes + starvedMinutes + idleMinutes;
    }
}
