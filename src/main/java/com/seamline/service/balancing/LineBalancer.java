package com.seamline.service.balancing;

import com.seamline.domain.Operation;
import com.seamline.domain.Operator;
import com.seamline.domain.Style;
import com.seamline.exception.SimulationException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Turns a style plus a roster into a balanced line.
 *
 * <p>The strategy decides the order of the operations; this class does the
 * packing: it keeps filling the current station while the next operation runs on
 * the same machine, its predecessors are already placed, and the station stays
 * within the target workload. Otherwise it opens a new station and hands it to
 * the fastest qualified operator still free.</p>
 */
@Service
public class LineBalancer {

    /** How far a station may exceed the average workload before a new one opens. */
    public static final double DEFAULT_TOLERANCE = 0.15;

    private final BalancingStrategyRegistry registry;

    public LineBalancer(BalancingStrategyRegistry registry) {
        this.registry = registry;
    }

    public LinePlan balance(Style style, List<Operator> roster, String algorithmKey, int teamSize) {
        return balance(style, roster, algorithmKey, teamSize, DEFAULT_TOLERANCE);
    }

    public LinePlan balance(Style style, List<Operator> roster, String algorithmKey, int teamSize, double tolerance) {
        List<Operation> operations = new ArrayList<>(style.getOperations());
        if (operations.isEmpty()) {
            throw new SimulationException("Style " + style.getCode() + " has no operations to balance");
        }
        if (teamSize < 1) {
            throw new SimulationException("Team size must be at least 1");
        }

        BalancingStrategy strategy = registry.resolve(algorithmKey);
        List<Operation> sequence = strategy.order(operations);

        List<WorkerProfile> pool = new ArrayList<>();
        roster.stream().limit(teamSize).map(WorkerProfile::from).forEach(pool::add);

        double totalSmv = style.totalSmv();
        double targetLoad = totalSmv / teamSize;
        double ceiling = targetLoad * (1 + tolerance);

        List<PlannedStation> stations = new ArrayList<>();
        Set<String> assigned = new HashSet<>();
        PlannedStation current = null;
        int helpers = 0;

        for (Operation operation : sequence) {
            boolean fits = current != null
                    && current.accepts(operation.getMachineType())
                    && current.loadWith(operation) <= ceiling
                    && assigned.containsAll(operation.predecessorCodes());

            if (!fits) {
                if (current != null) {
                    stations.add(current);
                }
                WorkerProfile worker = claimOperator(pool, operation, ++helpers);
                if (!worker.isHelper()) {
                    helpers--;
                }
                current = new PlannedStation(stations.size(), operation.getMachineType(), worker);
            }

            current.assign(operation);
            assigned.add(operation.getCode());
        }
        if (current != null) {
            stations.add(current);
        }

        return new LinePlan(strategy.key(), strategy.displayName(), stations, totalSmv);
    }

    /**
     * Takes the free operator with the highest efficiency on this machine who also
     * meets the grade requirement. When nobody qualifies, a helper is brought in.
     */
    private WorkerProfile claimOperator(List<WorkerProfile> pool, Operation operation, int helperNumber) {
        WorkerProfile best = null;
        double bestEfficiency = Double.NEGATIVE_INFINITY;

        for (WorkerProfile candidate : pool) {
            if (!candidate.canRun(operation)) {
                continue;
            }
            double efficiency = candidate.efficiencyFor(operation.getMachineType());
            if (efficiency > bestEfficiency) {
                bestEfficiency = efficiency;
                best = candidate;
            }
        }

        if (best == null) {
            return WorkerProfile.helper(helperNumber);
        }
        pool.remove(best);
        return best;
    }
}
