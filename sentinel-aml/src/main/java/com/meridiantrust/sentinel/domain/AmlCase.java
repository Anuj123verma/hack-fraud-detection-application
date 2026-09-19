package com.meridiantrust.sentinel.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "aml_case")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmlCase {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "case_number", nullable = false, unique = true)
    private String caseNumber;

    // EAGER for the same reason as Account.customer - see comment there.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Builder.Default
    @Column(nullable = false)
    private String status = "OPEN"; // OPEN, IN_PROGRESS, ESCALATED, SAR_FILED, CLOSED

    @Builder.Default
    @Column(nullable = false)
    private String priority = "LOW"; // LOW, MEDIUM, HIGH, CRITICAL

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
