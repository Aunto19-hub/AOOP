package com.seamline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One sewing operation of a style, with its standard time, the machine family it
 * needs, the minimum operator grade, and the operations that must come first.
 * The predecessor links form the precedence graph the balancer walks.
 */
@Entity
@Table(name = "operations")
public class Operation extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    /** Standard minute value: minutes a standard operator needs for one piece. */
    @Column(nullable = false)
    private double smv;

    @Enumerated(EnumType.STRING)
    @Column(name = "machine_type", nullable = false, length = 30)
    private MachineType machineType;

    @Column(name = "min_grade", nullable = false)
    private int minGrade;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "style_id")
    private Style style;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "operation_predecessors",
            joinColumns = @JoinColumn(name = "operation_id"),
            inverseJoinColumns = @JoinColumn(name = "predecessor_id"))
    private Set<Operation> predecessors = new LinkedHashSet<>();

    protected Operation() {
        // required by JPA
    }

    public Operation(String code, String name, double smv, MachineType machineType, int minGrade, int sequenceNo) {
        this.code = code;
        this.name = name;
        this.smv = smv;
        this.machineType = machineType;
        this.minGrade = minGrade;
        this.sequenceNo = sequenceNo;
    }

    public void addPredecessor(Operation predecessor) {
        predecessors.add(predecessor);
    }

    public Set<String> predecessorCodes() {
        return predecessors.stream().map(Operation::getCode).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public double getSmv() {
        return smv;
    }

    public MachineType getMachineType() {
        return machineType;
    }

    public int getMinGrade() {
        return minGrade;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public Style getStyle() {
        return style;
    }

    void setStyle(Style style) {
        this.style = style;
    }

    public Set<Operation> getPredecessors() {
        return predecessors;
    }
}
