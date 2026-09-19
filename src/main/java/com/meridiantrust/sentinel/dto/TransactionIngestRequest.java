package com.meridiantrust.sentinel.dto;

import com.meridiantrust.sentinel.domain.Direction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Request body for POST /api/v1/transactions (single/streaming ingest). */
public record TransactionIngestRequest(
        @NotBlank String accountNumber,
        @NotBlank String externalTxnId,
        @NotNull Direction direction,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank String currency,
        String counterpartyName,
        String counterpartyJurisdiction,
        @NotBlank String channel,
        LocalDateTime txnTimestamp
) {
}
