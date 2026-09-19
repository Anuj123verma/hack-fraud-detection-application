package com.meridiantrust.sentinel.dto;

import jakarta.validation.constraints.NotBlank;

/** Case statuses are plain strings (OPEN, IN_PROGRESS, ESCALATED, SAR_FILED, CLOSED). */
public record UpdateCaseStatusRequest(
        @NotBlank String status,
        String reason
) {
}
