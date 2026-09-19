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

class StructuringRuleTest {

    private final AmlProperties properties = new AmlProperties();
    private final StructuringRule rule = new StructuringRule(properties);

    @Test
    void triggersOnThreeQualifyingTransactionsWithinWindow() {
        Customer customer = TestFixtures.customer();
        Account account = TestFixtures.account(customer);
        LocalDateTime now = LocalDateTime.now();

        Transaction t1 = TestFixtures.txn(account, Direction.DEBIT, 9000, now.minusHours(20));
        Transaction t2 = TestFixtures.txn(account, Direction.DEBIT, 9500, now.minusHours(10));
        Transaction t3 = TestFixtures.txn(account, Direction.DEBIT, 9999, now); // current

        List<Transaction> history = List.of(t1, t2, t3);
        RuleResult result = rule.evaluate(new RuleContext(t3, account, customer, history, history));

        assertThat(result.triggered()).isTrue();
        assertThat(result.evidenceTxnIds()).hasSize(3);
    }

    @Test
    void doesNotTriggerWithOnlyTwoQualifyingTransactions() {
        Customer customer = TestFixtures.customer();
        Account account = TestFixtures.account(customer);
        LocalDateTime now = LocalDateTime.now();

        Transaction t1 = TestFixtures.txn(account, Direction.DEBIT, 9000, now.minusHours(5));
        Transaction t2 = TestFixtures.txn(account, Direction.DEBIT, 9500, now);

        List<Transaction> history = List.of(t1, t2);
        RuleResult result = rule.evaluate(new RuleContext(t2, account, customer, history, history));

        assertThat(result.triggered()).isFalse();
    }

    @Test
    void doesNotTriggerWhenQualifyingTransactionsAreOutsideTheWindow() {
        Customer customer = TestFixtures.customer();
        Account account = TestFixtures.account(customer);
        LocalDateTime now = LocalDateTime.now();

        Transaction t1 = TestFixtures.txn(account, Direction.DEBIT, 9000, now.minusHours(30)); // outside 24h window
        Transaction t2 = TestFixtures.txn(account, Direction.DEBIT, 9500, now.minusHours(23));
        Transaction t3 = TestFixtures.txn(account, Direction.DEBIT, 9999, now);

        List<Transaction> history = List.of(t1, t2, t3);
        RuleResult result = rule.evaluate(new RuleContext(t3, account, customer, history, history));

        assertThat(result.triggered()).isFalse();
    }

    @Test
    void amountsOutsideTheSmurfingBandAreIgnored() {
        Customer customer = TestFixtures.customer();
        Account account = TestFixtures.account(customer);
        LocalDateTime now = LocalDateTime.now();

        Transaction t1 = TestFixtures.txn(account, Direction.DEBIT, 8000, now.minusHours(5)); // below band
        Transaction current = TestFixtures.txn(account, Direction.DEBIT, 7000, now); // below band -> short-circuits

        List<Transaction> history = List.of(t1, current);
        RuleResult result = rule.evaluate(new RuleContext(current, account, customer, history, history));

        assertThat(result.triggered()).isFalse();
    }
}
