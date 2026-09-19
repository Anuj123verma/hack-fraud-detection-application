package com.meridiantrust.sentinel.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An Alert is the output of the detection engine: one or more rules fired
 * for a customer/account, backed by concrete evidence transactions.
 * Rows here are append-only from the app's perspective - disposition just
 * moves {@link #status} forward and records who/why.
 */
@Entity
@Table(name = "alert")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    // EAGER: read-only controllers map Alert -> DTO after the repository call
    // returns (open-in-view is off); see Account.customer for the full reasoning.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "case_id")
    private AmlCase amlCase;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private AlertStatus status = AlertStatus.NEW;

    /** Drives DB-level de-duplication: customer + rule + time-bucket. */
    @Column(name = "dedup_key", nullable = false, unique = true)
    private String dedupKey;

    /** Comma-separated rule codes that contributed, e.g. "STRUCTURING,LARGE_SINGLE_TXN". */
    @Column(name = "rule_codes", nullable = false)
    private String ruleCodes;

    /** Comma-separated transaction IDs that constitute the evidence. */
    @Column(name = "evidence_txn_ids", nullable = false, length = 2000)
    private String evidenceTxnIds;

    @Column(nullable = false, length = 2000)
    private String explanation;

    @Column(name = "disposition_reason", length = 500)
    private String dispositionReason;

    @Column(name = "disposed_by")
    private String disposedBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "disposed_at")
    private LocalDateTime disposedAt;
}
