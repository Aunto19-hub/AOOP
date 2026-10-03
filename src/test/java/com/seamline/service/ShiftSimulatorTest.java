package com.seamline.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.seamline.service.balancing.BalancingStrategyRegistry;
import com.seamline.service.balancing.GreedyTopologicalStrategy;
import com.seamline.service.balancing.LineBalancer;
import com.seamline.service.balancing.LinePlan;
import com.seamline.service.balancing.RankedPositionalWeightStrategy;
import com.seamline.service.simulation.ShiftSimulator;
import com.seamline.service.simulation.SimulationConfig;
import com.seamline.service.simulation.SimulationOutcome;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShiftSimulatorTest {

    private final LineBalancer balancer = new LineBalancer(new BalancingStrategyRegistry(
            List.of(new RankedPositionalWeightStrategy(), new GreedyTopologicalStrategy())));
    private final ShiftSimulator simulator = new ShiftSimulator();

    private LinePlan plan() {
        return balancer.balance(DemoFactory.poloStyle(), DemoFactory.roster(), "rpw", 8);
    }

    @Test
    void aShiftProducesPiecesAndFillsEveryHour() {
        SimulationOutcome outcome = simulator.run(plan(), SimulationConfig.of(480, 3, 0.12, 42L));

        assertThat(outcome.piecesProduced()).isPositive();
        assertThat(outcome.hourlyOutput()).hasSize(8);
        assertThat(outcome.clockMinutes()).isEqualTo(480d);
        assertThat(outcome.outputRatePerHour()).isGreaterThan(0d);
    }

    @Test
    void theSameSeedAlwaysGivesTheSameShift() {
        LinePlan plan = plan();
        SimulationConfig config = SimulationConfig.of(480, 3, 0.12, 42L);

        SimulationOutcome first = simulator.run(plan, config);
        SimulationOutcome second = simulator.run(plan, config);

        assertThat(second.piecesProduced()).isEqualTo(first.piecesProduced());
        assertThat(second.hourlyOutput()).isEqualTo(first.hourlyOutput());
    }

    @Test
    void everyStationMinuteIsAccountedFor() {
        SimulationOutcome outcome = simulator.run(plan(), SimulationConfig.of(480, 3, 0.12, 101L));

        double expected = 480d * outcome.stations().size();
        assertThat(outcome.availableMinutes()).isCloseTo(expected, org.assertj.core.data.Offset.offset(1d));
    }
}
