package com.meridiantrust.sentinel.service;

import com.meridiantrust.sentinel.domain.Alert;
import com.meridiantrust.sentinel.domain.AmlCase;
import com.meridiantrust.sentinel.domain.CaseAuditLog;
import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.repository.AlertRepository;
import com.meridiantrust.sentinel.repository.AmlCaseRepository;
import com.meridiantrust.sentinel.repository.CaseAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

/**
 * Case management: bundles related Alerts under one investigation, drives
 * status through a small workflow, and writes an immutable audit trail for
 * every transition - same pattern as {@link AlertService}, applied to Cases.
 */
@Service
public class CaseService {

    private static final Set<String> VALID_STATUSES = Set.of("OPEN", "IN_PROGRESS", "ESCALATED", "SAR_FILED", "CLOSED");
    private static final List<String> PRIORITY_RANK = List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private final AmlCaseRepository caseRepository;
    private final AlertRepository alertRepository;
    private final CaseAuditLogRepository auditLogRepository;

    public CaseService(AmlCaseRepository caseRepository, AlertRepository alertRepository, CaseAuditLogRepository auditLogRepository) {
        this.caseRepository = caseRepository;
        this.alertRepository = alertRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AmlCase openCase(List<UUID> alertIds, String actor) {
        List<Alert> alerts = alertIds.stream().map(this::getAlertOrThrow).toList();
        Customer customer = alerts.get(0).getCustomer();
        boolean sameCustomer = alerts.stream().allMatch(a -> a.getCustomer().getId().equals(customer.getId()));
        if (!sameCustomer) {
            throw new IllegalArgumentException("All alerts bundled into a case must belong to the same customer.");
        }

        int maxScore = alerts.stream().mapToInt(Alert::getRiskScore).max().orElse(0);
        AmlCase amlCase = AmlCase.builder()
                .caseNumber(generateCaseNumber())
                .customer(customer)
                .priority(priorityFor(maxScore))
                .build();
        AmlCase saved = caseRepository.save(amlCase);

        alerts.forEach(a -> a.setAmlCase(saved));
        alertRepository.saveAll(alerts);

        writeAudit(saved.getId(), actor, "CASE_OPENED", null, saved.getStatus(),
                "Opened from " + alerts.size() + " alert(s): " + alertIds);
        return saved;
    }

    @Transactional
    public AmlCase linkAlert(UUID caseId, UUID alertId, String actor) {
        AmlCase amlCase = getOrThrow(caseId);
        Alert alert = getAlertOrThrow(alertId);
        if (!alert.getCustomer().getId().equals(amlCase.getCustomer().getId())) {
            throw new IllegalArgumentException("Alert's customer does not match this case's customer.");
        }
        alert.setAmlCase(amlCase);
        alertRepository.save(alert);

        int maxScore = alertRepository.findByAmlCase_Id(caseId).stream().mapToInt(Alert::getRiskScore).max().orElse(0);
        amlCase.setPriority(priorityFor(maxScore));
        AmlCase saved = caseRepository.save(amlCase);

        writeAudit(caseId, actor, "ALERT_LINKED", null, saved.getStatus(), "Linked alert " + alertId);
        return saved;
    }

    @Transactional
    public AmlCase updateStatus(UUID caseId, String newStatus, String reason, String actor) {
        String normalized = newStatus == null ? "" : newStatus.toUpperCase();
        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid case status: " + newStatus + ". Must be one of " + VALID_STATUSES);
        }
        AmlCase amlCase = getOrThrow(caseId);

        boolean requiresReason = normalized.equals("CLOSED") || normalized.equals("SAR_FILED");
        if (requiresReason && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("A reason is required to close or file a SAR for a case.");
        }

        String old = amlCase.getStatus();
        amlCase.setStatus(normalized);
        if (normalized.equals("CLOSED")) {
            amlCase.setClosedAt(LocalDateTime.now());
        }
        AmlCase saved = caseRepository.save(amlCase);

        writeAudit(caseId, actor, "STATUS_CHANGE", old, normalized, reason);
        return saved;
    }

    public List<AmlCase> queue() {
        Comparator<AmlCase> byPriorityDesc = Comparator.comparingInt((AmlCase c) -> PRIORITY_RANK.indexOf(c.getPriority())).reversed();
        return caseRepository.findAll().stream()
                .sorted(byPriorityDesc.thenComparing(Comparator.comparing(AmlCase::getCreatedAt).reversed()))
                .toList();
    }

    public AmlCase getOrThrow(UUID id) {
        return caseRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Case not found: " + id));
    }

    public List<Alert> linkedAlerts(UUID caseId) {
        return alertRepository.findByAmlCase_Id(caseId);
    }

    private Alert getAlertOrThrow(UUID id) {
        return alertRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Alert not found: " + id));
    }

    private String priorityFor(int riskScore) {
        if (riskScore >= 90) return "CRITICAL";
        if (riskScore >= 70) return "HIGH";
        if (riskScore >= 40) return "MEDIUM";
        return "LOW";
    }

    private String generateCaseNumber() {
        long seq = caseRepository.count() + 1;
        return String.format("CASE-%d-%06d", LocalDateTime.now().getYear(), seq);
    }

    private void writeAudit(UUID caseId, String actor, String action, String oldStatus, String newStatus, String reason) {
        auditLogRepository.save(CaseAuditLog.builder()
                .caseId(caseId)
                .actor(actor)
                .action(action)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .reason(reason)
                .build());
    }
}
