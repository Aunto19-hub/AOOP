package com.seamline.service;

import com.seamline.domain.Employee;
import com.seamline.domain.Operator;
import com.seamline.domain.ProductionLine;
import com.seamline.domain.ShiftRun;
import com.seamline.domain.StationResult;
import com.seamline.domain.Style;
import com.seamline.dto.DashboardResponse;
import com.seamline.dto.RunRequest;
import com.seamline.exception.ResourceNotFoundException;
import com.seamline.repository.OperatorRepository;
import com.seamline.repository.ProductionLineRepository;
import com.seamline.repository.ShiftRunRepository;
import com.seamline.service.balancing.BalancingStrategyRegistry;
import com.seamline.service.balancing.LineBalancer;
import com.seamline.service.balancing.LinePlan;
import com.seamline.service.simulation.ShiftSimulator;
import com.seamline.service.simulation.SimulationConfig;
import com.seamline.service.simulation.SimulationOutcome;
import com.seamline.service.simulation.StationMetrics;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Everything behind the Dashboard screen: find the line, make sure a simulated
 * shift exists for it, and shape the stored result into the payload the UI draws.
 */
@Service
public class DashboardService {

    private final ProductionLineRepository lines;
    private final OperatorRepository operators;
    private final ShiftRunRepository runs;
    private final LineBalancer balancer;
    private final ShiftSimulator simulator;
    private final BalancingStrategyRegistry strategies;

    public DashboardService(ProductionLineRepository lines,
                            OperatorRepository operators,
                            ShiftRunRepository runs,
                            LineBalancer balancer,
                            ShiftSimulator simulator,
                            BalancingStrategyRegistry strategies) {
        this.lines = lines;
        this.operators = operators;
        this.runs = runs;
        this.balancer = balancer;
        this.simulator = simulator;
        this.strategies = strategies;
    }

    /** Latest stored shift for the line; simulates one on first visit. */
    @Transactional
    public DashboardResponse dashboard(String lineCode, Employee viewer) {
        ProductionLine line = resolveLine(lineCode, viewer);
        ShiftRun run = runs.findFirstByLineOrderByRunAtDescIdDesc(line)
                .orElseGet(() -> simulateAndStore(line, RunRequest.defaults(), viewer));
        return toResponse(run);
    }

    /** "Re-run simulation": balance again, simulate again, store, return. */
    @Transactional
    public DashboardResponse rerun(String lineCode, RunRequest request, Employee viewer) {
        ProductionLine line = resolveLine(lineCode, viewer);
        return toResponse(simulateAndStore(line, request, viewer));
    }

