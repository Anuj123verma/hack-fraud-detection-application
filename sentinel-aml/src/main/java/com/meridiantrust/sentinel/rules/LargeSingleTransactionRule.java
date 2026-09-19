package com.meridiantrust.sentinel.rules;

import com.meridiantrust.sentinel.config.AmlProperties;
import com.meridiantrust.sentinel.domain.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Business Rule #1: any single transaction >= threshold (CTR-style, default
 * $10k-equivalent) must be flagged, no exceptions.
 */
@Component
public class LargeSingleTransactionRule implements AmlRule {

    public static final String CODE = "LARGE_SINGLE_TXN";

    private final AmlProperties properties;

    public LargeSingleTransactionRule(AmlProperties properties) {
        this.properties = properties;
    }

    @Override
    public RuleResult evaluate(RuleContext context) {
        AmlProperties.LargeSingleTxn cfg = properties.getRules().getLargeSingleTxn();
        if (!cfg.isEnabled()) {
            return RuleResult.notTriggered();
        }
        Transaction txn = context.current();
        BigDecimal threshold = BigDecimal.valueOf(cfg.getThresholdBase());
        if (txn.getAmountBase().compareTo(threshold) < 0) {
            return RuleResult.notTriggered();
        }
        String explanation = String.format(
                "Single transaction of %s %s (base %.2f) meets/exceeds the CTR-style threshold of %.2f.",
                txn.getCurrency(), txn.getAmount(), txn.getAmountBase(), threshold);
        return new RuleResult(true, CODE, cfg.getWeight(), explanation, java.util.List.of(txn.getId()));
    }
}
