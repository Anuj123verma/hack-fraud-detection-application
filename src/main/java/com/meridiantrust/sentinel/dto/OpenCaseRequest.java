package com.meridiantrust.sentinel.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/** Opens a new Case by bundling one or more existing Alerts together. */
public record OpenCaseRequest(
        @NotEmpty List<UUID> alertIds
) {
}
