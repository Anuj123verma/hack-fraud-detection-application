package com.meridiantrust.sentinel.rules;

import com.meridiantrust.sentinel.config.AmlProperties;
import com.meridiantrust.sentinel.domain.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Business Rule #5: a customer's daily transaction value exceeding
 * {@code multiplier}x their {@code lookbackDays}-day rolling daily average
 * -> Behavioral Deviation alert.
 */
@Component
public class BehavioralDeviationRule implements AmlRule {

    public static final String CODE = "BEHAVIORAL_DEVIATION";

    private final AmlProperties properties;

    public BehavioralDeviationRule(AmlProperties properties) {
        this.properties = properties;
    }

    @Override
    public RuleResult evaluate(RuleContext context) {
        AmlProperties.BehavioralDeviation cfg = properties.getRules().getBehavioralDeviation();
        if (!cfg.isEnabled()) {
            return RuleResult.notTriggered();
        }
        Transaction current = context.current();
        LocalDate today = current.getTxnTimestamp().toLocalDate();
        LocalDateTime baselineStart = current.getTxnTimestamp().minusDays(cfg.getLookbackDays()).toLocalDate().atStartOfDay();
        LocalDateTime todayStart = today.atStartOfDay();

        List<Transaction> baseline = context.customerHistory().stream()
                .filter(t -> !t.getTxnTimestamp().isBefore(baselineStart))
                .filter(t -> t.getTxnTimestamp().isBefore(todayStart))
                .toList();

        if (baseline.isEmpty()) {
            // No history to compare against - don't punish new customers with a false positive.
            return RuleResult.notTriggered();
        }

        BigDecimal baselineTotal = baseline.stream().map(Transaction::getAmountBase).reduce(BigDecimal.ZERO, BigDecimal::add);
        // Average over days the customer actually transacted, not the full calendar
        // window - dividing by lookbackDays would dilute the baseline for anyone who
        // doesn't transact literally every day (i.e. almost everyone), making the
        // rule wildly over-sensitive and defeating the point of a "deviation" check.
        long activeDays = baseline.stream().map(t -> t.getTxnTimestamp().toLocalDate()).distinct().count();
        double avgDailyValue = baselineTotal.doubleValue() / Math.max(1, activeDays);
        if (avgDailyValue <= 0) {
            return RuleResult.notTriggered();
        }

        List<Transaction> todayTxns = context.customerHistory().stream()
                .filter(t -> !t.getTxnTimestamp().isBefore(todayStart))
                .filter(t -> !t.getTxnTimestamp().isAfter(current.getTxnTimestamp()))
                .toList();
        double todayValue = todayTxns.stream().mapToDouble(t -> t.getAmountBase().doubleValue()).sum();

        double thresholdValue = avgDailyValue * cfg.getMultiplier();
        if (todayValue <= thresholdValue) {
            return RuleResult.notTriggered();
        }

        double ratio = todayValue / avgDailyValue;
        int score = (int) Math.min(100, cfg.getWeight() * (ratio / cfg.getMultiplier()));
        String explanation = String.format(
                "Today's transaction value of %.2f is %.1fx the customer's %d-day rolling daily average of %.2f (threshold: %.1fx).",
                todayValue, ratio, cfg.getLookbackDays(), avgDailyValue, cfg.getMultiplier());

        return new RuleResult(true, CODE, score, explanation, todayTxns.stream().map(Transaction::getId).toList());
    }
}
