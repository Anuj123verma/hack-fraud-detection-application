package com.meridiantrust.sentinel.rules;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskScorerTest {

    @Test
    void sumsContributionsAndCapsAtOneHundred() {
        List<RuleResult> results = List.of(
                new RuleResult(true, "A", 60, "x", List.of()),
                new RuleResult(true, "B", 70, "y", List.of())
        );
        assertThat(RiskScorer.combine(results)).isEqualTo(100);
    }

    @Test
    void singleModerateRuleIsNotCapped() {
        List<RuleResult> results = List.of(new RuleResult(true, "A", 45, "x", List.of()));
        assertThat(RiskScorer.combine(results)).isEqualTo(45);
    }
}
