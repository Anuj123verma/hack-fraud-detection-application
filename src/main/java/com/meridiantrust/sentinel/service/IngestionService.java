package com.meridiantrust.sentinel.service;

import com.meridiantrust.sentinel.domain.Account;
import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.domain.Direction;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.dto.IngestionReportDto;
import com.meridiantrust.sentinel.dto.IngestionRowError;
import com.meridiantrust.sentinel.repository.AccountRepository;
import com.meridiantrust.sentinel.repository.CustomerRepository;
import com.meridiantrust.sentinel.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Bulk CSV ingestion for Customer / Account / Transaction. Deliberately
 * simple, dependency-free CSV parsing (comma-split, header row skipped) -
 * fine for well-formed synthetic seed files; production would swap in
 * Apache Commons CSV for quoting/escaping without touching callers.
 *
 * Every row is validated independently: malformed rows are rejected and
 * reported back (row number + reason), never silently dropped, and never
 * allowed to abort the rest of the batch.
 */
@Slf4j
@Service
public class IngestionService {

    private static final Set<String> CUSTOMER_TYPES = Set.of("RETAIL", "BUSINESS");
    private static final Set<String> RISK_RATINGS = Set.of("LOW", "MEDIUM", "HIGH");

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CurrencyService currencyService;
    private final DetectionEngineService detectionEngineService;

