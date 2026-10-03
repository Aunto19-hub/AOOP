package com.seamline.service.balancing;

import com.seamline.domain.Operation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Helgeson-Birnie ranked positional weight. Each operation is weighted by its own
 * SMV plus the SMV of everything that depends on it, so work that holds up the
 * rest of the garment is placed first.
 */
@Component
public class RankedPositionalWeightStrategy extends AbstractBalancingStrategy {

    @Override
    public String key() {
        return "rpw";
    }

    @Override
    public String displayName() {
        return "Ranked positional weight";
    }

    @Override
    public List<Operation> order(List<Operation> operations) {
        List<Operation> topological = topologicalOrder(operations);
        Map<String, Double> weights = positionalWeights(operations, topological);

        Set<String> placed = new HashSet<>();
        List<Operation> remaining = new ArrayList<>(operations);
        List<Operation> ordered = new ArrayList<>(operations.size());

        while (!remaining.isEmpty()) {
            Operation best = null;
            double bestWeight = Double.NEGATIVE_INFINITY;
            for (Operation candidate : remaining) {
                if (!placed.containsAll(candidate.predecessorCodes())) {
                    continue;
                }
                double weight = weights.getOrDefault(candidate.getCode(), 0d);
                if (weight > bestWeight) {
                    bestWeight = weight;
                    best = candidate;
                }
            }
            // A cycle would have been caught by topologicalOrder(), so best is never null here.
            ordered.add(best);
            placed.add(best.getCode());
            remaining.remove(best);
        }
        return ordered;
    }

    /** SMV of the operation plus the SMV of every downstream operation. */
    Map<String, Double> positionalWeights(List<Operation> operations, List<Operation> topological) {
        Map<String, Operation> byCode = index(operations);
        Map<String, List<String>> successors = successorIndex(operations);
        Map<String, Set<String>> descendants = new HashMap<>();

        for (int i = topological.size() - 1; i >= 0; i--) {
            String code = topological.get(i).getCode();
            Set<String> reachable = new LinkedHashSet<>();
            for (String next : successors.getOrDefault(code, List.of())) {
                reachable.add(next);
                reachable.addAll(descendants.getOrDefault(next, Set.of()));
            }
            descendants.put(code, reachable);
        }

        Map<String, Double> weights = new HashMap<>();
        for (Operation operation : operations) {
            double weight = operation.getSmv();
            for (String code : descendants.getOrDefault(operation.getCode(), Set.of())) {
                weight += byCode.get(code).getSmv();
            }
            weights.put(operation.getCode(), weight);
        }
        return weights;
    }
}
