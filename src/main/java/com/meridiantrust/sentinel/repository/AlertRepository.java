package com.meridiantrust.sentinel.repository;

import com.meridiantrust.sentinel.domain.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
    Optional<Alert> findByDedupKey(String dedupKey);
    List<Alert> findAllByOrderByRiskScoreDescCreatedAtDesc();
    List<Alert> findByAmlCase_Id(UUID caseId);
}
