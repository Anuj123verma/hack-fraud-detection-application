package com.meridiantrust.sentinel.rules;

import com.meridiantrust.sentinel.config.AmlProperties;
import com.meridiantrust.sentinel.domain.Direction;
import com.meridiantrust.sentinel.domain.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Business Rule #3: funds credited into an account, then >= minPercentOut of
 * that value debited back out within windowHours -> Rapid Movement (layering)
 * alert. Evaluated reactively on each new DEBIT, looking backward for the
 * originating CREDIT(s).
 */
@Component
public class RapidMovementRule implements AmlRule {

    public static final String CODE = "RAPID_MOVEMENT";

    private final AmlProperties properties;

    public RapidMovementRule(AmlProperties properties) {
        this.properties = properties;
    }

    @Override
    public RuleResult evaluate(RuleContext context) {
        AmlProperties.RapidMovement cfg = properties.getRules().getRapidMovement();
        if (!cfg.isEnabled()) {
            return RuleResult.notTriggered();
        }
        Transaction current = context.current();
        if (current.getDirection() != Direction.DEBIT) {
            return RuleResult.notTriggered();
        }

        List<Transaction> history = context.accountHistory();

        for (Transaction credit : history) {
            if (credit.getDirection() != Direction.CREDIT) continue;
            if (credit.getTxnTimestamp().isAfter(current.getTxnTimestamp())) continue;
            Duration sinceCredit = Duration.between(credit.getTxnTimestamp(), current.getTxnTimestamp());
            if (sinceCredit.toHours() > cfg.getWindowHours()) continue;

            LocalDateTime windowEnd = credit.getTxnTimestamp().plusHours(cfg.getWindowHours());
            List<Transaction> outboundInWindow = new ArrayList<>();
            BigDecimal outboundSum = BigDecimal.ZERO;
            for (Transaction t : history) {
                if (t.getDirection() != Direction.DEBIT) continue;
                if (t.getTxnTimestamp().isBefore(credit.getTxnTimestamp())) continue;
                if (t.getTxnTimestamp().isAfter(windowEnd)) continue;
                if (t.getTxnTimestamp().isAfter(current.getTxnTimestamp())) continue;
                outboundInWindow.add(t);
                outboundSum = outboundSum.add(t.getAmountBase());
            }

            BigDecimal requiredOut = credit.getAmountBase().multiply(BigDecimal.valueOf(cfg.getMinPercentOut()));
            if (outboundSum.compareTo(requiredOut) >= 0) {
                double pct = outboundSum.doubleValue() / credit.getAmountBase().doubleValue() * 100.0;
                String explanation = String.format(
                        "%.1f%% of a %.2f credit (on %s) was moved back out across %d debit(s) within %dh - possible layering.",
                        pct, credit.getAmountBase(), credit.getTxnTimestamp(), outboundInWindow.size(), cfg.getWindowHours());
                List<java.util.UUID> evidence = new ArrayList<>();
                evidence.add(credit.getId());
                outboundInWindow.forEach(t -> evidence.add(t.getId()));
                return new RuleResult(true, CODE, cfg.getWeight(), explanation, evidence);
            }
        }
        return RuleResult.notTriggered();
    }
}
