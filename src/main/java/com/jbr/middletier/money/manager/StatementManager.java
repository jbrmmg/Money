package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.data.StatementId;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dto.StatementDTO;
import com.jbr.middletier.money.dto.StatementIdDTO;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.util.FinancialAmount;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
public class StatementManager {
    private static final Logger LOG = LoggerFactory.getLogger(StatementManager.class);

    private final StatementRepository statementRepository;
    private final AccountRepository accountRepository;
    private final AccountTransactionManager accountTransactionManager;
    private final ModelMapper modelMapper;

    public StatementManager(StatementRepository statementRepository,
                            AccountRepository accountRepository,
                            AccountTransactionManager accountTransactionManager,
                            ModelMapper modelMapper) {
        this.statementRepository = statementRepository;
        this.accountRepository = accountRepository;
        this.accountTransactionManager = accountTransactionManager;
        this.modelMapper = modelMapper;
    }

    private Account getAccount(StatementDTO statement) throws InvalidAccountIdException {
        if(statement.getId() == null) {
            throw new InvalidAccountIdException("Null (Statement Id)");
        }

        if(statement.getId().getAccount() == null) {
            throw new InvalidAccountIdException("Null");
        }

        Optional<Account> account = accountRepository.findById(statement.getId().getAccount().getId());
        if(!account.isPresent()) {
            throw new InvalidAccountIdException(statement.getId().getAccount().getId());
        }

        return account.get();
    }

    private boolean returnStatement(Statement statement, String accountId, Boolean locked) {
        if(accountId != null) {
            if(!statement.getId().getAccount().getId().equals(accountId)) {
                return false;
            }
        }

        if(locked != null) {
            return statement.getLocked() == locked;
        }

        return true;
    }

    public List<StatementDTO> getStatements(String accountId, Boolean locked) {
        LOG.info("Get statements. {} {}", accountId, locked);

        List<StatementDTO> statementList = new ArrayList<>();
        for(Statement nextStatement : statementRepository.findAllByOrderByIdAccountAsc()){
            if(returnStatement(nextStatement,accountId,locked)) {
                statementList.add(modelMapper.map(nextStatement, StatementDTO.class));
            }
        }

        // Sort the list by the backup time.
        Collections.sort(statementList);

        return statementList;
    }

    public Iterable<StatementDTO> statementLock(StatementIdDTO request) throws InvalidStatementIdException, StatementAlreadyLockedException {
        LOG.info("Request statement lock.");

        // Get the internal representation.
        StatementId statementId = modelMapper.map(request,StatementId.class);

        // Load the statement to be locked.
        Optional<Statement> statement = statementRepository.findById(statementId);

        if(statement.isPresent()) {
            // Is the statement already locked?
            if(!statement.get().getLocked()) {
                // Calculate the balance of the next statement.
                FinancialAmount balance = accountTransactionManager.getFinalBalanceForStatement(statement.get());

                // Create a new statement.
                Statement newStatement = statement.get().lock(balance);

                // Update existing statement and create new one.
                statementRepository.save(statement.get());
                statementRepository.save(newStatement);
                LOG.info("Request statement lock - locked.");
            } else {
                throw new StatementAlreadyLockedException(statementId);
            }
        } else {
            throw new InvalidStatementIdException(statementId);
        }

        return getStatements(request.getAccount().getId(),null);
    }

    public Iterable<StatementDTO> createStatement(StatementDTO statement) throws InvalidAccountIdException, StatementAlreadyExists {
        Account account = getAccount(statement);

        // Get the statements currently available for this account.
        List<StatementDTO> statements = getStatements(account.getId(),null);

        // Can only create a statement if there are none.
        if(!statements.isEmpty()) {
            LOG.warn("Statements can only be created where none exist.");
            throw new StatementAlreadyExists(statements.get(0));
        }

        Statement newStatement = modelMapper.map(statement,Statement.class);
        this.statementRepository.save(newStatement);

        return getStatements(account.getId(),null);
    }

    @Transactional
    protected void deleteStatementTransaction(StatementDTO last, StatementDTO penultimate) {
        penultimate.setLocked(false);
        statementRepository.save(modelMapper.map(penultimate,Statement.class));

        accountTransactionManager.removeTransactionsFromStatement(modelMapper.map(last,Statement.class));

        statementRepository.delete(modelMapper.map(last,Statement.class));
    }

    @Transactional
    protected void internalDeleteStatement(StatementDTO statement, List<StatementDTO> statements) throws InvalidStatementIdException, CannotDeleteLockedStatement, CannotDeleteLastStatement {
        // Only the last statement in the list can be deleted
        if(statements.isEmpty()) {
            LOG.warn("There are no statements for the account.");
            throw new InvalidStatementIdException(statement);
        }

        // We cannot delete the last statement.
        if(statements.size() < 2) {
            LOG.warn("Cannot request delete if only one statement.");
            throw new CannotDeleteLastStatement(statement);
        }

        // The last statement should be unlocked.
        StatementDTO last = statements.get(statements.size()-1);
        StatementDTO penultimate = statements.get(statements.size()-2);

        if(!last.getId().equals(statement.getId())) {
            LOG.warn("Delete request is not for the last statement.");
            throw new CannotDeleteLockedStatement(statement);
        }

        if(last.getLocked() || !penultimate.getLocked()) {
            LOG.warn("Either the last statement is locked, or the penultimate is not locked.");
            throw new CannotDeleteLockedStatement(statement);
        }

        // Remove any transactions from this statement.
        deleteStatementTransaction(last, penultimate);
    }

    @Transactional
    public Iterable<StatementDTO> deleteStatement(StatementDTO statement) throws InvalidAccountIdException, InvalidStatementIdException, CannotDeleteLockedStatement, CannotDeleteLastStatement {
        Account account = getAccount(statement);

        // Get the statements currently available for this account.
        List<StatementDTO> statements = getStatements(account.getId(),null);

        // Perform the delete
        internalDeleteStatement(statement, statements);
        return statements;
    }
}
