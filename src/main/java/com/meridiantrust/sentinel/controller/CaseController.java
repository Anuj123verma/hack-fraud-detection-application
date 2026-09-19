package com.meridiantrust.sentinel.controller;

import com.meridiantrust.sentinel.domain.AmlCase;
import com.meridiantrust.sentinel.dto.CaseDetailDto;
import com.meridiantrust.sentinel.dto.CaseSummaryDto;
import com.meridiantrust.sentinel.dto.OpenCaseRequest;
import com.meridiantrust.sentinel.dto.UpdateCaseStatusRequest;
import com.meridiantrust.sentinel.security.PiiMasker;
import com.meridiantrust.sentinel.service.CaseService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Case management workflow: bundle alerts, escalate/clear, immutable audit trail. */
@RestController
@RequestMapping("/api/v1/cases")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ANALYST')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a new case from one or more alerts belonging to the same customer.")
    public CaseDetailDto open(@Valid @RequestBody OpenCaseRequest request, Authentication auth) {
        AmlCase saved = caseService.openCase(request.alertIds(), auth.getName());
        return toDetail(saved, isAdmin(auth));
    }

    @PostMapping("/{id}/alerts/{alertId}")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Link an additional alert into an existing case; recomputes priority.")
    public CaseDetailDto linkAlert(@PathVariable UUID id, @PathVariable UUID alertId, Authentication auth) {
        AmlCase saved = caseService.linkAlert(id, alertId, auth.getName());
        return toDetail(saved, isAdmin(auth));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Transition case status. CLOSED/SAR_FILED require a reason (audit requirement).")
    public CaseDetailDto updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateCaseStatusRequest request, Authentication auth) {
        AmlCase saved = caseService.updateStatus(id, request.status(), request.reason(), auth.getName());
        return toDetail(saved, isAdmin(auth));
    }

    @GetMapping
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Case queue, sorted by priority then recency. PII masked.")
    public List<CaseSummaryDto> queue() {
        return caseService.queue().stream().map(this::toSummary).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Case detail with linked alert ids. PII unmasked only for COMPLIANCE_ADMIN.")
    public CaseDetailDto detail(@PathVariable UUID id, Authentication auth) {
        return toDetail(caseService.getOrThrow(id), isAdmin(auth));
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COMPLIANCE_ADMIN"));
    }

    private CaseSummaryDto toSummary(AmlCase c) {
        int linkedCount = caseService.linkedAlerts(c.getId()).size();
        return new CaseSummaryDto(
                c.getId(), c.getCaseNumber(), c.getCustomer().getCustomerRef(),
                PiiMasker.maskName(c.getCustomer().getFullName()), c.getStatus(), c.getPriority(),
                linkedCount, c.getCreatedAt());
    }

    private CaseDetailDto toDetail(AmlCase c, boolean unmask) {
        String name = unmask ? c.getCustomer().getFullName() : PiiMasker.maskName(c.getCustomer().getFullName());
        List<UUID> alertIds = caseService.linkedAlerts(c.getId()).stream().map(a -> a.getId()).toList();
        return new CaseDetailDto(
                c.getId(), c.getCaseNumber(), c.getCustomer().getCustomerRef(), name,
                c.getStatus(), c.getPriority(), alertIds, c.getCreatedAt(), c.getClosedAt());
    }
}
