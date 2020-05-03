package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.exceptions.InvalidStatementIdException;
import com.jbr.middletier.money.manage.WebLogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final AccountRepository accountRepository;
    private final WebLogManager webLogManager;

    @Autowired
    public StatementController(StatementRepository statementRepository,
                               TransactionRepository transactionRepository,
                               AccountRepository accountRepository,
                               WebLogManager webLogManager) {
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.webLogManager = webLogManager;
    }

    @ExceptionHandler(Exception.class)
    public void handleException(Exception e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value());
    }

    private Iterable<Statement> statements() {
        LOG.info("Get statements.");

        List<Statement> statementList = statementRepository.findAllByOrderByIdAccountAsc();

        // Sort the list by the backup time.
        Collections.sort(statementList);

        return statementList;
    }

    private void statementLock(LockStatementRequest request) {
        LOG.info("Request statement lock.");

        // Get the account.
        Optional<Account> account = accountRepository.findById(request.getAccountId());
        if(!account.isPresent()) {
            throw new IllegalStateException("Request statement lock - invalid account");
        }

        webLogManager.postWebLog(WebLogManager.webLogLevel.INFO, "Statement Lock - " + request.getAccountId());

        // Load the statement to be locked.
        Optional<Statement> statement = statementRepository.findById(new StatementId(account.get(),request.getYear(),request.getMonth()));

        if(statement.isPresent()) {
            // Is the statement already locked?
            if(!statement.get().getLocked()) {
                DecimalFormat decimalFormat = new DecimalFormat("#.00");

                // Calculate the balance of the next statement.
                List<Transaction> transactions = transactionRepository.findByAccountAndStatementIdYearAndStatementIdMonth(
                        statement.get().getId().getAccount(),
                        statement.get().getId().getYear(),
                        statement.get().getId().getMonth());

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
                webLogManager.postWebLog(WebLogManager.webLogLevel.ERROR, "Request statement lock - statement already locked");
                throw new IllegalStateException("Request statement lock - statement already locked");
            }
        } else {
            webLogManager.postWebLog(WebLogManager.webLogLevel.ERROR, "Request statement lock - invalid statement id");
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
    public @ResponseBody OkStatus statementLockExt(@RequestBody LockStatementRequest request) {
        statementLock(request);

        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/int/money/statement/lock", method= RequestMethod.POST)
    public @ResponseBody OkStatus statementLockInt(@RequestBody LockStatementRequest request) {
        statementLock(request);

        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/int/money/statement",method=RequestMethod.POST)
    public @ResponseBody Iterable<Statement> createStatement(@RequestBody Statement statement) throws Exception {
        LOG.info("Create a new statement - " + statement.toString());

        // Is there an account with this ID?
        Optional<Statement> existingStatement = statementRepository.findById(statement.getId());
        if(existingStatement.isPresent()) {
            throw new Exception(statement.getId().toString() + " already exists");
        }

        statementRepository.save(statement);

        return statements();
    }

    @RequestMapping(path="/int/money/statement",method=RequestMethod.PUT)
    public @ResponseBody Iterable<Statement> updateStatement(@RequestBody Statement statement) throws Exception {
        LOG.info("Update a statement - " + statement.toString());

        // Is there a statement with this
        Optional<Statement> existingStatement = statementRepository.findById(statement.getId());
        if(existingStatement.isPresent()) {
            existingStatement.get().setOpenBalance(statement.getOpenBalance());

            LOG.warn("Statement balance updated.");

            statementRepository.save(existingStatement.get());
        } else {
            throw new Exception(statement.getId().toString() + " cannot find statement.");
        }

        return statements();
    }

    @RequestMapping(path="/int/money/statement",method=RequestMethod.DELETE)
    public @ResponseBody OkStatus deleteStatement(@RequestBody Statement statement) throws InvalidStatementIdException {
        LOG.info("Delete an account - " + statement.getId().toString());

        // Is there an account with this ID?
        Optional<Statement> existingStatement = statementRepository.findById(statement.getId());
        if(existingStatement.isPresent()) {

            statementRepository.delete(existingStatement.get());
            return OkStatus.getOkStatus();
        }

        throw new InvalidStatementIdException(statement);
    }
}
