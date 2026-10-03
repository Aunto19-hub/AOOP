package com.seamline.service.balancing;

import com.seamline.domain.Operation;
import java.util.List;

/**
 * Strategy pattern: the rule that decides in which order operations are offered
 * to the stations. Swapping the implementation changes the whole line layout,
 * which is exactly what the Scenarios screen compares.
 */
public interface BalancingStrategy {

    /** Stable key used by the API, e.g. "rpw". */
    String key();

    /** Human label for the UI, e.g. "Ranked positional weight". */
    String displayName();

    /**
     * Returns every operation exactly once, in an order that never places an
     * operation before one of its predecessors.
     */
    List<Operation> order(List<Operation> operations);
}
