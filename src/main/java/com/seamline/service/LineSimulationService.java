package com.seamline.service;

import com.seamline.domain.Employee;
import com.seamline.domain.Operator;
import com.seamline.domain.ProductionLine;
import com.seamline.domain.StationStatus;
import com.seamline.domain.Style;
import com.seamline.dto.DashboardResponse;
import com.seamline.dto.RunRequest;
import com.seamline.dto.ScenarioResponse;
import com.seamline.dto.SimulationResponse;
import com.seamline.exception.ResourceNotFoundException;
import com.seamline.repository.OperatorRepository;
import com.seamline.repository.ProductionLineRepository;
import com.seamline.service.balancing.BalancingStrategyRegistry;
import com.seamline.service.balancing.LineBalancer;
import com.seamline.service.balancing.LinePlan;
import com.seamline.service.balancing.PlannedStation;
import com.seamline.service.simulation.ShiftSimulator;
import com.seamline.service.simulation.SimulationConfig;
import com.seamline.service.simulation.SimulationOutcome;
import com.seamline.service.simulation.StationMetrics;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Throw-away runs for the Industrial Engineer: balance a line, simulate a shift,
 * return the numbers. Nothing is persisted, which is what makes the Line
 * simulation and Scenarios screens safe to hammer.
 *
 * <p>Which line is simulated follows the same rule as the Dashboard: an explicit
 * line code wins, otherwise the signed-in user's own line, otherwise the first
 * line on file. Every run reads the line's current style and the operator roster
 * straight from the database.
 */
@Service
public class LineSimulationService {

    private final ProductionLineRepository lines;
    private final OperatorRepository operators;
    private final LineBalancer balancer;
    private final ShiftSimulator simulator;
    private final BalancingStrategyRegistry strategies;

    public LineSimulationService(ProductionLineRepository lines,
                                 OperatorRepository operators,
                                 LineBalancer balancer,
                                 ShiftSimulator simulator,
                                 BalancingStrategyRegistry strategies) {
        this.lines = lines;
        this.operators = operators;
        this.balancer = balancer;
        this.simulator = simulator;
        this.strategies = strategies;
    }

    @Transactional(readOnly = true)
    public SimulationResponse run(String lineCode, RunRequest request, Employee viewer) {
        ProductionLine line = line(lineCode, viewer);
        Style style = style(line);
        List<Operator> roster = operators.findAllByOrderByCodeAsc();

        LinePlan plan = balancer.balance(style, roster, request.algorithmOrDefault(), request.teamSizeOrDefault());
        SimulationConfig config = SimulationConfig.of(line.getShiftMinutes(), request.bufferOrDefault(),
                request.variabilityOrDefault(), request.seedOrDefault());
        SimulationOutcome outcome = simulator.run(plan, config);

        return new SimulationResponse(
                new SimulationResponse.RunParameters(
                        plan.getAlgorithmKey(),
                        plan.getAlgorithmLabel(),
                        request.teamSizeOrDefault(),
                        config.bufferPerStation(),
                        config.variability(),
                        config.seed(),
                        config.shiftMinutes()),
                new SimulationResponse.Plan(
                        plan.stationCount(),
                        round(plan.getTotalSmv(), 2),
                        round(plan.getCycleTimeMinutes(), 2),
                        round(plan.getBalanceLossPercent(), 1),
                        round(plan.theoreticalRatePerHour(), 1),
                        stations(plan, outcome)),
                new SimulationResponse.Result(
                        outcome.piecesProduced(),
                        round(outcome.outputRatePerHour(), 1),
                        round(outcome.lineEfficiencyPercent(), 1),
                        round(plan.getCycleTimeMinutes(), 2),
                        round(outcome.clockMinutes(), 0),
                        outcome.hourlyOutput(),
                        losses(outcome)));
    }

