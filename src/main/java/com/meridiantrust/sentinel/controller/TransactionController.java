package com.meridiantrust.sentinel.controller;

import com.meridiantrust.sentinel.dto.TransactionIngestRequest;
import com.meridiantrust.sentinel.dto.TransactionIngestResponse;
import com.meridiantrust.sentinel.service.TransactionIngestService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Streaming/single-transaction ingestion - validates, persists, and runs detection inline. */
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionIngestService ingestService;

    public TransactionController(TransactionIngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Ingest a single transaction and run detection inline (sub-second).")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionIngestResponse ingest(@Valid @RequestBody TransactionIngestRequest request) {
        return ingestService.ingest(request);
    }
}
