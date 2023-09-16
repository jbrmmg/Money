package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dto.ReconcileTransactionDTO;
import com.jbr.middletier.money.dto.ReconcileUpdateDTO;
import com.jbr.middletier.money.dto.ReconciliationFileDTO;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.manager.ReconciliationFileManager;
import com.jbr.middletier.money.manager.ReconciliationManager;
import com.jbr.middletier.money.dto.MatchDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.io.*;
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

    @Autowired
    public ReconciliationController(ReconciliationFileManager reconciliationFileManager,
                                    ReconciliationManager reconciliationManager) {
        this.reconciliationFileManager = reconciliationFileManager;
        this.reconciliationManager = reconciliationManager;
    }

    @PutMapping(path="/ext/money/reconcile")
    public @ResponseBody OkStatus reconcileExt(@RequestBody ReconcileTransactionDTO reconcileTransaction) throws InvalidTransactionIdException, MultipleUnlockedStatementException {
        reconciliationManager.reconcile(reconcileTransaction.getTransactionId(),reconcileTransaction.getReconcile());
        return OkStatus.getOkStatus();
    }

    @PutMapping(path="/int/money/reconcile")
    public @ResponseBody OkStatus reconcileInt(@RequestBody ReconcileTransactionDTO reconcileTransaction) throws InvalidTransactionIdException, MultipleUnlockedStatementException {
        return reconcileExt(reconcileTransaction);
    }

    @PostMapping(path="/int/money/reconciliation/load")
    public @ResponseBody Iterable<ReconciliationFileDTO> reconcileDataLoadInt(@RequestBody ReconciliationFileDTO reconciliationFile) throws IOException {
        LOG.info("Request to load file - {}", reconciliationFile.getFilename());
        reconciliationManager.loadFile(reconciliationFile,reconciliationFileManager);
        return getListOfFiles();
    }

    @GetMapping(path="/int/money/reconciliation/files")
    public @ResponseBody Iterable<ReconciliationFileDTO> getListOfFiles() {
        LOG.info("Request to get list of files");
        return reconciliationFileManager.getFiles();
    }

    @PutMapping(path="/ext/money/reconciliation/update")
    public @ResponseBody OkStatus reconcileCategoryExt(@RequestBody ReconcileUpdateDTO reconciliationUpdate ) {
        LOG.info("Reconcile Category Update");
        reconciliationManager.processReconcileUpdate(reconciliationUpdate);
        return OkStatus.getOkStatus();
    }

    @PutMapping(path="/int/money/reconciliation/update")
    public @ResponseBody OkStatus reconcileCategoryInt(@RequestBody ReconcileUpdateDTO reconciliationUpdate) {
        return reconcileCategoryExt(reconciliationUpdate);
    }

    @GetMapping(path="/ext/money/match")
    public @ResponseBody List<MatchDataDTO> matchExt(@RequestParam(value="account", defaultValue="UNKN") String accountId) throws UpdateDeleteAccountException {
        LOG.info("External match data - reconciliation data with reconciled transactions");
        return reconciliationManager.matchImpl(accountId);
    }

    @GetMapping(path="/int/money/match")
    public @ResponseBody List<MatchDataDTO> matchInt(@RequestParam(value="account", defaultValue="UNKN") String accountId) throws UpdateDeleteAccountException {
        return matchExt(accountId);
    }

    @PutMapping(path="/ext/money/reconciliation/auto")
    public @ResponseBody OkStatus reconcileDataExt() throws MultipleUnlockedStatementException, InvalidTransactionIdException, InvalidTransactionException {
        LOG.info("Auto Reconciliation Data (ext) ");
        reconciliationManager.autoReconcileData();
        return OkStatus.getOkStatus();
    }

    @PutMapping(path="/int/money/reconciliation/auto")
    public @ResponseBody OkStatus reconcileDataInt() throws MultipleUnlockedStatementException, InvalidTransactionIdException, InvalidTransactionException {
        return reconcileDataExt();
    }

    @DeleteMapping(path="/ext/money/reconciliation/clear")
    public @ResponseBody OkStatus reconcileDataDeleteExt() {
        LOG.info("Clear Reconciliation Data (ext) ");
        reconciliationManager.clearRepositoryData();
        return OkStatus.getOkStatus();
    }

    @DeleteMapping(path="/int/money/reconciliation/clear")
    public @ResponseBody OkStatus reconcileDataDeleteInt() {
        return this.reconcileDataDeleteExt();
    }
}
