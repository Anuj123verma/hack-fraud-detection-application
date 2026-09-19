package com.meridiantrust.sentinel.dto;

import java.util.List;
import java.util.UUID;

public record TransactionIngestResponse(
        UUID transactionId,
        boolean alertTriggered,
        List<String> triggeredRules
) {
}
