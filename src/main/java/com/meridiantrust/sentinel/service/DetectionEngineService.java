package com.meridiantrust.sentinel.service;

import com.meridiantrust.sentinel.domain.Account;
import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.repository.TransactionRepository;
import com.meridiantrust.sentinel.rules.AmlRule;
import com.meridiantrust.sentinel.rules.RuleContext;
import com.meridiantrust.sentinel.rules.RuleResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates every configured {@link AmlRule} against a single transaction.
 * Used identically for streaming ingest (one txn, sub-second) and bulk
 * replay at startup (loop over many txns) - one code path, two callers,
 * per DRY.
 */
@Slf4j
@Service
public class DetectionEngineService {

    private final List<AmlRule> rules;
    private final TransactionRepository transactionRepository;
    private final AlertService alertService;

    public DetectionEngineService(List<AmlRule> rules, TransactionRepository transactionRepository, AlertService alertService) {
        this.rules = rules;
        this.transactionRepository = transactionRepository;
        this.alertService = alertService;
    }

    public List<RuleResult> evaluate(Transaction transaction) {
        Account account = transaction.getAccount();
        Customer customer = account.getCustomer();

        List<Transaction> accountHistory = transactionRepository.findByAccountIdOrderByTxnTimestampAsc(account.getId());
        List<Transaction> customerHistory = transactionRepository.findByAccount_Customer_IdOrderByTxnTimestampAsc(customer.getId());

        RuleContext context = new RuleContext(transaction, account, customer, accountHistory, customerHistory);

        List<RuleResult> triggered = rules.stream()
                .map(rule -> rule.evaluate(context))
                .filter(RuleResult::triggered)
                .toList();

        if (!triggered.isEmpty()) {
            log.info("Transaction {} triggered rules: {}", transaction.getExternalTxnId(),
                    triggered.stream().map(RuleResult::ruleCode).toList());
            alertService.recordDetectionResult(customer, account, transaction, triggered);
        }
        return triggered;
    }
}
