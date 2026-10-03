package com.seamline.service.simulation;

import com.seamline.service.balancing.LinePlan;
import com.seamline.service.balancing.PlannedStation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.springframework.stereotype.Service;

/**
 * Discrete-event simulation of one shift on a balanced line.
 *
 * <p>Time jumps from event to event instead of ticking: a station takes a bundle,
 * the simulator schedules "this bundle is finished at minute X", and nothing is
 * computed in between. Bundles move forward only when the next station has room
 * in its buffer, so blocking and starving emerge from the model rather than being
 * assumed.</p>
 *
 * <p>The service itself holds no mutable state; each run gets its own
 * {@link Execution}, so it is safe to call from concurrent requests.</p>
 */
@Service
public class ShiftSimulator {

    /** Bundles waiting at the head of the line. Large enough never to run dry. */
    private static final int INPUT_BUNDLES = 500;

    private static final double MACHINE_BREAKDOWN_CHANCE = 0.01;
    private static final double REWORK_CHANCE = 0.03;

    public SimulationOutcome run(LinePlan plan, SimulationConfig config) {
        return new Execution(plan, config).execute();
    }

    private enum EventType {
        BUNDLE_FINISHED,
        SHIFT_END
    }

    private record Event(double time, EventType type, int stationIndex, long sequence) {
    }

    private static final class Bundle {
        private final int pieces;

        private Bundle(int pieces) {
            this.pieces = pieces;
        }
    }

    private static final class StationRuntime {
        private final PlannedStation definition;
        private final int capacity;
        private final Deque<Bundle> buffer = new ArrayDeque<>();
        private final Map<StationState, Double> time = new EnumMap<>(StationState.class);
        private Bundle inProgress;
        private Bundle held;
        private StationState state = StationState.STARVED;
        private double lastChange;
        private int bundlesDone;

        private StationRuntime(PlannedStation definition, int capacity) {
            this.definition = definition;
            this.capacity = capacity;
            for (StationState value : StationState.values()) {
                time.put(value, 0d);
            }
        }

        private boolean hasRoom() {
            return buffer.size() < capacity;
        }
    }

    /** One simulation run. Everything mutable lives here. */
    private static final class Execution {

        private final LinePlan plan;
        private final SimulationConfig config;
        private final DeterministicRandom random;
        private final List<StationRuntime> stations = new ArrayList<>();
        private final PriorityQueue<Event> queue = new PriorityQueue<>(
                Comparator.<Event>comparingDouble(Event::time).thenComparingLong(Event::sequence));
        private final List<Integer> hourly = new ArrayList<>();

        private double clock;
        private int pieces;
        private boolean finished;
        private long sequence;

        private Execution(LinePlan plan, SimulationConfig config) {
            this.plan = plan;
            this.config = config;
            this.random = new DeterministicRandom(config.seed());
        }

        private SimulationOutcome execute() {
            setUp();
            drainQueue();
            closeBooks();
            return summarise();
        }

        private void setUp() {
            List<PlannedStation> planned = plan.getStations();
            for (int i = 0; i < planned.size(); i++) {
                int capacity = i == 0 ? Integer.MAX_VALUE : Math.max(1, config.bufferPerStation());
                stations.add(new StationRuntime(planned.get(i), capacity));
            }
            if (stations.isEmpty()) {
                return;
            }
            for (int i = 0; i < INPUT_BUNDLES; i++) {
                stations.get(0).buffer.addLast(new Bundle(config.bundleSize()));
            }
            schedule(config.shiftMinutes(), EventType.SHIFT_END, -1);
            for (int i = 0; i < stations.size(); i++) {
                startNextBundle(i);
            }
        }

        private void drainQueue() {
            while (!queue.isEmpty()) {
                Event event = queue.poll();
                clock = Math.min(event.time(), config.shiftMinutes());
                sampleHourly();
                if (event.type() == EventType.SHIFT_END) {
                    finished = true;
                    break;
                }
                completeBundle(event.stationIndex());
            }
            clock = Math.min(Math.max(clock, 0d), config.shiftMinutes());
        }

        private void schedule(double time, EventType type, int stationIndex) {
            queue.add(new Event(time, type, stationIndex, sequence++));
        }

        private void changeState(StationRuntime station, StationState next) {
            station.time.merge(station.state, clock - station.lastChange, Double::sum);
            station.state = next;
            station.lastChange = clock;
        }

