package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.primary.OkStatus;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.manager.ReconciliationFileManager;
import com.jbr.middletier.money.manager.ReconciliationManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.io.*;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reconciliation", description = "Bank statement import, matching, and reconciliation")
public class ReconciliationController {
    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationController.class);

    private final ReconciliationFileManager reconciliationFileManager;
    private final ReconciliationManager reconciliationManager;
    private final Flux<ServerSentEvent<ReconcileFileDataUpdateDTO>> updateNotifier;

    @Autowired
    public ReconciliationController(ReconciliationFileManager reconciliationFileManager,
                                    ReconciliationManager reconciliationManager) {
        this.reconciliationFileManager = reconciliationFileManager;
        this.reconciliationManager = reconciliationManager;
        this.updateNotifier = Flux.interval(Duration.ofSeconds(2))
                .map(this::checkFileUpdates);
    }

    @PutMapping(path = "/reconcile")
    public OkStatus reconcile(@Valid @RequestBody ReconcileTransactionDTO reconcileTransactions) throws InvalidTransactionIdException, MultipleUnlockedStatementException {
        reconciliationManager.reconcile(reconcileTransactions);
        return OkStatus.getOkStatus();
    }

    @PostMapping(path = "/reconciliation/load")
    public Iterable<ReconciliationFileDTO> reconcileDataLoad(@Valid @RequestBody ReconciliationFileLoadDTO reconciliationFileLoad) throws IOException {
        LOG.info("Request to load file (sanitized) - {}", reconciliationFileLoad);
        reconciliationManager.loadFile(reconciliationFileLoad);
        return getListOfFiles();
    }

    @Operation(summary = "Update the account association for a reconciliation file")
    @PutMapping(path = "/reconciliation/account")
    public Iterable<ReconciliationFileDTO> updateFileAccount(@Valid @RequestBody ReconciliationFileUpdateAccountDTO reconciliationFileUpdateAccount) throws IOException {
        LOG.info("Request to update account on file (sanitized) - {}", reconciliationFileUpdateAccount);
        reconciliationManager.updateAccount(reconciliationFileUpdateAccount);
        return getListOfFiles();
    }

    @GetMapping(path = "/reconciliation/files")
    public Iterable<ReconciliationFileDTO> getListOfFiles() {
        LOG.info("Request to get list of files");
        return reconciliationFileManager.getFiles();
    }

    @DeleteMapping(path = "/reconciliation/clear")
    public OkStatus reconcileDataDelete() {
        LOG.info("Clear Reconciliation Data");
        reconciliationManager.clearRepositoryData();
        return OkStatus.getOkStatus();
    }

    private ServerSentEvent<ReconcileFileDataUpdateDTO> checkFileUpdates(long unused) {
        return ServerSentEvent.<ReconcileFileDataUpdateDTO> builder()
                .data(reconciliationFileManager.getLastUpdateTime())
                .build();
    }

    @Operation(summary = "SSE stream of reconciliation file update notifications")
    @GetMapping(path="/reconciliation/file-updates", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ReconcileFileDataUpdateDTO>> fileUpdate() {
        try {
            return this.updateNotifier;
        } catch (Exception e) {
            LOG.info("Exception");
        }
        return null;
    }
}
