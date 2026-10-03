package com.seamline.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A sewing operator. Speed is not a single number: an operator can be fast on an
 * overlock and slow on a plain machine, which is exactly why station placement
 * changes line output.
 */
@Entity
@Table(name = "operators")
public class Operator extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private int grade;

    /** Efficiency used for machines with no explicit skill entry. 1.00 = standard pace. */
    @Column(name = "default_efficiency", nullable = false)
    private double defaultEfficiency;

    @OneToMany(mappedBy = "operator", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<OperatorSkill> skills = new LinkedHashSet<>();

    protected Operator() {
        // required by JPA
    }

    public Operator(String code, String name, int grade, double defaultEfficiency) {
        this.code = code;
        this.name = name;
        this.grade = grade;
        this.defaultEfficiency = defaultEfficiency;
    }

    public void addSkill(MachineType machineType, double efficiency) {
        skills.add(new OperatorSkill(this, machineType, efficiency));
    }

    /** Behaviour lives with the data: ask the operator how fast they are here. */
    public double efficiencyFor(MachineType machineType) {
        return skills.stream()
                .filter(skill -> skill.getMachineType() == machineType)
                .mapToDouble(OperatorSkill::getEfficiency)
                .findFirst()
                .orElse(defaultEfficiency);
    }

    public boolean canRun(Operation operation) {
        return grade >= operation.getMinGrade();
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

    public double getDefaultEfficiency() {
        return defaultEfficiency;
    }

    public Set<OperatorSkill> getSkills() {
        return skills;
    }
}
