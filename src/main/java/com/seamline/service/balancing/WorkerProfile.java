package com.seamline.service.balancing;

import com.seamline.domain.MachineType;
import com.seamline.domain.Operation;
import com.seamline.domain.Operator;
import java.util.EnumMap;
import java.util.Map;

/**
 * An immutable snapshot of an operator used during planning. Working on a copy
 * keeps the persistent {@link Operator} out of the algorithm, and lets the
 * balancer invent "Helper" workers when nobody on the roster is qualified.
 */
public final class WorkerProfile {

    private final String code;
    private final String name;
    private final int grade;
    private final double defaultEfficiency;
    private final Map<MachineType, Double> skills;
    private final boolean helper;

    private WorkerProfile(String code, String name, int grade, double defaultEfficiency,
                          Map<MachineType, Double> skills, boolean helper) {
        this.code = code;
        this.name = name;
        this.grade = grade;
        this.defaultEfficiency = defaultEfficiency;
        this.skills = skills;
        this.helper = helper;
    }

    public static WorkerProfile from(Operator operator) {
        Map<MachineType, Double> skills = new EnumMap<>(MachineType.class);
        operator.getSkills().forEach(skill -> skills.put(skill.getMachineType(), skill.getEfficiency()));
        return new WorkerProfile(operator.getCode(), operator.getName(), operator.getGrade(),
                operator.getDefaultEfficiency(), skills, false);
    }

    /** Fallback worker pulled in when the roster cannot cover an operation. */
    public static WorkerProfile helper(int number) {
        return new WorkerProfile("H" + number, "Helper " + number, 3, 0.70,
                new EnumMap<>(MachineType.class), true);
    }

    public double efficiencyFor(MachineType machineType) {
        return skills.getOrDefault(machineType, defaultEfficiency);
    }

    public boolean canRun(Operation operation) {
        return grade >= operation.getMinGrade();
    }

    /** Minutes this worker needs for one piece of the given operation. */
    public double minutesFor(Operation operation) {
        return operation.getSmv() / Math.max(0.05, efficiencyFor(operation.getMachineType()));
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getGrade() {
        return grade;
    }

    public boolean isHelper() {
        return helper;
    }
}
