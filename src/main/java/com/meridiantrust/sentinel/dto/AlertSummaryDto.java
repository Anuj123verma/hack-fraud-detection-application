package com.meridiantrust.sentinel.dto;

import com.meridiantrust.sentinel.domain.AlertStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** Masked view for the analyst queue - PII stays hidden until someone opens the detail view. */
public record AlertSummaryDto(
        UUID id,
        String customerRef,
        String maskedCustomerName,
        String accountNumber,
        int riskScore,
        AlertStatus status,
        String ruleCodes,
        LocalDateTime createdAt
) {
}
