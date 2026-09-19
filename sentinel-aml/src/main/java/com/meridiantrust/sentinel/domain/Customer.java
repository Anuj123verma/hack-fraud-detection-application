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
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "customer_ref", nullable = false, unique = true)
    private String customerRef;

    /** PII - must be masked in list-view DTOs. */
    @Column(name = "full_name", nullable = false)
    private String fullName;

    /** PII - must be masked in list-view DTOs. */
    @Column(name = "id_number", nullable = false)
    private String idNumber;

    @Column(name = "customer_type", nullable = false)
    private String customerType; // RETAIL | BUSINESS

    @Column(name = "base_risk_rating", nullable = false)
    private String baseRiskRating; // LOW | MEDIUM | HIGH

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
