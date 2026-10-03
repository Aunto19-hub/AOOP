package com.seamline.dto;

import com.seamline.domain.RequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OperatorRequestCreateRequest(

        @NotNull(message = "Pick what this is about")
        RequestType type,

        @NotBlank(message = "Description cannot be empty")
        @Size(max = 1000)
        String description) {
}
