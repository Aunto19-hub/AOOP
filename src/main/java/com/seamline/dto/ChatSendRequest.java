package com.seamline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatSendRequest(

        @NotBlank(message = "Message cannot be empty")
        @Size(max = 2000)
        String body) {
}
