package com.meridiantrust.sentinel.rules;

import java.util.List;

/** Pure combinator: sums each triggered rule's contribution, capped at 100. */
public final class RiskScorer {

    private RiskScorer() {
    }

    public static int combine(List<RuleResult> triggeredResults) {
        int sum = triggeredResults.stream().mapToInt(RuleResult::contributionScore).sum();
        return Math.min(100, sum);
    }
}
