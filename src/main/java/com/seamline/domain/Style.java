package com.seamline.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/** A garment style (e.g. Men's Polo Shirt) and its operation breakdown. */
@Entity
@Table(name = "styles")
public class Style extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    // LAZY on purpose: this and ShiftRun.stationResults are both List collections,
    // and Hibernate refuses to eagerly fetch two of them in one query.
    @OneToMany(mappedBy = "style", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Operation> operations = new ArrayList<>();

    protected Style() {
        // required by JPA
    }

    public Style(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /** Keeps both sides of the association in sync. */
    public void addOperation(Operation operation) {
        operations.add(operation);
        operation.setStyle(this);
    }

    /** Standard minute value of the whole garment. */
    public double totalSmv() {
        return operations.stream().mapToDouble(Operation::getSmv).sum();
    }

    public int operationCount() {
        return operations.size();
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public List<Operation> getOperations() {
        return operations;
    }
}
