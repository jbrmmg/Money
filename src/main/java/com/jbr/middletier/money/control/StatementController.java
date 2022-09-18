package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.StatementDTO;
import com.jbr.middletier.money.exceptions.InvalidStatementIdException;
import com.jbr.middletier.money.exceptions.StatementAlreadyExists;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Created by jason on 07/03/17.
 */
@Controller
@RequestMapping("/jbr")
public class StatementController {
    private static final Logger LOG = LoggerFactory.getLogger(StatementController.class);

    private final StatementRepository statementRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public StatementController(StatementRepository statementRepository,
                               TransactionRepository transactionRepository,
                               AccountRepository accountRepository, ModelMapper modelMapper) {
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.modelMapper = modelMapper;
    }

    @ExceptionHandler(Exception.class)
    public void handleException(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value());
    }

    private Iterable<StatementDTO> statements() {
        LOG.info("Get statements.");

        List<StatementDTO> statementList = new ArrayList<>();
        for(Statement nextStatement : statementRepository.findAllByOrderByIdAccountAsc()){
            statementList.add(modelMapper.map(nextStatement,StatementDTO.class));
        }

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
                throw new IllegalStateException("Request statement lock - statement already locked");
            }
        } else {
            throw new IllegalStateException("Request statement lock - invalid statement id");
        }
    }

    @GetMapping(path="/ext/money/statement")
    public @ResponseBody
    Iterable<StatementDTO>  statementsExt() {
        return statements();
    }

    @GetMapping(path="/int/money/statement")
    public @ResponseBody
    Iterable<StatementDTO>  statementsInt() {
        return statements();
    }

    @PostMapping(path="/ext/money/statement/lock")
    public @ResponseBody OkStatus statementLockExt(@RequestBody LockStatementRequest request) {
        statementLock(request);

        return OkStatus.getOkStatus();
    }

    @PostMapping(path="/int/money/statement/lock")
    public @ResponseBody OkStatus statementLockInt(@RequestBody LockStatementRequest request) {
        statementLock(request);

        return OkStatus.getOkStatus();
    }

    @PostMapping(path="/int/money/statement")
    public @ResponseBody Iterable<StatementDTO> createStatement(@RequestBody StatementDTO statement) throws StatementAlreadyExists {
        LOG.info("Create a new statement - {}", statement.toString());

        // Is there an account with this ID?
        Optional<Statement> existingStatement = statementRepository.findById(modelMapper.map(statement.getId(),StatementId.class));
        if(existingStatement.isPresent()) {
            throw new StatementAlreadyExists(statement);
        }

        statementRepository.save(modelMapper.map(statement,Statement.class));

        return statements();
    }

    @PutMapping(path="/int/money/statement")
    public @ResponseBody Iterable<StatementDTO> updateStatement(@RequestBody StatementDTO statement) throws InvalidStatementIdException {
        LOG.info("Update a statement - {}", statement.toString());

        // Is there a statement with this
        Optional<Statement> existingStatement = statementRepository.findById(modelMapper.map(statement.getId(), StatementId.class));
        if(existingStatement.isPresent()) {
            existingStatement.get().setOpenBalance(statement.getOpenBalance());

            LOG.warn("Statement balance updated.");

            statementRepository.save(existingStatement.get());
        } else {
            throw new InvalidStatementIdException(statement);
        }

        return statements();
    }

    @DeleteMapping(path="/int/money/statement")
    public @ResponseBody OkStatus deleteStatement(@RequestBody StatementDTO statement) throws InvalidStatementIdException {
        LOG.info("Delete an account - {}", statement.getId().toString());

        // Is there an account with this ID?
        Optional<Statement> existingStatement = statementRepository.findById(modelMapper.map(statement.getId(), StatementId.class));
        if(existingStatement.isPresent()) {

            statementRepository.delete(existingStatement.get());
            return OkStatus.getOkStatus();
        }

        throw new InvalidStatementIdException(statement);
    }
}
