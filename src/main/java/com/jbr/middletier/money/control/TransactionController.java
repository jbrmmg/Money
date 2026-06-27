package com.jbr.middletier.money.control;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.dto.TransactionReportDTO;
import com.jbr.middletier.money.dto.VersionDTO;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.manager.AccountTransactionManager;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Transactions", description = "Financial transaction creation, update, and deletion")
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

    @PostMapping(path="/transaction")
    public Iterable<TransactionDTO> addTransaction(@Valid @RequestBody List<TransactionDTO> transaction) throws InvalidTransactionException {
        LOG.trace("Add transaction.");
        return this.accountTransactionManager.createTransaction(transaction);
    }

    @PutMapping(path="/transaction")
    public Iterable<TransactionDTO> updateTransaction(@Valid @RequestBody List<TransactionReportDTO> transactions) throws InvalidTransactionException {
        return this.accountTransactionManager.updateTransactions(transactions);
    }

    @DeleteMapping(path="/transaction")
    public Iterable<TransactionDTO> deleteTransaction(@Valid @RequestBody List<TransactionDTO> transactions) throws InvalidTransactionIdException {
        return this.accountTransactionManager.deleteTransactions(transactions);
    }

    @GetMapping("/version")
    public VersionDTO getVersion() {
        VersionDTO version = new VersionDTO();
        version.setVersion(applicationProperties.getVersion());
        return version;
    }
}
