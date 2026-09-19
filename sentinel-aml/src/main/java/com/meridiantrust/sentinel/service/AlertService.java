package com.meridiantrust.sentinel.service;

import com.meridiantrust.sentinel.domain.*;
import com.meridiantrust.sentinel.repository.AlertAuditLogRepository;
import com.meridiantrust.sentinel.repository.AlertRepository;
import com.meridiantrust.sentinel.rules.RiskScorer;
import com.meridiantrust.sentinel.rules.RuleResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Creates and de-duplicates Alerts, and enforces that disposition changes
 * always carry a reason + actor (never a silent delete), writing an
 * immutable audit trail entry for every state change.
 */
@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertAuditLogRepository auditLogRepository;

    public AlertService(AlertRepository alertRepository, AlertAuditLogRepository auditLogRepository) {
        this.alertRepository = alertRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public Alert recordDetectionResult(Customer customer, Account account, Transaction current, List<RuleResult> triggered) {
        String ruleCodes = triggered.stream().map(RuleResult::ruleCode).distinct().sorted().collect(Collectors.joining(","));
        String newEvidence = triggered.stream()
                .flatMap(r -> r.evidenceTxnIds().stream())
                .distinct()
                .map(UUID::toString)
                .collect(Collectors.joining(","));
        String newExplanation = triggered.stream().map(RuleResult::explanation).collect(Collectors.joining(" | "));
        int riskScore = RiskScorer.combine(triggered);

        // Dedup bucket: same customer + account + rule-set + calendar day -> one alert, not fifty.
        String dedupKey = String.join("|", customer.getId().toString(), account.getId().toString(),
                ruleCodes, current.getTxnTimestamp().toLocalDate().toString());

        Optional<Alert> existingOpt = alertRepository.findByDedupKey(dedupKey);
        boolean isNew = existingOpt.isEmpty();
        Alert alert = existingOpt.orElseGet(() -> Alert.builder()
                .customer(customer)
                .account(account)
                .dedupKey(dedupKey)
                .ruleCodes(ruleCodes)
                .evidenceTxnIds("")
                .explanation("")
                .riskScore(0)
                .build());

        alert.setRiskScore(Math.max(alert.getRiskScore(), riskScore));
        alert.setEvidenceTxnIds(mergeCsv(alert.getEvidenceTxnIds(), newEvidence));
        alert.setExplanation(isNew ? newExplanation : alert.getExplanation() + " || " + newExplanation);

        Alert saved = alertRepository.save(alert);

        auditLogRepository.save(AlertAuditLog.builder()
                .alertId(saved.getId())
                .actor("SYSTEM/DetectionEngine")
                .action(isNew ? "ALERT_CREATED" : "ALERT_AGGREGATED")
                .newStatus(saved.getStatus().name())
                .build());

        return saved;
    }

    @Transactional
    public Alert disposition(UUID alertId, AlertStatus newStatus, String reason, String actor) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NoSuchElementException("Alert not found: " + alertId));

        boolean requiresReason = newStatus == AlertStatus.CLEARED || newStatus == AlertStatus.CLOSED;
        if (requiresReason && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("A disposition_reason is required to clear or close an alert.");
        }

        String oldStatus = alert.getStatus().name();
        alert.setStatus(newStatus);
        alert.setDispositionReason(reason);
        alert.setDisposedBy(actor);
        alert.setDisposedAt(LocalDateTime.now());
        Alert saved = alertRepository.save(alert);

        auditLogRepository.save(AlertAuditLog.builder()
                .alertId(alertId)
                .actor(actor)
                .action("STATUS_CHANGE")
                .oldStatus(oldStatus)
                .newStatus(newStatus.name())
                .reason(reason)
                .build());

        return saved;
    }

    public List<Alert> queue() {
        return alertRepository.findAllByOrderByRiskScoreDescCreatedAtDesc();
    }

    public Alert getOrThrow(UUID id) {
        return alertRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Alert not found: " + id));
    }

    private String mergeCsv(String existing, String incoming) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (existing != null && !existing.isBlank()) set.addAll(Arrays.asList(existing.split(",")));
        if (incoming != null && !incoming.isBlank()) set.addAll(Arrays.asList(incoming.split(",")));
        return String.join(",", set);
    }
}