    public IngestionService(CustomerRepository customerRepository, AccountRepository accountRepository,
                             TransactionRepository transactionRepository, CurrencyService currencyService,
                             DetectionEngineService detectionEngineService) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.currencyService = currencyService;
        this.detectionEngineService = detectionEngineService;
    }

    /** Header: customerRef,fullName,idNumber,customerType,baseRiskRating */
    @Transactional
    public IngestionReportDto ingestCustomers(MultipartFile file) throws IOException {
        List<String> lines = readDataLines(file);
        List<IngestionRowError> errors = new ArrayList<>();
        int success = 0;

        for (int i = 0; i < lines.size(); i++) {
            int rowNumber = i + 2; // +1 for 0-index, +1 for header row
            String line = lines.get(i);
            try {
                String[] c = splitCsv(line, 5);
                String customerRef = c[0];
                if (customerRef.isBlank()) throw new IllegalArgumentException("customerRef is required");
                if (customerRepository.findByCustomerRef(customerRef).isPresent())
                    throw new IllegalArgumentException("customerRef already exists: " + customerRef);
                if (c[1].isBlank()) throw new IllegalArgumentException("fullName is required");
                if (c[2].isBlank()) throw new IllegalArgumentException("idNumber is required");
                if (!CUSTOMER_TYPES.contains(c[3].toUpperCase()))
                    throw new IllegalArgumentException("customerType must be one of " + CUSTOMER_TYPES);
                if (!RISK_RATINGS.contains(c[4].toUpperCase()))
                    throw new IllegalArgumentException("baseRiskRating must be one of " + RISK_RATINGS);

                customerRepository.save(Customer.builder()
                        .customerRef(customerRef)
                        .fullName(c[1])
                        .idNumber(c[2])
                        .customerType(c[3].toUpperCase())
                        .baseRiskRating(c[4].toUpperCase())
                        .build());
                success++;
            } catch (Exception ex) {
                errors.add(new IngestionRowError(rowNumber, line, ex.getMessage()));
            }
        }
        log.info("Customer ingestion: {} rows, {} succeeded, {} rejected", lines.size(), success, errors.size());
        return new IngestionReportDto(lines.size(), success, errors.size(), errors);
    }

    /** Header: accountNumber,customerRef,currency,riskRating */
    @Transactional
    public IngestionReportDto ingestAccounts(MultipartFile file) throws IOException {
        List<String> lines = readDataLines(file);
        List<IngestionRowError> errors = new ArrayList<>();
        int success = 0;

        for (int i = 0; i < lines.size(); i++) {
            int rowNumber = i + 2;
            String line = lines.get(i);
            try {
                String[] c = splitCsv(line, 4);
                String accountNumber = c[0];
                if (accountNumber.isBlank()) throw new IllegalArgumentException("accountNumber is required");
                if (accountRepository.findByAccountNumber(accountNumber).isPresent())
                    throw new IllegalArgumentException("accountNumber already exists: " + accountNumber);

                Customer customer = customerRepository.findByCustomerRef(c[1])
                        .orElseThrow(() -> new IllegalArgumentException("Unknown customerRef (referential integrity): " + c[1]));
                if (c[2].isBlank() || c[2].length() != 3)
                    throw new IllegalArgumentException("currency must be a 3-letter ISO code");
                if (!RISK_RATINGS.contains(c[3].toUpperCase()))
                    throw new IllegalArgumentException("riskRating must be one of " + RISK_RATINGS);

                accountRepository.save(Account.builder()
                        .customer(customer)
                        .accountNumber(accountNumber)
                        .currency(c[2].toUpperCase())
                        .riskRating(c[3].toUpperCase())
                        .build());
                success++;
            } catch (Exception ex) {
                errors.add(new IngestionRowError(rowNumber, line, ex.getMessage()));
            }
        }
        log.info("Account ingestion: {} rows, {} succeeded, {} rejected", lines.size(), success, errors.size());
        return new IngestionReportDto(lines.size(), success, errors.size(), errors);
    }

    /** Header: externalTxnId,accountNumber,direction,amount,currency,counterpartyName,counterpartyJurisdiction,channel,txnTimestamp */
    @Transactional
    public IngestionReportDto ingestTransactions(MultipartFile file) throws IOException {
        List<String> lines = readDataLines(file);
        List<IngestionRowError> errors = new ArrayList<>();
        List<Transaction> saved = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            int rowNumber = i + 2;
            String line = lines.get(i);
            try {
                String[] c = splitCsv(line, 9);
                String externalTxnId = c[0];
                if (externalTxnId.isBlank()) throw new IllegalArgumentException("externalTxnId is required");
                if (transactionRepository.findByExternalTxnId(externalTxnId).isPresent())
                    throw new IllegalArgumentException("Duplicate externalTxnId (already ingested): " + externalTxnId);

                Account account = accountRepository.findByAccountNumber(c[1])
                        .orElseThrow(() -> new IllegalArgumentException("Unknown accountNumber (referential integrity): " + c[1]));

                Direction direction;
                try {
                    direction = Direction.valueOf(c[2].toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("direction must be CREDIT or DEBIT");
                }

                BigDecimal amount;
                try {
                    amount = new BigDecimal(c[3]);
                    if (amount.signum() <= 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("amount must be a positive number");
                }

                BigDecimal amountBase = currencyService.toBase(amount, c[4].toUpperCase());

                LocalDateTime txnTimestamp = c[8].isBlank() ? LocalDateTime.now() : LocalDateTime.parse(c[8]);
                if (txnTimestamp.isAfter(LocalDateTime.now()))
                    throw new IllegalArgumentException("txnTimestamp cannot be in the future");

                Transaction txn = Transaction.builder()
                        .externalTxnId(externalTxnId)
                        .account(account)
                        .direction(direction)
                        .amount(amount)
                        .currency(c[4].toUpperCase())
                        .amountBase(amountBase)
                        .counterpartyName(c[5].isBlank() ? null : c[5])
                        .counterpartyJurisdiction(c[6].isBlank() ? null : c[6].toUpperCase())
                        .channel(c[7].isBlank() ? "UNKNOWN" : c[7].toUpperCase())
                        .txnTimestamp(txnTimestamp)
                        .build();
                saved.add(transactionRepository.save(txn));
            } catch (Exception ex) {
                errors.add(new IngestionRowError(rowNumber, line, ex.getMessage()));
            }
        }

        // Replay through detection in chronological order so window-based rules
        // (structuring, rapid movement, behavioral deviation) see history correctly.
        saved.stream()
                .sorted((a, b) -> a.getTxnTimestamp().compareTo(b.getTxnTimestamp()))
                .forEach(detectionEngineService::evaluate);

        log.info("Transaction ingestion: {} rows, {} succeeded, {} rejected", lines.size(), saved.size(), errors.size());
        return new IngestionReportDto(lines.size(), saved.size(), errors.size(), errors);
    }

    private List<String> readDataLines(MultipartFile file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // header, discarded
            if (line == null) throw new IllegalArgumentException("CSV file is empty (no header row).");
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) lines.add(line);
            }
        }
        return lines;
    }

    private String[] splitCsv(String line, int expectedColumns) {
        String[] parts = line.split(",", -1);
        if (parts.length != expectedColumns) {
            throw new IllegalArgumentException("Expected " + expectedColumns + " columns, found " + parts.length);
        }
        for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim();
        return parts;
    }
}
