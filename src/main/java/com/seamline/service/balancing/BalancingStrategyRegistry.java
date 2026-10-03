package com.seamline.service.balancing;

import com.seamline.exception.SimulationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Spring injects every {@link BalancingStrategy} bean here, so adding a new
 * algorithm is one new class and no edits anywhere else (open/closed principle).
 */
@Component
public class BalancingStrategyRegistry {

    private final Map<String, BalancingStrategy> strategies = new LinkedHashMap<>();

    public BalancingStrategyRegistry(List<BalancingStrategy> discovered) {
        discovered.forEach(strategy -> strategies.put(strategy.key().toLowerCase(Locale.ROOT), strategy));
    }

    public BalancingStrategy resolve(String key) {
        BalancingStrategy strategy = strategies.get(key == null ? "" : key.toLowerCase(Locale.ROOT));
        if (strategy == null) {
            throw new SimulationException("Unknown balancing algorithm '" + key + "'. Available: " + strategies.keySet());
        }
        return strategy;
    }

    public List<BalancingStrategy> available() {
        return List.copyOf(strategies.values());
    }
}