    /**
     * The five comparisons the Scenarios screen shows. Every one uses the same
     * seed, so the only thing that changes is the variable in the scenario name.
     */
    @Transactional(readOnly = true)
    public ScenarioResponse compare(String lineCode, Long seed, Employee viewer) {
        long effectiveSeed = seed == null ? RunRequest.DEFAULT_SEED : seed;
        int team = RunRequest.DEFAULT_TEAM_SIZE;

        List<ScenarioDefinition> definitions = List.of(
                new ScenarioDefinition("Ranked positional weight", "rpw", team, 3),
                new ScenarioDefinition("Greedy topological fill", "greedy", team, 3),
                new ScenarioDefinition("RPW, buffer 1 (tight)", "rpw", team, 1),
                new ScenarioDefinition("RPW, buffer 8 (loose)", "rpw", team, 8),
                new ScenarioDefinition("RPW, 11 operators (short team)", "rpw", 11, 3));

        List<SimulationResponse> results = new ArrayList<>();
        for (ScenarioDefinition definition : definitions) {
            results.add(run(lineCode, new RunRequest(definition.algorithm(), definition.teamSize(),
                    definition.buffer(), RunRequest.DEFAULT_VARIABILITY, effectiveSeed), viewer));
        }

        int best = 0;
        for (int i = 1; i < results.size(); i++) {
            if (results.get(i).result().piecesProduced() > results.get(best).result().piecesProduced()) {
                best = i;
            }
        }
        int baseline = results.get(0).result().piecesProduced();

        List<ScenarioResponse.Scenario> scenarios = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            SimulationResponse result = results.get(i);
            scenarios.add(new ScenarioResponse.Scenario(
                    definitions.get(i).name(),
                    result.parameters().algorithm(),
                    result.parameters().teamSize(),
                    result.parameters().bufferPerStation(),
                    result.plan().stationCount(),
                    result.plan().cycleTimeMinutes(),
                    result.plan().balanceLossPercent(),
                    result.result().piecesProduced(),
                    result.result().lineEfficiency(),
                    verdict(i, best, baseline, result.result().piecesProduced())));
        }
        return new ScenarioResponse(effectiveSeed, scenarios);
    }

    private String verdict(int index, int best, int baseline, int pieces) {
        if (index == best) {
            return "Best output";
        }
        int delta = pieces - baseline;
        if (index == 0) {
            return "Baseline";
        }
        return (delta >= 0 ? "+" : "") + delta + " pcs vs baseline";
    }

    private List<SimulationResponse.Station> stations(LinePlan plan, SimulationOutcome outcome) {
        List<SimulationResponse.Station> stations = new ArrayList<>(plan.stationCount());
        for (StationMetrics metrics : outcome.stations()) {
            PlannedStation planned = plan.getStations().get(metrics.index());
            StationStatus status = classify(metrics, plan.getCycleTimeMinutes());
            stations.add(new SimulationResponse.Station(
                    metrics.index(),
                    metrics.stationCode(),
                    metrics.machineType().name(),
                    metrics.machineType().getLabel(),
                    metrics.operatorCode(),
                    metrics.operatorName(),
                    planned.getOperations().stream().map(operation -> operation.getCode()).toList(),
                    round(metrics.minutesPerPiece(), 2),
                    round(metrics.utilisationPercent(), 1),
                    round(metrics.blockedMinutes(), 1),
                    round(metrics.starvedMinutes(), 1),
                    metrics.piecesDone(),
                    status.name(),
                    status.getLabel()));
        }
        return stations;
    }

    private StationStatus classify(StationMetrics metrics, double cycleTime) {
        if (metrics.minutesPerPiece() >= cycleTime - 1e-9) {
            return StationStatus.BOTTLENECK;
        }
        if (metrics.utilisationPercent() >= 85d) {
            return StationStatus.TIGHT;
        }
        if (metrics.starvedMinutes() >= 60d) {
            return StationStatus.STARVED;
        }
        return StationStatus.HEALTHY;
    }

    private List<DashboardResponse.LossSlice> losses(SimulationOutcome outcome) {
        double total = outcome.availableMinutes();
        return List.of(
                slice("Working", outcome.workingMinutes(), total),
                slice("Blocked", outcome.blockedMinutes(), total),
                slice("Starved", outcome.starvedMinutes(), total),
                slice("Idle", outcome.idleMinutes(), total));
    }

    private DashboardResponse.LossSlice slice(String label, double minutes, double total) {
        double percent = total <= 0 ? 0d : minutes / total * 100d;
        return new DashboardResponse.LossSlice(label, round(minutes, 1), round(percent, 1));
    }

    private ProductionLine line(String lineCode, Employee viewer) {
        if (lineCode != null && !lineCode.isBlank()) {
            return lines.findByCodeIgnoreCase(lineCode)
                    .orElseThrow(() -> ResourceNotFoundException.of("Production line", lineCode));
        }
        if (viewer != null && viewer.getAssignedLine() != null) {
            return lines.findById(viewer.getAssignedLine().getId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Production line", "assigned"));
        }
        return lines.findAllByOrderByCodeAsc().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No production line has been configured yet"));
    }

    private Style style(ProductionLine line) {
        Style style = line.getCurrentStyle();
        if (style == null) {
            throw new ResourceNotFoundException("Line " + line.getCode() + " has no style loaded");
        }
        return style;
    }

    private static double round(double value, int places) {
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }

    private record ScenarioDefinition(String name, String algorithm, int teamSize, int buffer) {
    }
}
