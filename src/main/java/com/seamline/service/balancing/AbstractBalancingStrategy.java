package com.seamline.service.balancing;

import com.seamline.domain.Operation;
import com.seamline.exception.SimulationException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared graph plumbing for the concrete strategies: a topological sort and a
 * successor index. Subclasses only implement the ranking rule.
 */
public abstract class AbstractBalancingStrategy implements BalancingStrategy {

    /** Kahn's algorithm over the precedence graph. Detects cycles. */
    protected List<Operation> topologicalOrder(List<Operation> operations) {
        Map<String, Operation> byCode = index(operations);
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        Map<String, List<String>> successors = successorIndex(operations);

        for (Operation operation : operations) {
            inDegree.put(operation.getCode(), operation.getPredecessors().size());
        }

        Deque<String> ready = new ArrayDeque<>();
        for (Operation operation : operations) {
            if (inDegree.get(operation.getCode()) == 0) {
                ready.addLast(operation.getCode());
            }
        }

        List<Operation> ordered = new ArrayList<>(operations.size());
        while (!ready.isEmpty()) {
            String code = ready.pollFirst();
            ordered.add(byCode.get(code));
            for (String next : successors.getOrDefault(code, List.of())) {
                int remaining = inDegree.merge(next, -1, Integer::sum);
                if (remaining == 0) {
                    ready.addLast(next);
                }
            }
        }

        if (ordered.size() != operations.size()) {
            throw new SimulationException("The precedence graph contains a cycle; operations cannot be sequenced");
        }
        return ordered;
    }

    protected Map<String, List<String>> successorIndex(List<Operation> operations) {
        Map<String, List<String>> successors = new HashMap<>();
        operations.forEach(operation -> successors.put(operation.getCode(), new ArrayList<>()));
        for (Operation operation : operations) {
            for (String predecessor : operation.predecessorCodes()) {
                successors.computeIfAbsent(predecessor, key -> new ArrayList<>()).add(operation.getCode());
            }
        }
        return successors;
    }

    protected Map<String, Operation> index(List<Operation> operations) {
        Map<String, Operation> byCode = new LinkedHashMap<>();
        operations.forEach(operation -> byCode.put(operation.getCode(), operation));
        return byCode;
    }
}
