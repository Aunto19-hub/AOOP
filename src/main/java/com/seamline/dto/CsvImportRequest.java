package com.seamline.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of the "Import from CSV" upload — the file's text, read client-side. */
public record CsvImportRequest(

        @NotBlank(message = "CSV content is required")
        String csv) {
}
