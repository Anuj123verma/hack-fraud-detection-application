package com.meridiantrust.sentinel.repository;

import com.meridiantrust.sentinel.domain.AlertAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertAuditLogRepository extends JpaRepository<AlertAuditLog, UUID> {
}
