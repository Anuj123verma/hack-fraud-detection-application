package com.meridiantrust.sentinel.dto;

import java.util.List;

/** Bulk ingestion never silently drops bad rows - every rejection is reported back with a reason. */
public record IngestionReportDto(
        int totalRows,
        int successCount,
        int errorCount,
        List<IngestionRowError> errors
) {
}
