package com.jbr.middletier.money.control;

import com.jbr.middletier.money.dto.TransactionDataDTO;
import com.jbr.middletier.money.dto.TransactionFilterDTO;
import com.jbr.middletier.money.manager.TransactionReportManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/jbr")
public class TransactionReportController {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionReportController.class);

    private final TransactionReportManager accountTransactionManager;

    @Autowired
    public TransactionReportController(TransactionReportManager accountTransactionManager) {
        this.accountTransactionManager = accountTransactionManager;
    }

    @GetMapping(path="/ext/money/transaction2")
    @ResponseBody TransactionDataDTO getTransactionsExternal(@RequestBody TransactionFilterDTO filter) {
        return this.accountTransactionManager.getTransactions(filter);
    }

    @GetMapping(path="/int/money/transaction2")
    @ResponseBody TransactionDataDTO getTransactionsInternal(@RequestBody TransactionFilterDTO filter) {
        return this.accountTransactionManager.getTransactions(filter);
    }
}
