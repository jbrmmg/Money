package com.jbr.middletier.money.control;

import com.jbr.middletier.money.dto.TransactionFilterDTO;
import com.jbr.middletier.money.dto.TransactionReportDTO;
import com.jbr.middletier.money.manager.TransactionReportManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Transaction Reports", description = "Filtered transaction listing and report data reset")
public class TransactionReportController {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionReportController.class);

    private final TransactionReportManager accountTransactionManager;

    @Autowired
    public TransactionReportController(TransactionReportManager accountTransactionManager) {
        this.accountTransactionManager = accountTransactionManager;
    }

    @Operation(summary = "List transactions matching the supplied filter criteria")
    @PostMapping(path="/transaction/list")
    public List<TransactionReportDTO> getTransactions(@Valid @RequestBody TransactionFilterDTO filter) {
        LOG.trace("Transaction report");
        return this.accountTransactionManager.getTransactions(filter);
    }

    @Operation(summary = "Reset cached transaction report data")
    @PostMapping(path="/transaction/reset")
    public void resetTransactions() {
        LOG.info("Reset the transaction data");
        this.accountTransactionManager.reset();
    }
}
