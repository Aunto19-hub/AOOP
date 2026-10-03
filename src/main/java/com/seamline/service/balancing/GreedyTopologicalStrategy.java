package com.seamline.service.balancing;

import com.seamline.domain.Operation;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The naive baseline: take operations in plain topological order, which is
 * roughly how a supervisor lays out a line by hand.
 */
@Component
public class GreedyTopologicalStrategy extends AbstractBalancingStrategy {

    @Override
    public String key() {
        return "greedy";
    }

    @Override
    public String displayName() {
        return "Greedy topological fill";
    }

    @Override
    public List<Operation> order(List<Operation> operations) {
        return topologicalOrder(operations);
    }
}
