package com.seamline.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/** One row of the "Add operator" form on the Operators screen. */
public record AddOperatorRequest(

        @NotBlank(message = "Operator code is required")
        String code,

        @NotBlank(message = "Operator name is required")
        String name,

        @NotNull(message = "Grade is required")
        @Min(value = 1, message = "Grade must be at least 1")
        @Max(value = 5, message = "Grade must be 5 or less")
        Integer grade,

        @NotNull(message = "Default efficiency is required")
        @DecimalMin(value = "0.01", message = "Default efficiency must be greater than zero")
        @DecimalMax(value = "2.0", message = "Default efficiency must be 2.0 or less")
        Double defaultEfficiency,

        Map<String, Double> skills) {

    public Map<String, Double> skillsOrEmpty() {
        return skills == null ? Map.of() : skills;
    }
}
