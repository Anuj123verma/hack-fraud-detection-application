package com.meridiantrust.sentinel.rules;

import com.meridiantrust.sentinel.config.AmlProperties;
import com.meridiantrust.sentinel.domain.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Business Rule #4: any transaction touching a counterparty jurisdiction on
 * the configurable high-risk/sanctions list must always alert, regardless of
 * amount.
 */
@Component
public class HighRiskJurisdictionRule implements AmlRule {

    public static final String CODE = "HIGH_RISK_JURISDICTION";

    private final AmlProperties properties;

    public HighRiskJurisdictionRule(AmlProperties properties) {
        this.properties = properties;
    }

    @Override
    public RuleResult evaluate(RuleContext context) {
        AmlProperties.HighRiskJurisdiction cfg = properties.getRules().getHighRiskJurisdiction();
        if (!cfg.isEnabled()) {
            return RuleResult.notTriggered();
        }
        Transaction txn = context.current();
        String jurisdiction = txn.getCounterpartyJurisdiction();
        if (jurisdiction == null || !cfg.getJurisdictions().contains(jurisdiction)) {
            return RuleResult.notTriggered();
        }
        String explanation = String.format(
                "Transaction involves counterparty jurisdiction '%s', which is on the configured high-risk/sanctions list.",
                jurisdiction);
        return new RuleResult(true, CODE, cfg.getWeight(), explanation, List.of(txn.getId()));
    }
}
