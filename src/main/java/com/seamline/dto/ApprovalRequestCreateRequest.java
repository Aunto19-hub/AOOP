package com.seamline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApprovalRequestCreateRequest(

        @NotBlank(message = "Describe the change being proposed")
        @Size(max = 200)
        String title,

        @NotBlank(message = "Describe the projected impact")
        @Size(max = 300)
        String projectedImpact) {
}
