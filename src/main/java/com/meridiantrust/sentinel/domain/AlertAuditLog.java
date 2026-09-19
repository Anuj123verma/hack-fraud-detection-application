package com.meridiantrust.sentinel.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Append-only audit trail row. No update/delete is ever issued against this table. */
@Entity
@Table(name = "alert_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertAuditLog {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "alert_id", nullable = false)
    private UUID alertId;

    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private String action;

    @Column(name = "old_status")
    private String oldStatus;

    @Column(name = "new_status")
    private String newStatus;

    @Column(length = 500)
    private String reason;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
