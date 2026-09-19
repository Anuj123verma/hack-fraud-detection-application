package com.meridiantrust.sentinel.controller;

import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.dto.CustomerSummaryDto;
import com.meridiantrust.sentinel.dto.TransactionDto;
import com.meridiantrust.sentinel.repository.CustomerRepository;
import com.meridiantrust.sentinel.repository.TransactionRepository;
import com.meridiantrust.sentinel.security.PiiMasker;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/** Read-only customer list + transaction timeline, used by the analyst frontend. */
@RestController
@RequestMapping("/api/v1/customers")
@PreAuthorize("hasRole('ANALYST')")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    public CustomerController(CustomerRepository customerRepository, TransactionRepository transactionRepository) {
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    @Operation(summary = "List all customers (masked PII).")
    public List<CustomerSummaryDto> list() {
        return customerRepository.findAll().stream().map(this::toSummary).toList();
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "Transaction timeline for a customer across all their accounts, most recent first.")
    public List<TransactionDto> timeline(@PathVariable UUID id) {
        if (!customerRepository.existsById(id)) {
            throw new NoSuchElementException("Customer not found: " + id);
        }
        return transactionRepository.findByAccount_Customer_IdOrderByTxnTimestampDesc(id).stream()
                .map(this::toDto)
                .toList();
    }

    private CustomerSummaryDto toSummary(Customer c) {
        return new CustomerSummaryDto(c.getId(), c.getCustomerRef(), PiiMasker.maskName(c.getFullName()),
                c.getCustomerType(), c.getBaseRiskRating());
    }

    private TransactionDto toDto(Transaction t) {
        return new TransactionDto(t.getId(), t.getAccount().getAccountNumber(), t.getDirection(), t.getAmount(),
                t.getCurrency(), t.getAmountBase(), t.getCounterpartyName(), t.getCounterpartyJurisdiction(),
                t.getChannel(), t.getTxnTimestamp());
    }
}
