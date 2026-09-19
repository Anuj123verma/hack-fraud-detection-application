package com.meridiantrust.sentinel.repository;

import com.meridiantrust.sentinel.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByExternalTxnId(String externalTxnId);

    List<Transaction> findByAccountIdOrderByTxnTimestampAsc(UUID accountId);

    /** Used by the Behavioral Deviation rule, which looks across all of a customer's accounts. */
    List<Transaction> findByAccount_Customer_IdOrderByTxnTimestampAsc(UUID customerId);

    /** Used by the customer timeline view - most recent first. */
    List<Transaction> findByAccount_Customer_IdOrderByTxnTimestampDesc(UUID customerId);

    List<Transaction> findAllByOrderByTxnTimestampAsc();
}
