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

class LargeSingleTransactionRuleTest {

    private final AmlProperties properties = new AmlProperties();
    private final LargeSingleTransactionRule rule = new LargeSingleTransactionRule(properties);

    @Test
    void triggersAtExactlyTheThreshold() {
        Customer customer = TestFixtures.customer();
        Account account = TestFixtures.account(customer);
        Transaction txn = TestFixtures.txn(account, Direction.DEBIT, 10000.00, LocalDateTime.now());

        RuleResult result = rule.evaluate(new RuleContext(txn, account, customer, List.of(txn), List.of(txn)));

        assertThat(result.triggered()).isTrue();
        assertThat(result.ruleCode()).isEqualTo(LargeSingleTransactionRule.CODE);
        assertThat(result.contributionScore()).isEqualTo(60);
    }

    @Test
    void doesNotTriggerJustBelowThreshold() {
        Customer customer = TestFixtures.customer();
        Account account = TestFixtures.account(customer);
        Transaction txn = TestFixtures.txn(account, Direction.DEBIT, 9999.99, LocalDateTime.now());

        RuleResult result = rule.evaluate(new RuleContext(txn, account, customer, List.of(txn), List.of(txn)));

        assertThat(result.triggered()).isFalse();
    }

    @Test
    void disabledRuleNeverTriggers() {
        properties.getRules().getLargeSingleTxn().setEnabled(false);
        Customer customer = TestFixtures.customer();
        Account account = TestFixtures.account(customer);
        Transaction txn = TestFixtures.txn(account, Direction.DEBIT, 999999, LocalDateTime.now());

        RuleResult result = rule.evaluate(new RuleContext(txn, account, customer, List.of(txn), List.of(txn)));

        assertThat(result.triggered()).isFalse();
    }
}
