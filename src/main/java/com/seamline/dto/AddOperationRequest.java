package com.seamline.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** One row of the "Add operation" form on the Style breakdown screen. */
public record AddOperationRequest(

        @NotBlank(message = "Operation code is required")
        String code,

        @NotBlank(message = "Operation name is required")
        String name,

        @NotNull(message = "SMV is required")
        @DecimalMin(value = "0.01", message = "SMV must be greater than zero")
        @DecimalMax(value = "10.0", message = "SMV must be 10.0 or less")
        Double smv,

        @NotBlank(message = "Machine type is required")
        String machine,

        @NotNull(message = "Minimum grade is required")
        @Min(value = 1, message = "Minimum grade must be at least 1")
        @Max(value = 5, message = "Minimum grade must be 5 or less")
        Integer minGrade,

        List<String> predecessors) {

    public List<String> predecessorsOrEmpty() {
        return predecessors == null ? List.of() : predecessors;
    }
}
