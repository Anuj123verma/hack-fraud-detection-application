package com.meridiantrust.sentinel.dto;

public record IngestionRowError(
        int rowNumber,
        String rawLine,
        String reason
) {
}
