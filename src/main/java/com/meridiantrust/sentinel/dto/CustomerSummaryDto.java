package com.meridiantrust.sentinel.dto;

import java.util.UUID;

public record CustomerSummaryDto(
        UUID id,
        String customerRef,
        String maskedName,
        String customerType,
        String baseRiskRating
) {
}
