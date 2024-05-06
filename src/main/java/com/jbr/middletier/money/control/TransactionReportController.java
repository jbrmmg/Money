package com.jbr.middletier.money.control;

import com.jbr.middletier.money.dto.TransactionDataDTO;
import com.jbr.middletier.money.dto.TransactionFilterDTO;
import com.jbr.middletier.money.manager.TransactionReportManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jbr")
public class TransactionReportController {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionReportController.class);

    private final TransactionReportManager accountTransactionManager;

    @Autowired
    public TransactionReportController(TransactionReportManager accountTransactionManager) {
        this.accountTransactionManager = accountTransactionManager;
    }

    @GetMapping(path="/ext/money/transaction/list")
    TransactionDataDTO getTransactionsExternal(@RequestBody TransactionFilterDTO filter) {
        LOG.trace("EXT: transaction report");
        return this.accountTransactionManager.getTransactions(filter);
    }

    @GetMapping(path="/int/money/transaction/list")
    TransactionDataDTO getTransactionsInternal(@RequestBody TransactionFilterDTO filter) {
        LOG.trace("INT: transaction report");
        return this.accountTransactionManager.getTransactions(filter);
    }
}
