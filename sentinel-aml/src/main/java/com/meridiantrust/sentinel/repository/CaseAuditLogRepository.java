package com.meridiantrust.sentinel.repository;

import com.meridiantrust.sentinel.domain.CaseAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CaseAuditLogRepository extends JpaRepository<CaseAuditLog, UUID> {
}
