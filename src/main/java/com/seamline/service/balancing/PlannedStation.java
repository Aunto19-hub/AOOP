package com.seamline.service.balancing;

import com.seamline.domain.MachineType;
import com.seamline.domain.Operation;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** One station of a balanced line: a machine, an operator and the operations they own. */
public class PlannedStation {

    private final int index;
    private final MachineType machineType;
    private final WorkerProfile operator;
    private final List<Operation> operations = new ArrayList<>();
    private double minutesPerPiece;

    PlannedStation(int index, MachineType machineType, WorkerProfile operator) {
        this.index = index;
        this.machineType = machineType;
        this.operator = operator;
    }

    void assign(Operation operation) {
        operations.add(operation);
        minutesPerPiece += operator.minutesFor(operation);
    }

    /** What the load would become if this operation were added. */
    double loadWith(Operation operation) {
        return minutesPerPiece + operator.minutesFor(operation);
    }

    public boolean accepts(MachineType type) {
        return machineType == type;
    }

    public String getCode() {
        return "ST" + (index + 1);
    }

    public String operationCodes() {
        return operations.stream().map(Operation::getCode).collect(Collectors.joining("+"));
    }

    public String operationNames() {
        return operations.stream().map(Operation::getName).collect(Collectors.joining(" + "));
    }

    public int getIndex() {
        return index;
    }

    public MachineType getMachineType() {
        return machineType;
    }

    public WorkerProfile getOperator() {
        return operator;
    }

    public List<Operation> getOperations() {
        return List.copyOf(operations);
    }

    public double getMinutesPerPiece() {
        return minutesPerPiece;
    }
}
