package com.meridiantrust.sentinel.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "external_txn_id", nullable = false, unique = true)
    private String externalTxnId;

    // EAGER for the same reason as Account.customer - see comment there.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    /** Amount normalized to the base currency (INR) at ingestion time. */
    @Column(name = "amount_base", nullable = false)
    private BigDecimal amountBase;

    @Column(name = "counterparty_name")
    private String counterpartyName;

    @Column(name = "counterparty_jurisdiction", length = 2)
    private String counterpartyJurisdiction;

    @Column(nullable = false)
    private String channel;

    @Column(name = "txn_timestamp", nullable = false)
    private LocalDateTime txnTimestamp;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