        /** Minutes this station needs for one bundle, including pace variation and upsets. */
        private double workDuration(StationRuntime station, Bundle bundle) {
            double minutes = station.definition.getMinutesPerPiece() * bundle.pieces;
            double pace = 1d + random.nextGaussian() * config.variability();
            minutes *= Math.max(0.6, Math.min(1.6, pace));

            if (random.nextDouble() < MACHINE_BREAKDOWN_CHANCE) {
                minutes += 12d * (0.5 + random.nextDouble());
            }
            if (random.nextDouble() < REWORK_CHANCE) {
                minutes += station.definition.getMinutesPerPiece() * 2d;
            }
            return minutes;
        }

        private void startNextBundle(int index) {
            if (finished) {
                return;
            }
            StationRuntime station = stations.get(index);
            if (station.inProgress != null || station.held != null) {
                return;
            }
            Bundle bundle = station.buffer.pollFirst();
            if (bundle == null) {
                if (station.state != StationState.STARVED) {
                    changeState(station, StationState.STARVED);
                }
                return;
            }
            station.inProgress = bundle;
            changeState(station, StationState.WORKING);
            schedule(clock + workDuration(station, bundle), EventType.BUNDLE_FINISHED, index);
            pullFromUpstream(index);
        }

        /** A station that just freed a buffer slot can unblock the one before it. */
        private void pullFromUpstream(int index) {
            if (index <= 0) {
                return;
            }
            StationRuntime upstream = stations.get(index - 1);
            StationRuntime here = stations.get(index);
            if (upstream.held != null && here.hasRoom()) {
                here.buffer.addLast(upstream.held);
                upstream.held = null;
                changeState(upstream, StationState.IDLE);
                startNextBundle(index);
                startNextBundle(index - 1);
            }
        }

        private void completeBundle(int index) {
            StationRuntime station = stations.get(index);
            Bundle bundle = station.inProgress;
            if (bundle == null) {
                return;
            }
            station.inProgress = null;
            station.bundlesDone++;
            changeState(station, StationState.IDLE);

            if (index == stations.size() - 1) {
                pieces += bundle.pieces;
            } else {
                StationRuntime next = stations.get(index + 1);
                if (next.hasRoom()) {
                    next.buffer.addLast(bundle);
                    startNextBundle(index + 1);
                } else {
                    station.held = bundle;
                    changeState(station, StationState.BLOCKED);
                }
            }
            startNextBundle(index);
        }

        private void sampleHourly() {
            int completedHours = (int) Math.floor(clock / 60d);
            while (hourly.size() < completedHours) {
                hourly.add(pieces);
            }
        }

        private void closeBooks() {
            for (StationRuntime station : stations) {
                station.time.merge(station.state, clock - station.lastChange, Double::sum);
                station.lastChange = clock;
            }
            int expectedHours = Math.max(1, config.shiftMinutes() / 60);
            while (hourly.size() < expectedHours) {
                hourly.add(pieces);
            }
        }

        private SimulationOutcome summarise() {
            double elapsed = Math.max(clock, 0.0001);
            List<StationMetrics> metrics = new ArrayList<>(stations.size());
            double working = 0;
            double blocked = 0;
            double starved = 0;
            double idle = 0;

            for (int i = 0; i < stations.size(); i++) {
                StationRuntime station = stations.get(i);
                PlannedStation definition = station.definition;
                double stationWorking = station.time.get(StationState.WORKING);
                double stationBlocked = station.time.get(StationState.BLOCKED);
                double stationStarved = station.time.get(StationState.STARVED);
                double stationIdle = station.time.get(StationState.IDLE);

                working += stationWorking;
                blocked += stationBlocked;
                starved += stationStarved;
                idle += stationIdle;

                metrics.add(new StationMetrics(
                        i,
                        definition.getCode(),
                        definition.getMachineType(),
                        definition.getOperator().getCode(),
                        definition.getOperator().getName(),
                        definition.operationCodes(),
                        definition.getMinutesPerPiece(),
                        stationWorking / elapsed * 100d,
                        stationWorking,
                        stationBlocked,
                        stationStarved,
                        stationIdle,
                        station.bundlesDone * config.bundleSize()));
            }

            double ratePerHour = pieces / elapsed * 60d;
            double lineEfficiency = stations.isEmpty()
                    ? 0d
                    : plan.getTotalSmv() * pieces / (stations.size() * elapsed) * 100d;

            return new SimulationOutcome(clock, pieces, ratePerHour, lineEfficiency,
                    working, blocked, starved, idle, List.copyOf(hourly), List.copyOf(metrics));
        }
    }
}
