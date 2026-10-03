package com.seamline.dto;

import java.util.List;

/**
 * Side-by-side comparison of several plans run against the same random seed, so
 * any difference is caused by the variable named in the scenario and not by luck.
 */
public record ScenarioResponse(long seed, List<Scenario> scenarios) {

    public record Scenario(String name,
                           String algorithm,
                           int teamSize,
                           int bufferPerStation,
                           int stationCount,
                           double cycleTimeMinutes,
                           double balanceLossPercent,
                           int piecesProduced,
                           double lineEfficiency,
                           String verdict) {
    }
}
