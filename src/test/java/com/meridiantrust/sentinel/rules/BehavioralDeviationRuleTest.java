package com.meridiantrust.sentinel.rules;

import com.meridiantrust.sentinel.config.AmlProperties;
import com.meridiantrust.sentinel.domain.Account;
import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.domain.Direction;
import com.meridiantrust.sentinel.domain.Transaction;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BehavioralDeviationRuleTest {

    private final AmlProperties properties = new AmlProperties();
    private final BehavioralDeviationRule rule = new BehavioralDeviationRule(properties);

    @Test
    void triggersOnASpikeWellAboveTheRollingAverage() {
        Customer customer = TestFixtures.customer();
        Account account = TestFixtures.account(customer);
        LocalDateTime now = LocalDateTime.now();

        // Baseline: ~2000/day average across 90 days (spread thin, only a few sample days).
        List<Transaction> history = new ArrayList<>();
        for (int i = 10; i <= 80; i += 10) {
            history.add(TestFixtures.txn(account, Direction.DEBIT, 2000, now.minusDays(i)));
        }
        Transaction spike = TestFixtures.txn(account, Direction.DEBIT, 30000, now); // way more than 3x avg
        history.add(spike);

        RuleResult result = rule.evaluate(new RuleContext(spike, account, customer, history, history));

        assertThat(result.triggered()).isTrue();
    }

    @Test
    void doesNotTriggerWhenBelowTheMultiplierThreshold() {
        Customer customer = TestFixtures.customer();
        Account account = TestFixtures.account(customer);
        LocalDateTime now = LocalDateTime.now();

        List<Transaction> history = new ArrayList<>();
        for (int i = 10; i <= 80; i += 10) {
            history.add(TestFixtures.txn(account, Direction.DEBIT, 5000, now.minusDays(i)));
        }
        Transaction today = TestFixtures.txn(account, Direction.DEBIT, 6000, now); // modest, not a deviation
        history.add(today);

        RuleResult result = rule.evaluate(new RuleContext(today, account, customer, history, history));

        assertThat(result.triggered()).isFalse();
    }

    @Test
    void newCustomersWithNoBaselineHistoryNeverFalsePositive() {
        Customer customer = TestFixtures.customer();
        Account account = TestFixtures.account(customer);
        Transaction onlyTxn = TestFixtures.txn(account, Direction.DEBIT, 100000, LocalDateTime.now());

        RuleResult result = rule.evaluate(new RuleContext(onlyTxn, account, customer, List.of(onlyTxn), List.of(onlyTxn)));

        assertThat(result.triggered()).isFalse();
    }
}
