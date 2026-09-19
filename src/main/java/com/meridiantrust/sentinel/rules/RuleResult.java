package com.meridiantrust.sentinel.rules;

import java.util.List;
import java.util.UUID;

/** The verdict of a single rule evaluation. */
public record RuleResult(
        boolean triggered,
        String ruleCode,
        int contributionScore,
        String explanation,
        List<UUID> evidenceTxnIds
) {
    public static RuleResult notTriggered() {
        return new RuleResult(false, null, 0, null, List.of());
    }
}
