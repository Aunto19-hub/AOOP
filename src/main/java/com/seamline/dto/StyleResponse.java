package com.seamline.dto;

import com.seamline.domain.Operation;
import com.seamline.domain.Style;
import java.util.Comparator;
import java.util.List;

/**
 * A style with its full operation bulletin — this is what the Industrial
 * Engineer's "Style breakdown" screen draws, table and precedence graph alike.
 */
public record StyleResponse(String code,
                            String name,
                            int operationCount,
                            double totalSmv,
                            List<OperationResponse> operations) {

    public record OperationResponse(String code,
                                    String name,
                                    double smv,
                                    String machine,
                                    String machineLabel,
                                    int minGrade,
                                    int sequenceNo,
                                    List<String> predecessors) {

        public static OperationResponse from(Operation operation) {
            return new OperationResponse(
                    operation.getCode(),
                    operation.getName(),
                    operation.getSmv(),
                    operation.getMachineType().name(),
                    operation.getMachineType().getLabel(),
                    operation.getMinGrade(),
                    operation.getSequenceNo(),
                    List.copyOf(operation.predecessorCodes()));
        }
    }

    public static StyleResponse from(Style style) {
        List<OperationResponse> operations = style.getOperations().stream()
                .sorted(Comparator.comparingInt(Operation::getSequenceNo))
                .map(OperationResponse::from)
                .toList();

        return new StyleResponse(style.getCode(), style.getName(),
                style.operationCount(), round(style.totalSmv()), operations);
    }

    private static double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
