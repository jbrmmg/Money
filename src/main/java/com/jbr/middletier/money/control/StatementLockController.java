package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;

/**
 * Created by jason on 08/03/17.
 */
@Controller
@RequestMapping("/jbr")
public class StatementLockController {
    final static private Logger LOG = LoggerFactory.getLogger(StatementLockController.class);

    private final StatementRepository statementRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public StatementLockController(StatementRepository statementRepository,
                                   TransactionRepository transactionRepository) {
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
    }

    @ExceptionHandler(IllegalStateException.class)
    public void handleIllegalArgumentException(IllegalStateException e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value());
    }

    private void statementLock(LockStatementRequest request) {
        LOG.info("Request statement lock.");

        // Load the statement to be locked.
        Optional<Statement> statement = statementRepository.findById(new StatementId(request.getAccountId(),request.getYear(),request.getMonth()));

        if(statement.isPresent()) {
            // Is the statement already locked?
            if(statement.get().getNotLocked()) {
                DecimalFormat decimalFormat = new DecimalFormat("#.00");

                // Calculate the balance of the next statement.
                List<Transaction> transactions = transactionRepository.findByAccountAndStatement(statement.get().getAccount(),statement.get().getYearMonthId());

                double balance = statement.get().getOpenBalance();

                for (Transaction nextTransaction : transactions ) {
                    balance += nextTransaction.getAmount();
                    LOG.debug(String.format("Transaction %s",decimalFormat.format(nextTransaction.getAmount())));
                }

                LOG.info(String.format("Balance: %s",decimalFormat.format(balance)));

                // Create a new statement.
                Statement newStatement = statement.get().lock(balance);

                // Update existing statement and create new one.
                statementRepository.save(statement.get());
                statementRepository.save(newStatement);
                LOG.info("Request statement lock - locked.");
            } else {
                throw new IllegalStateException("Request statement lock - statement already locked");
            }
        } else {
            throw new IllegalStateException("Request statement lock - invalid statement id");
        }
    }

    @RequestMapping(path="/ext/money/statement/lock", method= RequestMethod.POST)
    public @ResponseBody
    StatusResponse statementLockExt(@RequestBody LockStatementRequest request) {
        statementLock(request);
        return new StatusResponse();
    }

    @RequestMapping(path="/int/money/statement/lock", method= RequestMethod.POST)
    public @ResponseBody StatusResponse statementLockInt( @RequestBody LockStatementRequest request) {
        statementLock(request);
        return new StatusResponse();
    }
}
