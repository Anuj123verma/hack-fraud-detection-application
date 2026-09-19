package com.meridiantrust.sentinel.service;

import com.meridiantrust.sentinel.domain.Account;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.dto.TransactionIngestRequest;
import com.meridiantrust.sentinel.dto.TransactionIngestResponse;
import com.meridiantrust.sentinel.repository.AccountRepository;
import com.meridiantrust.sentinel.repository.TransactionRepository;
import com.meridiantrust.sentinel.rules.RuleResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Streaming ingestion entry point: validate -> normalize currency ->
 * persist -> run detection inline (same transaction thread), giving
 * sub-second turnaround from ingest to alert.
 */
@Service
public class TransactionIngestService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CurrencyService currencyService;
    private final DetectionEngineService detectionEngineService;

    public TransactionIngestService(AccountRepository accountRepository, TransactionRepository transactionRepository,
                                     CurrencyService currencyService, DetectionEngineService detectionEngineService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.currencyService = currencyService;
        this.detectionEngineService = detectionEngineService;
    }

    @Transactional
    public TransactionIngestResponse ingest(TransactionIngestRequest request) {
        Account account = accountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Unknown account number: " + request.accountNumber()));

        transactionRepository.findByExternalTxnId(request.externalTxnId()).ifPresent(t -> {
            throw new IllegalArgumentException("Duplicate external_txn_id (already ingested): " + request.externalTxnId());
        });

        BigDecimal amountBase = currencyService.toBase(request.amount(), request.currency());

        Transaction txn = Transaction.builder()
                .externalTxnId(request.externalTxnId())
                .account(account)
                .direction(request.direction())
                .amount(request.amount())
                .currency(request.currency())
                .amountBase(amountBase)
                .counterpartyName(request.counterpartyName())
                .counterpartyJurisdiction(request.counterpartyJurisdiction())
                .channel(request.channel())
                .txnTimestamp(request.txnTimestamp() != null ? request.txnTimestamp() : LocalDateTime.now())
                .build();

        Transaction saved = transactionRepository.save(txn);
        List<RuleResult> triggered = detectionEngineService.evaluate(saved);

        return new TransactionIngestResponse(
                saved.getId(),
                !triggered.isEmpty(),
                triggered.stream().map(RuleResult::ruleCode).toList());
    }
}
