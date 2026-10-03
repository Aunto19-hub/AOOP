package com.seamline.dto;

import java.util.List;

/**
 * A balanced plan plus the shift that was simulated on it. Nothing is stored:
 * this is the "what if" endpoint behind the Line simulation screen.
 */
public record SimulationResponse(RunParameters parameters,
                                 Plan plan,
                                 Result result) {

    public record RunParameters(String algorithm,
                                String algorithmLabel,
                                int teamSize,
                                int bufferPerStation,
                                double variability,
                                long seed,
                                int shiftMinutes) {
    }

    public record Plan(int stationCount,
                       double totalSmv,
                       double cycleTimeMinutes,
                       double balanceLossPercent,
                       double theoreticalRatePerHour,
                       List<Station> stations) {
    }

    public record Station(int index,
                          String code,
                          String machine,
                          String machineLabel,
                          String operatorCode,
                          String operatorName,
                          List<String> operations,
                          double minutesPerPiece,
                          double utilisation,
                          double blockedMinutes,
                          double starvedMinutes,
                          int piecesDone,
                          String status,
                          String statusLabel) {
    }

    public record Result(int piecesProduced,
                         double outputRatePerHour,
                         double lineEfficiency,
                         double cycleTimeMinutes,
                         double clockMinutes,
                         List<Integer> hourlyOutput,
                         List<DashboardResponse.LossSlice> lossBreakdown) {
    }
}
