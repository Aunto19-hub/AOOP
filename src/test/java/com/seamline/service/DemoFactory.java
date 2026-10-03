package com.seamline.service;

import com.seamline.domain.MachineType;
import com.seamline.domain.Operation;
import com.seamline.domain.Operator;
import com.seamline.domain.Style;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small in-memory factory so the unit tests do not need a database. */
final class DemoFactory {

    private DemoFactory() {
    }

    static Style poloStyle() {
        Style style = new Style("POLO-TEST", "Men's Polo Shirt");
        Map<String, Operation> byCode = new LinkedHashMap<>();

        add(style, byCode, "C01", "Cut panel check", 0.30, MachineType.MANUAL, 1, 1);
        add(style, byCode, "S01", "Shoulder join", 0.55, MachineType.OVERLOCK, 3, 2, "C01");
        add(style, byCode, "S02", "Shoulder topstitch", 0.45, MachineType.PLAIN, 3, 3, "S01");
        add(style, byCode, "L01", "Sleeve attach", 0.90, MachineType.OVERLOCK, 4, 4, "S02");
        add(style, byCode, "L02", "Armhole topstitch", 0.60, MachineType.FLATLOCK, 3, 5, "L01");
        add(style, byCode, "S03", "Side seam", 1.10, MachineType.OVERLOCK, 4, 6, "L02");
        add(style, byCode, "H01", "Bottom hem", 0.65, MachineType.PLAIN, 3, 7, "S03");
        add(style, byCode, "T01", "Thread trim", 0.70, MachineType.MANUAL, 1, 8, "H01");
        return style;
    }

    static List<Operator> roster() {
        List<Operator> roster = new ArrayList<>();
        roster.add(operator("OP01", "Rahima", 4, 0.85, MachineType.PLAIN, 1.05, MachineType.OVERLOCK, 0.80));
        roster.add(operator("OP02", "Shirin", 4, 0.80, MachineType.OVERLOCK, 1.10, MachineType.FLATLOCK, 0.90));
        roster.add(operator("OP03", "Nasima", 5, 0.90, MachineType.PLAIN, 1.15, MachineType.MANUAL, 0.95));
        roster.add(operator("OP04", "Parvin", 3, 0.75, MachineType.FLATLOCK, 1.00, MachineType.MANUAL, 1.10));
        roster.add(operator("OP05", "Jesmin", 4, 0.85, MachineType.PLAIN, 0.95, MachineType.OVERLOCK, 1.00));
        roster.add(operator("OP06", "Ruma", 3, 0.70, MachineType.MANUAL, 1.20, MachineType.IRON, 1.00));
        roster.add(operator("OP07", "Sultana", 5, 0.95, MachineType.PLAIN, 1.20, MachineType.FLATLOCK, 0.85));
        roster.add(operator("OP08", "Momena", 4, 0.80, MachineType.OVERLOCK, 1.05, MachineType.PLAIN, 0.90));
        return roster;
    }

    private static void add(Style style, Map<String, Operation> byCode, String code, String name, double smv,
                            MachineType machineType, int grade, int sequenceNo, String... predecessors) {
        Operation operation = new Operation(code, name, smv, machineType, grade, sequenceNo);
        for (String predecessor : predecessors) {
            operation.addPredecessor(byCode.get(predecessor));
        }
        style.addOperation(operation);
        byCode.put(code, operation);
    }

    private static Operator operator(String code, String name, int grade, double defaultEfficiency,
                                     MachineType first, double firstEfficiency,
                                     MachineType second, double secondEfficiency) {
        Operator operator = new Operator(code, name, grade, defaultEfficiency);
        operator.addSkill(first, firstEfficiency);
        operator.addSkill(second, secondEfficiency);
        return operator;
    }
}
