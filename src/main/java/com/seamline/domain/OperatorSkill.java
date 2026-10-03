package com.seamline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** One cell of the skill matrix: how fast an operator is on one machine family. */
@Entity
@Table(name = "operator_skills",
        uniqueConstraints = @UniqueConstraint(columnNames = {"operator_id", "machine_type"}))
public class OperatorSkill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id")
    private Operator operator;

    @Enumerated(EnumType.STRING)
    @Column(name = "machine_type", nullable = false, length = 30)
    private MachineType machineType;

    @Column(nullable = false)
    private double efficiency;

    protected OperatorSkill() {
        // required by JPA
    }

    OperatorSkill(Operator operator, MachineType machineType, double efficiency) {
        this.operator = operator;
        this.machineType = machineType;
        this.efficiency = efficiency;
    }

    public Operator getOperator() {
        return operator;
    }

    public MachineType getMachineType() {
        return machineType;
    }

    public double getEfficiency() {
        return efficiency;
    }
}
