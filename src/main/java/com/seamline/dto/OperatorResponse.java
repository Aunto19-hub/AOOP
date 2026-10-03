package com.seamline.dto;

import com.seamline.domain.Operator;
import com.seamline.domain.OperatorSkill;
import java.util.LinkedHashMap;
import java.util.Map;

/** One row of the skill matrix on the "Operators" screen. */
public record OperatorResponse(String code,
                               String name,
                               int grade,
                               double defaultEfficiency,
                               Map<String, Double> skills) {

    public static OperatorResponse from(Operator operator) {
        Map<String, Double> skills = new LinkedHashMap<>();
        operator.getSkills().forEach(skill -> skills.put(machineKey(skill), skill.getEfficiency()));
        return new OperatorResponse(operator.getCode(), operator.getName(), operator.getGrade(),
                operator.getDefaultEfficiency(), skills);
    }

    private static String machineKey(OperatorSkill skill) {
        return skill.getMachineType().name();
    }
}
