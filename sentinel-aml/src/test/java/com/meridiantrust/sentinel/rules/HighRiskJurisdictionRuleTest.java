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

class HighRiskJurisdictionRuleTest {

    private final AmlProperties properties = new AmlProperties();
    private final HighRiskJurisdictionRule rule = new HighRiskJurisdictionRule(properties);

    HighRiskJurisdictionRuleTest() {
        properties.getRules().getHighRiskJurisdiction().setJurisdictions(List.of("IR", "KP", "SY"));
    }

    @Test
    void alwaysTriggersForListedJurisdictionRegardlessOfAmount() {
        Customer customer = TestFixtures.customer();
        Account account = TestFixtures.account(customer);
        Transaction txn = TestFixtures.txn(account, Direction.DEBIT, 1.00, LocalDateTime.now(), "KP");

        RuleResult result = rule.evaluate(new RuleContext(txn, account, customer, List.of(txn), List.of(txn)));

        assertThat(result.triggered()).isTrue();
    }

    @Test
    void doesNotTriggerForNonListedJurisdiction() {
        Customer customer = TestFixtures.customer();
        Account account = TestFixtures.account(customer);
        Transaction txn = TestFixtures.txn(account, Direction.DEBIT, 500000, LocalDateTime.now(), "US");

        RuleResult result = rule.evaluate(new RuleContext(txn, account, customer, List.of(txn), List.of(txn)));

        assertThat(result.triggered()).isFalse();
    }

    @Test
    void doesNotTriggerWhenJurisdictionIsAbsent() {
        Customer customer = TestFixtures.customer();
        Account account = TestFixtures.account(customer);
        Transaction txn = TestFixtures.txn(account, Direction.DEBIT, 500000, LocalDateTime.now(), null);

        RuleResult result = rule.evaluate(new RuleContext(txn, account, customer, List.of(txn), List.of(txn)));

        assertThat(result.triggered()).isFalse();
    }
}
