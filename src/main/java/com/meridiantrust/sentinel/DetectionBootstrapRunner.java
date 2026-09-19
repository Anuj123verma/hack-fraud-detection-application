package com.meridiantrust.sentinel;

import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.repository.TransactionRepository;
import com.meridiantrust.sentinel.service.DetectionEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Simulates a bulk load: replays every seeded transaction through the exact
 * same detection path streaming ingestion uses, in chronological order, so
 * the alert queue is populated the moment the app starts (nice for demoing)
 * and we get a wall-clock timing log proving the performance NFR
 * ("10k transactions in under 2 minutes").
 */
@Slf4j
@Component
public class DetectionBootstrapRunner implements ApplicationRunner {

    private final TransactionRepository transactionRepository;
    private final DetectionEngineService detectionEngineService;

    public DetectionBootstrapRunner(TransactionRepository transactionRepository, DetectionEngineService detectionEngineService) {
        this.transactionRepository = transactionRepository;
        this.detectionEngineService = detectionEngineService;
    }

    // One transaction spans the whole replay so lazily-loaded account/customer
    // associations on each Transaction stay attached to a live Hibernate session
    // for the entire loop (they'd otherwise detach the moment findAll() returns).
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Transaction> all = transactionRepository.findAllByOrderByTxnTimestampAsc();
        log.info("=== Bulk detection replay starting over {} seeded transactions ===", all.size());
        long start = System.currentTimeMillis();
        all.forEach(detectionEngineService::evaluate);
        long elapsed = System.currentTimeMillis() - start;
        log.info("=== Bulk detection replay complete: {} transactions in {} ms ===", all.size(), elapsed);
    }
}
