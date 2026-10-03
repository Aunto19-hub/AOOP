package com.seamline.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.seamline.domain.Operation;
import com.seamline.domain.Style;
import com.seamline.service.balancing.BalancingStrategyRegistry;
import com.seamline.service.balancing.GreedyTopologicalStrategy;
import com.seamline.service.balancing.LineBalancer;
import com.seamline.service.balancing.LinePlan;
import com.seamline.service.balancing.PlannedStation;
import com.seamline.service.balancing.RankedPositionalWeightStrategy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LineBalancerTest {

    private final LineBalancer balancer = new LineBalancer(new BalancingStrategyRegistry(
            List.of(new RankedPositionalWeightStrategy(), new GreedyTopologicalStrategy())));

    @Test
    void everyOperationIsPlacedExactlyOnce() {
        Style style = DemoFactory.poloStyle();

        LinePlan plan = balancer.balance(style, DemoFactory.roster(), "rpw", 8);

        List<String> placed = new ArrayList<>();
        plan.getStations().forEach(station ->
                station.getOperations().forEach(operation -> placed.add(operation.getCode())));

        assertThat(placed).hasSize(style.operationCount());
        assertThat(new HashSet<>(placed)).hasSize(style.operationCount());
    }

    @Test
    void precedenceIsNeverViolated() {
        LinePlan plan = balancer.balance(DemoFactory.poloStyle(), DemoFactory.roster(), "rpw", 8);

        Set<String> seen = new HashSet<>();
        for (PlannedStation station : plan.getStations()) {
            for (Operation operation : station.getOperations()) {
                assertThat(seen).containsAll(operation.predecessorCodes());
                seen.add(operation.getCode());
            }
        }
    }

    @Test
    void cycleTimeAndBalanceLossAreReported() {
        LinePlan plan = balancer.balance(DemoFactory.poloStyle(), DemoFactory.roster(), "greedy", 8);

        assertThat(plan.getStations()).isNotEmpty();
        assertThat(plan.getCycleTimeMinutes()).isGreaterThan(0d);
        assertThat(plan.getBalanceLossPercent()).isBetween(0d, 100d);
        assertThat(plan.theoreticalRatePerHour()).isGreaterThan(0d);
    }

    @Test
    void aStationNeverMixesMachineTypes() {
        LinePlan plan = balancer.balance(DemoFactory.poloStyle(), DemoFactory.roster(), "rpw", 8);

        for (PlannedStation station : plan.getStations()) {
            assertThat(station.getOperations())
                    .allMatch(operation -> operation.getMachineType() == station.getMachineType());
        }
    }
}