    private ProductionLine resolveLine(String lineCode, Employee viewer) {
        if (lineCode != null && !lineCode.isBlank()) {
            return lines.findByCodeIgnoreCase(lineCode)
                    .orElseThrow(() -> ResourceNotFoundException.of("Production line", lineCode));
        }
        if (viewer != null && viewer.getAssignedLine() != null) {
            return lines.findById(viewer.getAssignedLine().getId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Production line", "assigned"));
        }
        return lines.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No production line has been configured yet"));
    }

    private ShiftRun simulateAndStore(ProductionLine line, RunRequest request, Employee viewer) {
        Style style = line.getCurrentStyle();
        if (style == null) {
            throw new ResourceNotFoundException("Line " + line.getCode() + " has no style loaded");
        }

        List<Operator> roster = operators.findAllByOrderByCodeAsc();
        LinePlan plan = balancer.balance(style, roster, request.algorithmOrDefault(), request.teamSizeOrDefault());

        SimulationConfig config = SimulationConfig.of(
                line.getShiftMinutes(),
                request.bufferOrDefault(),
                request.variabilityOrDefault(),
                request.seedOrDefault());

        SimulationOutcome outcome = simulator.run(plan, config);

        ShiftRun run = new ShiftRun(line, style, plan.getAlgorithmKey(), request.teamSizeOrDefault(),
                config.bufferPerStation(), config.variability(), config.seed());
        run.setTriggeredBy(viewer);
        run.setPiecesProduced(outcome.piecesProduced());
        run.setOutputRatePerHour(outcome.outputRatePerHour());
        run.setLineEfficiency(outcome.lineEfficiencyPercent());
        run.setBalanceLoss(plan.getBalanceLossPercent());
        run.setCycleTimeMinutes(plan.getCycleTimeMinutes());
        run.setWorkingMinutes(outcome.workingMinutes());
        run.setBlockedMinutes(outcome.blockedMinutes());
        run.setStarvedMinutes(outcome.starvedMinutes());
        run.setIdleMinutes(outcome.idleMinutes());
        run.setHourlyOutput(new ArrayList<>(outcome.hourlyOutput()));

        for (StationMetrics metrics : outcome.stations()) {
            StationResult result = new StationResult(
                    metrics.index(),
                    metrics.stationCode(),
                    metrics.machineType(),
                    metrics.operatorCode(),
                    metrics.operatorName(),
                    metrics.operationCodes(),
                    metrics.minutesPerPiece());
            result.setUtilisation(metrics.utilisationPercent());
            result.setBlockedMinutes(metrics.blockedMinutes());
            result.setStarvedMinutes(metrics.starvedMinutes());
            result.setPiecesDone(metrics.piecesDone());
            result.classify(plan.getCycleTimeMinutes());
            run.addStationResult(result);
        }

        return runs.save(run);
    }

    // ------------------------------------------------------------------
    // Mapping to the UI payload
    // ------------------------------------------------------------------

    private DashboardResponse toResponse(ShiftRun run) {
        ProductionLine line = run.getLine();
        Style style = run.getStyle();

        DashboardResponse.LineSummary lineSummary = new DashboardResponse.LineSummary(
                line.getCode(),
                line.getUnitName(),
                line.getShiftMinutes(),
                line.getTargetRatePerHour(),
                line.targetPiecesPerShift(),
                round(line.taktMinutesPerPiece(), 2));

        DashboardResponse.StyleSummary styleSummary = new DashboardResponse.StyleSummary(
                style.getCode(), style.getName(), style.operationCount(), round(style.totalSmv(), 2));

        DashboardResponse.RunSummary runSummary = new DashboardResponse.RunSummary(
                run.getId(),
                run.getRunAt(),
                run.getAlgorithm(),
                strategies.resolve(run.getAlgorithm()).displayName(),
                run.getTeamSize(),
                run.stationCount(),
                run.getBufferPerStation(),
                run.getVariability(),
                run.getSeed(),
                run.getTriggeredBy() == null ? null : run.getTriggeredBy().getFullName());

        int targetPieces = line.targetPiecesPerShift();
        double delta = targetPieces == 0 ? 0d
                : (run.getPiecesProduced() - targetPieces) * 100d / targetPieces;

        DashboardResponse.KpiSummary kpis = new DashboardResponse.KpiSummary(
                run.getPiecesProduced(),
                targetPieces,
                round(delta, 1),
                round(run.getOutputRatePerHour(), 1),
                round(run.getLineEfficiency(), 1),
                round(run.getBalanceLoss(), 1),
                round(run.getCycleTimeMinutes(), 2));

        return new DashboardResponse(
                lineSummary,
                styleSummary,
                runSummary,
                kpis,
                buildAlert(run),
                buildHourly(run),
                buildLossBreakdown(run),
                buildStationRows(run));
    }

    private DashboardResponse.AlertSummary buildAlert(ShiftRun run) {
        StationResult bottleneck = run.bottleneck();
        if (bottleneck == null) {
            return new DashboardResponse.AlertSummary("ok", "No stations planned", "Balance the line to see results.");
        }

        double takt = run.getLine().taktMinutesPerPiece();
        double blockedUpstream = run.getStationResults().stream()
                .filter(station -> station.getStationIndex() < bottleneck.getStationIndex())
                .mapToDouble(StationResult::getBlockedMinutes)
                .sum();

        String severity;
        if (bottleneck.getMinutesPerPiece() > takt * 1.25) {
            severity = "critical";
        } else if (bottleneck.getMinutesPerPiece() > takt) {
            severity = "warning";
        } else {
            severity = "ok";
        }

        String detail = String.format(Locale.ROOT,
                "%s on %s runs %.2f min/pc against a %.2f min target. Upstream stations lost %.0f minutes to blocking.",
                bottleneck.getOperationCodes(),
                bottleneck.getMachineType().getLabel().toLowerCase(),
                bottleneck.getMinutesPerPiece(),
                takt,
                blockedUpstream);

        return new DashboardResponse.AlertSummary(
                severity,
                bottleneck.getStationCode() + " is constraining the line",
                detail);
    }

    private List<DashboardResponse.HourlyPoint> buildHourly(ShiftRun run) {
        List<Integer> cumulative = run.getHourlyOutput();
        List<DashboardResponse.HourlyPoint> points = new ArrayList<>(cumulative.size());
        int previous = 0;
        for (int hour = 0; hour < cumulative.size(); hour++) {
            int total = cumulative.get(hour) == null ? previous : cumulative.get(hour);
            points.add(new DashboardResponse.HourlyPoint(
                    hour + 1,
                    "H" + (hour + 1),
                    Math.max(0, total - previous),
                    run.getLine().getTargetRatePerHour()));
            previous = total;
        }
        return points;
    }

    private List<DashboardResponse.LossSlice> buildLossBreakdown(ShiftRun run) {
        double total = run.getWorkingMinutes() + run.getBlockedMinutes()
                + run.getStarvedMinutes() + run.getIdleMinutes();
        return List.of(
                slice("Working", run.getWorkingMinutes(), total),
                slice("Blocked", run.getBlockedMinutes(), total),
                slice("Starved", run.getStarvedMinutes(), total),
                slice("Idle", run.getIdleMinutes(), total));
    }

    private DashboardResponse.LossSlice slice(String label, double minutes, double total) {
        double percent = total <= 0 ? 0d : minutes / total * 100d;
        return new DashboardResponse.LossSlice(label, round(minutes, 1), round(percent, 1));
    }

    private List<DashboardResponse.StationRow> buildStationRows(ShiftRun run) {
        List<DashboardResponse.StationRow> rows = new ArrayList<>(run.getStationResults().size());
        for (StationResult station : run.getStationResults()) {
            rows.add(new DashboardResponse.StationRow(
                    station.getStationIndex(),
                    station.getStationCode(),
                    station.getMachineType().name(),
                    station.getMachineType().getLabel(),
                    station.getOperatorName(),
                    station.getOperationCodes(),
                    round(station.getMinutesPerPiece(), 2),
                    round(station.getUtilisation(), 1),
                    round(station.getBlockedMinutes(), 1),
                    round(station.getStarvedMinutes(), 1),
                    station.getPiecesDone(),
                    station.getStatus().name(),
                    station.getStatus().getLabel()));
        }
        return rows;
    }

    private static double round(double value, int places) {
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }
}
