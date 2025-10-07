package com.jbr.middletier.money.control;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.dto.TransactionReportDTO;
import com.jbr.middletier.money.dto.VersionDTO;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.manager.AccountTransactionManager;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Created by jason on 08/03/17.
 */
@RestController
@RequestMapping("/jbr")
@Validated
public class TransactionController {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionController.class);

    private final AccountTransactionManager accountTransactionManager;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public TransactionController(AccountTransactionManager accountTransactionManager, ApplicationProperties applicationProperties) {
        this.accountTransactionManager = accountTransactionManager;
        this.applicationProperties = applicationProperties;
    }

    @PostMapping(path="/ext/money/transaction")
    public Iterable<TransactionDTO>  addTransactionExt(@Valid @RequestBody List<TransactionDTO> transaction) throws InvalidTransactionException {
        LOG.trace("Add transaction (E).");
        return this.accountTransactionManager.createTransaction(transaction);
    }

    @PostMapping(path="/int/money/transaction")
    public Iterable<TransactionDTO>  addTransactionInt(@Valid @RequestBody List<TransactionDTO> transaction) throws InvalidTransactionException {
        LOG.trace("Add transaction (I).");
        return this.accountTransactionManager.createTransaction(transaction);
    }

    @PutMapping(path="/ext/money/transaction")
    public Iterable<TransactionDTO> updateTransactionExt(@Valid @RequestBody List<TransactionReportDTO> transactions) throws InvalidTransactionException {
        return this.accountTransactionManager.updateTransactions(transactions);
    }

    @PutMapping(path="/int/money/transaction")
    public Iterable<TransactionDTO> updateTransactionInt(@Valid @RequestBody List<TransactionReportDTO> transactions) throws InvalidTransactionException {
        return this.accountTransactionManager.updateTransactions(transactions);
    }

    @DeleteMapping(path="/ext/money/transaction")
    public Iterable<TransactionDTO> deleteExternal(@Valid @RequestBody List<TransactionDTO> transactions) throws InvalidTransactionIdException {
        return this.accountTransactionManager.deleteTransactions(transactions);
    }

    @DeleteMapping(path="/int/money/transaction")
    public Iterable<TransactionDTO> deleteInternal(@Valid @RequestBody List<TransactionDTO> transactions) throws InvalidTransactionIdException {
        return this.accountTransactionManager.deleteTransactions(transactions);
    }

    @GetMapping("/version")
    public VersionDTO getVersion() {
        VersionDTO version = new VersionDTO();
        version.setVersion(applicationProperties.getVersion());

        return version;
    }
}
