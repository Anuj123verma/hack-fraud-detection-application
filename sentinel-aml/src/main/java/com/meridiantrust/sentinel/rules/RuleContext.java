package com.meridiantrust.sentinel.rules;

import com.meridiantrust.sentinel.domain.Account;
import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.domain.Transaction;

import java.util.List;

/**
 * Everything a rule needs to make a decision about {@code current}, pre-fetched
 * once by the DetectionEngineService and shared across all rules so we don't
 * hit the DB once per rule per transaction.
 */
public record RuleContext(
        Transaction current,
        Account account,
        Customer customer,
        List<Transaction> accountHistory,   // all txns for this account, ascending by time, current included
        List<Transaction> customerHistory   // all txns across all of the customer's accounts, ascending, current included
) {
}
