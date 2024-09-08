package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.primary.OkStatus;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.manager.ReconciliationFileManager;
import com.jbr.middletier.money.manager.ReconciliationManager;
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


/**
 * Created by jason on 07/03/17.
 */
@RestController
@RequestMapping("/jbr")
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

    @PutMapping(path = "/ext/money/reconcile")
    public OkStatus reconcileExt(@Valid @RequestBody ReconcileTransactionDTO reconcileTransactions) throws InvalidTransactionIdException, MultipleUnlockedStatementException {
        reconciliationManager.reconcile(reconcileTransactions);
        return OkStatus.getOkStatus();
    }

    @PutMapping(path = "/int/money/reconcile")
    public OkStatus reconcileInt(@Valid @RequestBody ReconcileTransactionDTO reconcileTransactions) throws InvalidTransactionIdException, MultipleUnlockedStatementException {
        return reconcileExt(reconcileTransactions);
    }

    @PostMapping(path = "/int/money/reconciliation/load")
    public Iterable<ReconciliationFileDTO> reconcileDataLoadInt(@Valid @RequestBody ReconciliationFileLoadDTO reconciliationFileLoad) throws IOException {
        LOG.info("Request to load file (sanitized) - {}", reconciliationFileLoad);
        reconciliationManager.loadFile(reconciliationFileLoad);
        return getListOfFiles();
    }

    @PutMapping(path = "/int/money/reconciliation/updatefileaccount")
    public Iterable<ReconciliationFileDTO> updateFileAccountInt(@Valid @RequestBody ReconciliationFileUpdateAccountDTO reconciliationFileUpdateAccount) throws IOException {
        LOG.info("Request to update account on file (sanitized) - {}", reconciliationFileUpdateAccount);
        reconciliationManager.updateAccount(reconciliationFileUpdateAccount);
        return getListOfFiles();
    }

    @PutMapping(path = "/ext/money/reconciliation/updatefileaccount")
    public Iterable<ReconciliationFileDTO> updateFileAccountExt(@Valid @RequestBody ReconciliationFileUpdateAccountDTO reconciliationFileUpdateAccount) throws IOException {
        return updateFileAccountInt(reconciliationFileUpdateAccount);
    }

    @GetMapping(path = "/int/money/reconciliation/files")
    public Iterable<ReconciliationFileDTO> getListOfFiles() {
        LOG.info("Request to get list of files");
        return reconciliationFileManager.getFiles();
    }

    @DeleteMapping(path = "/ext/money/reconciliation/clear")
    public OkStatus reconcileDataDeleteExt() {
        LOG.info("Clear Reconciliation Data (ext) ");
        reconciliationManager.clearRepositoryData();
        return OkStatus.getOkStatus();
    }

    @DeleteMapping(path = "/int/money/reconciliation/clear")
    public OkStatus reconcileDataDeleteInt() {
        return this.reconcileDataDeleteExt();
    }

    private ServerSentEvent<ReconcileFileDataUpdateDTO> checkFileUpdates(long unused) {
        // Return the update information.
        return ServerSentEvent.<ReconcileFileDataUpdateDTO> builder()
                .data(reconciliationFileManager.getLastUpdateTime())
                .build();
    }

    @GetMapping(path="/int/money/reconciliation/file-updates",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ReconcileFileDataUpdateDTO>> fileUpdate() {
        try {
            return this.updateNotifier;
        } catch (Exception e) {
            LOG.info("Exception");
        }

        return null;
    }
}
