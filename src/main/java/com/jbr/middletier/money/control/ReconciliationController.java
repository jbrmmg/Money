package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.manager.ReconciliationFileManager;
import com.jbr.middletier.money.manager.ReconciliationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.io.*;
import java.time.Duration;
import java.util.*;


/**
 * Created by jason on 07/03/17.
 */
@Controller
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
    public @ResponseBody OkStatus reconcileExt(@RequestBody ReconcileTransactionDTO reconcileTransaction) throws InvalidTransactionIdException, MultipleUnlockedStatementException {
        reconciliationManager.reconcile(reconcileTransaction.getTransactionId(), reconcileTransaction.getReconcile());
        return OkStatus.getOkStatus();
    }

    @PutMapping(path = "/int/money/reconcile")
    public @ResponseBody OkStatus reconcileInt(@RequestBody ReconcileTransactionDTO reconcileTransaction) throws InvalidTransactionIdException, MultipleUnlockedStatementException {
        return reconcileExt(reconcileTransaction);
    }

    @PostMapping(path = "/int/money/reconciliation/load")
    public @ResponseBody Iterable<ReconciliationFileDTO> reconcileDataLoadInt(@RequestBody ReconciliationFileLoadDTO reconciliationFileLoad) throws IOException {
        LOG.info("Request to load file - {}", reconciliationFileLoad.getFilename());
        reconciliationManager.loadFile(reconciliationFileLoad, reconciliationFileManager);
        return getListOfFiles();
    }

    @GetMapping(path = "/int/money/reconciliation/files")
    public @ResponseBody Iterable<ReconciliationFileDTO> getListOfFiles() {
        LOG.info("Request to get list of files");
        return reconciliationFileManager.getFiles();
    }

    @PutMapping(path = "/ext/money/reconciliation/update")
    public @ResponseBody OkStatus reconcileCategoryExt(@RequestBody ReconcileUpdateDTO reconciliationUpdate) {
        LOG.info("Reconcile Category Update");
        reconciliationManager.processReconcileUpdate(reconciliationUpdate);
        return OkStatus.getOkStatus();
    }

    @PutMapping(path = "/int/money/reconciliation/update")
    public @ResponseBody OkStatus reconcileCategoryInt(@RequestBody ReconcileUpdateDTO reconciliationUpdate) {
        return reconcileCategoryExt(reconciliationUpdate);
    }

    @GetMapping(path = "/ext/money/match")
    public @ResponseBody List<MatchDataDTO> matchExt(@RequestParam(value = "account", defaultValue = "UNKN") String accountId) throws UpdateDeleteAccountException {
        LOG.info("External match data - reconciliation data with reconciled transactions");
        return reconciliationManager.matchImpl(accountId);
    }

    @GetMapping(path = "/int/money/match")
    public @ResponseBody List<MatchDataDTO> matchInt(@RequestParam(value = "account", defaultValue = "UNKN") String accountId) throws UpdateDeleteAccountException {
        return matchExt(accountId);
    }

    @PutMapping(path = "/ext/money/reconciliation/auto")
    public @ResponseBody OkStatus reconcileDataExt() throws MultipleUnlockedStatementException, InvalidTransactionIdException, InvalidTransactionException {
        LOG.info("Auto Reconciliation Data (ext) ");
        reconciliationManager.autoReconcileData();
        return OkStatus.getOkStatus();
    }

    @PutMapping(path = "/int/money/reconciliation/auto")
    public @ResponseBody OkStatus reconcileDataInt() throws MultipleUnlockedStatementException, InvalidTransactionIdException, InvalidTransactionException {
        return reconcileDataExt();
    }

    @DeleteMapping(path = "/ext/money/reconciliation/clear")
    public @ResponseBody OkStatus reconcileDataDeleteExt() {
        LOG.info("Clear Reconciliation Data (ext) ");
        reconciliationManager.clearRepositoryData();
        return OkStatus.getOkStatus();
    }

    @DeleteMapping(path = "/int/money/reconciliation/clear")
    public @ResponseBody OkStatus reconcileDataDeleteInt() {
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
