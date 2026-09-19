package com.meridiantrust.sentinel.controller;

import com.meridiantrust.sentinel.dto.IngestionReportDto;
import com.meridiantrust.sentinel.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Bulk/batch ingestion via CSV upload for the three core entities. Complements
 * {@link TransactionController}'s streaming single-transaction endpoint - one
 * validation+persist path is reused by both (see IngestionService/DetectionEngineService).
 */
@RestController
@RequestMapping("/api/v1/ingestion")
@PreAuthorize("hasRole('COMPLIANCE_ADMIN')")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(value = "/customers", consumes = "multipart/form-data")
    @Operation(summary = "Bulk-load customers from CSV: customerRef,fullName,idNumber,customerType,baseRiskRating")
    public IngestionReportDto customers(@RequestParam("file") MultipartFile file) throws IOException {
        return ingestionService.ingestCustomers(file);
    }

    @PostMapping(value = "/accounts", consumes = "multipart/form-data")
    @Operation(summary = "Bulk-load accounts from CSV: accountNumber,customerRef,currency,riskRating")
    public IngestionReportDto accounts(@RequestParam("file") MultipartFile file) throws IOException {
        return ingestionService.ingestAccounts(file);
    }

    @PostMapping(value = "/transactions", consumes = "multipart/form-data")
    @Operation(summary = "Bulk-load transactions from CSV and run detection over them in chronological order: "
            + "externalTxnId,accountNumber,direction,amount,currency,counterpartyName,counterpartyJurisdiction,channel,txnTimestamp")
    public IngestionReportDto transactions(@RequestParam("file") MultipartFile file) throws IOException {
        return ingestionService.ingestTransactions(file);
    }
}
