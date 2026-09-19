package com.meridiantrust.sentinel.dto;

import com.meridiantrust.sentinel.domain.Direction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Read-only view of a transaction, used for the customer timeline. */
public record TransactionDto(
        UUID id,
        String accountNumber,
        Direction direction,
        BigDecimal amount,
        String currency,
        BigDecimal amountBase,
        String counterpartyName,
        String counterpartyJurisdiction,
        String channel,
        LocalDateTime txnTimestamp
) {
}
