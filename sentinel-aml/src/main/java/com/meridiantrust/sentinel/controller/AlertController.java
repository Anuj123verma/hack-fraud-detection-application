package com.meridiantrust.sentinel.controller;

import com.meridiantrust.sentinel.domain.Alert;
import com.meridiantrust.sentinel.dto.AlertDetailDto;
import com.meridiantrust.sentinel.dto.AlertSummaryDto;
import com.meridiantrust.sentinel.dto.DispositionRequest;
import com.meridiantrust.sentinel.security.PiiMasker;
import com.meridiantrust.sentinel.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Alert queue + detail + disposition. RBAC and PII masking enforced here, server-side. */
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Alert queue sorted by risk score descending. Customer PII is always masked here.")
    public List<AlertSummaryDto> queue() {
        return alertService.queue().stream().map(this::toSummary).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Alert detail with full evidence/explanation. PII only unmasked for COMPLIANCE_ADMIN.")
    public AlertDetailDto detail(@PathVariable UUID id, Authentication auth) {
        Alert alert = alertService.getOrThrow(id);
        boolean unmask = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COMPLIANCE_ADMIN"));
        return toDetail(alert, unmask);
    }

    @PatchMapping("/{id}/disposition")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Change alert status. CLEARED/CLOSED require a disposition reason (audit requirement).")
    public AlertDetailDto disposition(@PathVariable UUID id, @Valid @RequestBody DispositionRequest request, Authentication auth) {
        Alert saved = alertService.disposition(id, request.status(), request.reason(), auth.getName());
        boolean unmask = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COMPLIANCE_ADMIN"));
        return toDetail(saved, unmask);
    }

    private AlertSummaryDto toSummary(Alert a) {
        return new AlertSummaryDto(
                a.getId(),
                a.getCustomer().getCustomerRef(),
                PiiMasker.maskName(a.getCustomer().getFullName()),
                a.getAccount() != null ? a.getAccount().getAccountNumber() : null,
                a.getRiskScore(),
                a.getStatus(),
                a.getRuleCodes(),
                a.getCreatedAt());
    }

    private AlertDetailDto toDetail(Alert a, boolean unmask) {
        String name = unmask ? a.getCustomer().getFullName() : PiiMasker.maskName(a.getCustomer().getFullName());
        String idNumber = unmask ? a.getCustomer().getIdNumber() : PiiMasker.maskIdNumber(a.getCustomer().getIdNumber());
        return new AlertDetailDto(
                a.getId(),
                a.getCustomer().getCustomerRef(),
                name,
                idNumber,
                a.getAccount() != null ? a.getAccount().getAccountNumber() : null,
                a.getRiskScore(),
                a.getStatus(),
                a.getRuleCodes(),
                a.getEvidenceTxnIds(),
                a.getExplanation(),
                a.getDispositionReason(),
                a.getDisposedBy(),
                a.getCreatedAt(),
                a.getDisposedAt());
    }
}
