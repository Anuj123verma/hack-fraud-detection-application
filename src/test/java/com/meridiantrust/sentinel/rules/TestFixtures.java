package com.meridiantrust.sentinel.rules;

import com.meridiantrust.sentinel.domain.Account;
import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.domain.Direction;
import com.meridiantrust.sentinel.domain.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Shared builders for rule unit tests - keeps each test focused on behavior, not setup boilerplate. */
final class TestFixtures {

    private TestFixtures() {
    }

    static Customer customer() {
        return Customer.builder()
                .customerRef("CUST-T1")
                .fullName("Test Customer")
                .idNumber("ID-0000")
                .customerType("RETAIL")
                .baseRiskRating("LOW")
                .build();
    }

    static Account account(Customer customer) {
        return Account.builder()
                .customer(customer)
                .accountNumber("ACC-T1")
                .currency("INR")
                .riskRating("LOW")
                .build();
    }

    static Transaction txn(Account account, Direction direction, double amount, LocalDateTime ts) {
        return txn(account, direction, amount, ts, null);
    }

    static Transaction txn(Account account, Direction direction, double amount, LocalDateTime ts, String jurisdiction) {
        return Transaction.builder()
                .externalTxnId(UUID.randomUUID().toString())
                .account(account)
                .direction(direction)
                .amount(BigDecimal.valueOf(amount))
                .currency("INR")
                .amountBase(BigDecimal.valueOf(amount))
                .counterpartyJurisdiction(jurisdiction)
                .channel("WIRE")
                .txnTimestamp(ts)
                .build();
    }
}
