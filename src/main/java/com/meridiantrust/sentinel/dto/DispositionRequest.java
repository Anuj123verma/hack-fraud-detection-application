package com.meridiantrust.sentinel.dto;

import com.meridiantrust.sentinel.domain.AlertStatus;
import jakarta.validation.constraints.NotNull;

public record DispositionRequest(
        @NotNull AlertStatus status,
        String reason
) {
}
