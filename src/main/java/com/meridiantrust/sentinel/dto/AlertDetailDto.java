package com.meridiantrust.sentinel.dto;

import com.meridiantrust.sentinel.domain.AlertStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Full detail view. {@code customerName}/{@code idNumber} are only
 * populated (unmasked) for callers holding COMPLIANCE_ADMIN - enforced in
 * the service layer, not just by which DTO we choose to return.
 */
public record AlertDetailDto(
        UUID id,
        String customerRef,
        String customerName,
        String idNumber,
        String accountNumber,
        int riskScore,
        AlertStatus status,
        String ruleCodes,
        String evidenceTxnIds,
        String explanation,
        String dispositionReason,
        String disposedBy,
        LocalDateTime createdAt,
        LocalDateTime disposedAt
) {
}
