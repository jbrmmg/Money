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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Created by jason on 07/03/17.
 */
@Controller
@RequestMapping("/jbr")
public class StatementController {
    final static private Logger LOG = LoggerFactory.getLogger(StatementController.class);

    private final StatementRepository statementRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public StatementController(StatementRepository statementRepository,
                               TransactionRepository transactionRepository) {
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
    }

    @ExceptionHandler(IllegalStateException.class)
    public void handleIllegalArgumentException(IllegalStateException e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value());
    }

    private Iterable<Statement> statements() {
        LOG.info("Get statements.");

        List<Statement> statementList = statementRepository.findAllByOrderByAccountAsc();

        // Sort the list by the backup time.
        Collections.sort(statementList);

        return statementList;
    }

    private void statementLock(LockStatementRequest request) {
        LOG.info("Request statement lock.");

        // Load the statement to be locked.
        Optional<Statement> statement = statementRepository.findById(new StatementId(request.getAccountId(),request.getYear(),request.getMonth()));

        if(statement.isPresent()) {
            // Is the statement already locked?
            if(!statement.get().getLocked()) {
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

    @RequestMapping(path="/ext/money/statement", method= RequestMethod.GET)
    public @ResponseBody
    Iterable<Statement>  statementsExt() {
        return statements();
    }

    @RequestMapping(path="/int/money/statement", method= RequestMethod.GET)
    public @ResponseBody
    Iterable<Statement>  statementsInt() {
        return statements();
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

    @RequestMapping(path="/int/money/statement",method=RequestMethod.POST)
    public @ResponseBody Iterable<Statement> createStatement(@RequestBody Statement statement) throws Exception {
        StatementId statementId = new StatementId(statement.getAccount(),statement.getYear(),statement.getMonth());
        LOG.info("Create a new account - " + statementId.toString());

        // Is there an account with this ID?
        Optional<Statement> existingStatement = statementRepository.findById(statementId);
        if(existingStatement.isPresent()) {
            throw new Exception(statementId.toString() + " already exists");
        }

        statementRepository.save(statement);

        return statements();
    }

    @RequestMapping(path="/int/money/statement",method=RequestMethod.PUT)
    public @ResponseBody Iterable<Statement> updateStatement(@RequestBody Statement statement) throws Exception {
        StatementId statementId = new StatementId(statement.getAccount(),statement.getYear(),statement.getMonth());
        LOG.info("Update an account - " + statementId.toString());

        // Is there a statement with this
        Optional<Statement> existingStatement = statementRepository.findById(statementId);
        if(existingStatement.isPresent()) {
            existingStatement.get().setOpenBalance(statement.getOpenBalance());

            LOG.warn("Statement balance updated.");

            statementRepository.save(existingStatement.get());
        } else {
            throw new Exception(statementId.toString() + " cannot find statement.");
        }

        return statements();
    }

    @RequestMapping(path="/int/money/statement",method=RequestMethod.DELETE)
    public @ResponseBody
    StatusResponse deleteStatement(@RequestBody Statement statement) {
        StatementId statementId = new StatementId(statement.getAccount(),statement.getYear(),statement.getMonth());
        LOG.info("Delete an account - " + statementId.toString());

        // Is there an account with this ID?
        Optional<Statement> existingStatement = statementRepository.findById(statementId);
        if(existingStatement.isPresent()) {

            statementRepository.delete(existingStatement.get());
            return new StatusResponse();
        }

        return new StatusResponse("Category does not exist " + statementId);
    }
}
