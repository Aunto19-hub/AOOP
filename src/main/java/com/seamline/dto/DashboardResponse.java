package com.seamline.dto;

import java.time.Instant;
import java.util.List;

/**
 * Everything the dashboard screen draws, in one payload: the header, the four
 * KPI cards, the bottleneck alert, the two charts and the station table.
 */
public record DashboardResponse(LineSummary line,
                                StyleSummary style,
                                RunSummary run,
                                KpiSummary kpis,
                                AlertSummary alert,
                                List<HourlyPoint> hourly,
                                List<LossSlice> lossBreakdown,
                                List<StationRow> stations) {

    public record LineSummary(String code,
                              String unitName,
                              int shiftMinutes,
                              int targetRatePerHour,
                              int targetPiecesPerShift,
                              double taktMinutesPerPiece) {
    }

    public record StyleSummary(String code,
                               String name,
                               int operationCount,
                               double totalSmv) {
    }

    public record RunSummary(Long id,
                             Instant runAt,
                             String algorithm,
                             String algorithmLabel,
                             int teamSize,
                             int stationCount,
                             int bufferPerStation,
                             double variability,
                             long seed,
                             String triggeredBy) {
    }

    /** The four cards across the top of the dashboard. */
    public record KpiSummary(int piecesThisShift,
                             int targetPieces,
                             double piecesDeltaPercent,
                             double outputRatePerHour,
                             double lineEfficiency,
                             double balanceLoss,
                             double cycleTimeMinutes) {
    }

    public record AlertSummary(String severity,
                               String title,
                               String detail) {
    }

    /** One bar of "Production by hour — simulated vs target". */
    public record HourlyPoint(int hour,
                              String label,
                              int pieces,
                              int target) {
    }

    /** One slice of "Where the shift went". */
    public record LossSlice(String label,
                            double minutes,
                            double percent) {
    }

    /** One row of the station performance table. */
    public record StationRow(int index,
                             String station,
                             String machine,
                             String machineLabel,
                             String operator,
                             String operations,
                             double minutesPerPiece,
                             double utilisation,
                             double blockedMinutes,
                             double starvedMinutes,
                             int piecesDone,
                             String status,
                             String statusLabel) {
    }
}
