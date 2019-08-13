package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.ReconcileTransaction;
import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.data.StatusResponse;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Created by jason on 07/03/17.
 */
@Controller
@RequestMapping("/jbr")
public class ReconciliationController {
    final static private Logger LOG = LoggerFactory.getLogger(ReconciliationController.class);

    private final TransactionRepository transactionRepository;
    private final StatementRepository statementRepository;

    @Autowired
    public ReconciliationController(StatementRepository statementRepository,
                                    TransactionRepository transactionRepository) {
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
    }

    private boolean reconcile (int transactionId, boolean reconcile) {
        LOG.info("Reconcile transaction.");

        // Get the transaction.
        Optional<Transaction> transaction = transactionRepository.findById(transactionId);

        if(transaction.isPresent()) {
            // Then set the reconciliation or remove the flag.
            if (reconcile) {
                // Find the statement associated with the transaction.
                List<Statement> statements = statementRepository.findByAccountAndLocked(transaction.get().getAccount(), "N");

                if (statements.size() == 1) {
                    // Set the statement.
                    transaction.get().setStatement(statements.get(0));
                } else {
                    LOG.info("Reconcile transaction - ignored (statement count not 1).");
                    return false;
                }
            } else if (!reconcile) {
                // Remove the statement
                transaction.get().clearStatement();
            } else {
                // Do nothing.
                LOG.info("Reconcile transaction - ignored (invalid reconcile).");
                return false;
            }

            // Save the transaction.
            transactionRepository.save(transaction.get());
            return true;
        }

        return false;
    }

    @RequestMapping(path="/ext/money/reconcile", method= RequestMethod.POST)
    public @ResponseBody StatusResponse reconcileExt(@RequestBody ReconcileTransaction reconcileTransaction) {
        if(reconcile(reconcileTransaction.getTransactionId(),reconcileTransaction.getReconcile())) {
            return new StatusResponse();
        }

        return new StatusResponse("Failed to reconcile transaction");
    }

    @RequestMapping(path="/int/money/reconcile", method= RequestMethod.POST)
    public @ResponseBody StatusResponse  reconcileInt(@RequestBody ReconcileTransaction reconcileTransaction) {
        if(reconcile(reconcileTransaction.getTransactionId(),reconcileTransaction.getReconcile())) {
            return new StatusResponse();
        }

        return new StatusResponse("Failed to reconcile transaction");
    }
}
