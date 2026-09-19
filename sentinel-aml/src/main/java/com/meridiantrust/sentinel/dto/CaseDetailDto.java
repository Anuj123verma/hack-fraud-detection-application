package com.meridiantrust.sentinel.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CaseDetailDto(
        UUID id,
        String caseNumber,
        String customerRef,
        String customerName,
        String status,
        String priority,
        List<UUID> linkedAlertIds,
        LocalDateTime createdAt,
        LocalDateTime closedAt
) {
}
