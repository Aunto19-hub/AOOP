package com.seamline.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A new production line. {@code styleCode} is optional — a line can sit idle with no style assigned yet. */
public record CreateLineRequest(

        @NotBlank(message = "Give the line a code, e.g. Line-21")
        @Size(max = 40)
        String code,

        @Size(max = 60)
        String unitName,

        @Min(value = 1, message = "Target rate must be at least 1 piece/hour")
        int targetRatePerHour,

        String styleCode) {
}
