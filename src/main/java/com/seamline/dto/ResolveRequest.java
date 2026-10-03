package com.seamline.dto;

import jakarta.validation.constraints.Size;

/** {@code note} is optional — not every closed request needs an explanation. */
public record ResolveRequest(@Size(max = 1000) String note) {
}
