package com.meridiantrust.sentinel.controller;

import com.meridiantrust.sentinel.config.AmlProperties;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only visibility into the currently-effective rule thresholds, so a
 * compliance admin can confirm what's live without grepping application.yml.
 * Tuning itself is done via config/env vars + restart (see README) rather
 * than a full CRUD API, to keep the rule-version audit story simple.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('COMPLIANCE_ADMIN')")
public class AdminController {

    private final AmlProperties properties;

    public AdminController(AmlProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/rule-config")
    @Operation(summary = "View the currently-effective AML rule thresholds/weights.")
    public AmlProperties.Rules currentRuleConfig() {
        return properties.getRules();
    }

    @GetMapping("/currency-config")
    @Operation(summary = "View the currently-effective base currency and exchange rate table.")
    public AmlProperties.Currency currentCurrencyConfig() {
        return properties.getCurrency();
    }
}
