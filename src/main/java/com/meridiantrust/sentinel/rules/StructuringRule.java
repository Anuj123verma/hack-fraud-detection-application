package com.meridiantrust.sentinel.rules;

import com.meridiantrust.sentinel.config.AmlProperties;
import com.meridiantrust.sentinel.domain.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Business Rule #2: 3+ transactions on the same account, each individually
 * between minAmount and maxAmount (the "just under the reporting threshold"
 * smurfing band), within a rolling window -> Structuring alert.
 */
@Component
public class StructuringRule implements AmlRule {

    public static final String CODE = "STRUCTURING";

    private final AmlProperties properties;

    public StructuringRule(AmlProperties properties) {
        this.properties = properties;
    }

    @Override
    public RuleResult evaluate(RuleContext context) {
        AmlProperties.Structuring cfg = properties.getRules().getStructuring();
        if (!cfg.isEnabled()) {
            return RuleResult.notTriggered();
        }
        Transaction current = context.current();
        BigDecimal min = BigDecimal.valueOf(cfg.getMinAmount());
        BigDecimal max = BigDecimal.valueOf(cfg.getMaxAmount());

        // Only worth checking when the triggering txn itself is in the smurfing band.
        if (current.getAmountBase().compareTo(min) < 0 || current.getAmountBase().compareTo(max) > 0) {
            return RuleResult.notTriggered();
        }

        LocalDateTime windowStart = current.getTxnTimestamp().minusHours(cfg.getWindowHours());

        List<Transaction> qualifying = context.accountHistory().stream()
                .filter(t -> !t.getTxnTimestamp().isBefore(windowStart))
                .filter(t -> !t.getTxnTimestamp().isAfter(current.getTxnTimestamp()))
                .filter(t -> t.getAmountBase().compareTo(min) >= 0 && t.getAmountBase().compareTo(max) <= 0)
                .toList();

        if (qualifying.size() < cfg.getMinCount()) {
            return RuleResult.notTriggered();
        }

        BigDecimal total = qualifying.stream().map(Transaction::getAmountBase).reduce(BigDecimal.ZERO, BigDecimal::add);
        int score = Math.min(100, cfg.getWeight() + 10 * (qualifying.size() - cfg.getMinCount()));
        String explanation = String.format(
                "%d transactions between %.2f-%.2f totalling %.2f occurred on this account within a %dh window - classic structuring pattern.",
                qualifying.size(), cfg.getMinAmount(), cfg.getMaxAmount(), total, cfg.getWindowHours());

        return new RuleResult(true, CODE, score, explanation, qualifying.stream().map(Transaction::getId).toList());
    }
}
