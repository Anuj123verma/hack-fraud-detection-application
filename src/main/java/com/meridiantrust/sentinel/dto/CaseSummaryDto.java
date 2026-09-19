package com.meridiantrust.sentinel.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CaseSummaryDto(
        UUID id,
        String caseNumber,
        String customerRef,
        String maskedCustomerName,
        String status,
        String priority,
        int linkedAlertCount,
        LocalDateTime createdAt
) {
}
