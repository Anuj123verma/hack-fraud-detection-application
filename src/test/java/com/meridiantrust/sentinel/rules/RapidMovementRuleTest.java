package com.meridiantrust.sentinel.rules;

import com.meridiantrust.sentinel.config.AmlProperties;
import com.meridiantrust.sentinel.domain.Account;
import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.domain.Direction;
import com.meridiantrust.sentinel.domain.Transaction;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RapidMovementRuleTest {

    private final AmlProperties properties = new AmlProperties();
    private final RapidMovementRule rule = new RapidMovementRule(properties);

    @Test
    void triggersWhenEightyPercentOrMoreMovesOutWithinWindow() {
        Customer customer = TestFixtures.customer();
        Account account = TestFixtures.account(customer);
        LocalDateTime now = LocalDateTime.now();

        Transaction credit = TestFixtures.txn(account, Direction.CREDIT, 100000, now.minusHours(40));
        Transaction debit = TestFixtures.txn(account, Direction.DEBIT, 85000, now); // 85% >= 80%

        List<Transaction> history = List.of(credit, debit);
        RuleResult result = rule.evaluate(new RuleContext(debit, account, customer, history, history));

        assertThat(result.triggered()).isTrue();
    }

    @Test
    void doesNotTriggerBelowThePercentThreshold() {
        Customer customer = TestFixtures.customer();
        Account account = TestFixtures.account(customer);
        LocalDateTime now = LocalDateTime.now();

        Transaction credit = TestFixtures.txn(account, Direction.CREDIT, 100000, now.minusHours(40));
        Transaction debit = TestFixtures.txn(account, Direction.DEBIT, 70000, now); // 70% < 80%

        List<Transaction> history = List.of(credit, debit);
        RuleResult result = rule.evaluate(new RuleContext(debit, account, customer, history, history));

        assertThat(result.triggered()).isFalse();
    }

    @Test
    void doesNotTriggerWhenOutsideTheTimeWindow() {
        Customer customer = TestFixtures.customer();
        Account account = TestFixtures.account(customer);
        LocalDateTime now = LocalDateTime.now();

        Transaction credit = TestFixtures.txn(account, Direction.CREDIT, 100000, now.minusHours(60)); // > 48h window
        Transaction debit = TestFixtures.txn(account, Direction.DEBIT, 90000, now);

        List<Transaction> history = List.of(credit, debit);
        RuleResult result = rule.evaluate(new RuleContext(debit, account, customer, history, history));

        assertThat(result.triggered()).isFalse();
    }

    @Test
    void creditTransactionsThemselvesNeverTrigger() {
        Customer customer = TestFixtures.customer();
        Account account = TestFixtures.account(customer);
        Transaction credit = TestFixtures.txn(account, Direction.CREDIT, 100000, LocalDateTime.now());

        RuleResult result = rule.evaluate(new RuleContext(credit, account, customer, List.of(credit), List.of(credit)));

        assertThat(result.triggered()).isFalse();
    }
}
