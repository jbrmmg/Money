package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.data.StatementId;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dto.StatementDTO;
import com.jbr.middletier.money.dto.StatementIdDTO;
import com.jbr.middletier.money.dto.mapper.StatementMapper;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.util.FinancialAmount;
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
    private final AccountManager accountManager;
    private final StatementMapper statementMapper;

    public StatementManager(StatementRepository statementRepository,
                            AccountManager accountManager,
                            StatementMapper statementMapper) {
        this.statementRepository = statementRepository;
        this.accountManager = accountManager;
        this.statementMapper = statementMapper;
    }

    private Account getAccount(StatementDTO statement) throws UpdateDeleteAccountException {
        Statement internalStatement = statementMapper.map(statement,Statement.class);

        if(internalStatement.getId() == null) {
            throw new UpdateDeleteAccountException("Null (Statement Id)");
        }

        if(internalStatement.getId().getAccount() == null) {
            throw new UpdateDeleteAccountException(statement.getAccountId());
        }

        Optional<Account> account = accountManager.getIfValid(internalStatement.getId().getAccount().getId());
        if(account.isEmpty()) {
            throw new UpdateDeleteAccountException(internalStatement.getId().getAccount().getId());
        }

        return account.get();
    }

    private boolean returnStatement(Statement statement, String accountId, Boolean locked) {
        if((accountId != null) && (!statement.getId().getAccount().getId().equals(accountId))) {
            return false;
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
                statementList.add(statementMapper.map(nextStatement, StatementDTO.class));
            }
        }

        // Sort the list by the backup time.
        Collections.sort(statementList);

        return statementList;
    }

    public Iterable<StatementDTO> statementLock(StatementIdDTO request, AccountTransactionManager accountTransactionManager) throws InvalidStatementIdException, StatementAlreadyLockedException {
        LOG.info("Request statement lock.");

        // Get the internal representation.
        StatementId statementId = statementMapper.map(request,StatementId.class);

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

        return getStatements(statementId.getAccount().getId(),null);
    }

    public Iterable<StatementDTO> createStatement(StatementDTO statement) throws UpdateDeleteAccountException, StatementAlreadyExistsException {
        Account account = getAccount(statement);

        // Get the statements currently available for this account.
        List<StatementDTO> statements = getStatements(account.getId(),null);

        // Can only create a statement if there are none.
        if(!statements.isEmpty()) {
            LOG.warn("Statements can only be created where none exist.");
            throw new StatementAlreadyExistsException(statements.get(0));
        }

        Statement newStatement = statementMapper.map(statement,Statement.class);
        this.statementRepository.save(newStatement);

        return getStatements(account.getId(),null);
    }

    @Transactional
    public void deleteStatementTransaction(StatementDTO last, StatementDTO penultimate, AccountTransactionManager accountTransactionManager) {
        penultimate.setLocked(false);
        statementRepository.save(statementMapper.map(penultimate,Statement.class));

        accountTransactionManager.removeTransactionsFromStatement(statementMapper.map(last,Statement.class));

        statementRepository.delete(statementMapper.map(last,Statement.class));
    }

    @Transactional
    public void internalDeleteStatement(StatementDTO statement, List<StatementDTO> statements, AccountTransactionManager accountTransactionManager) throws InvalidStatementIdException, CannotDeleteLockedStatementException, CannotDeleteLastStatementException {
        // Only the last statement in the list can be deleted
        if(statements.isEmpty()) {
            LOG.warn("There are no statements for the account.");
            throw new InvalidStatementIdException(statement);
        }

        // We cannot delete the last statement.
        if(statements.size() < 2) {
            LOG.warn("Cannot request delete if only one statement.");
            throw new CannotDeleteLastStatementException(statement);
        }

        // The last statement should be unlocked.
        StatementDTO last = statements.get(statements.size()-1);
        StatementDTO penultimate = statements.get(statements.size()-2);

        if(!last.equals(statement)) {
            LOG.warn("Delete request is not for the last statement.");
            throw new CannotDeleteLockedStatementException(statement);
        }

        if(last.getLocked() || !penultimate.getLocked()) {
            LOG.warn("Either the last statement is locked, or the penultimate is not locked.");
            throw new CannotDeleteLockedStatementException(statement);
        }

        // Remove any transactions from this statement.
        deleteStatementTransaction(last, penultimate, accountTransactionManager);
    }

    @Transactional
    public Iterable<StatementDTO> deleteStatement(StatementDTO statement, AccountTransactionManager accountTransactionManager) throws UpdateDeleteAccountException, InvalidStatementIdException, CannotDeleteLockedStatementException, CannotDeleteLastStatementException {
        Account account = getAccount(statement);

        // Get the statements currently available for this account.
        List<StatementDTO> statements = getStatements(account.getId(),null);

        // Perform the delete
        internalDeleteStatement(statement, statements, accountTransactionManager);
        return statements;
    }

    public List<Statement> getLatestStatementInternal(Account account) {
        return statementRepository.findByIdAccountAndLocked(account, false);
    }

    public Optional<Statement> getStatement(Account account, Integer month, Integer year) {
        // If there is no year or month then return null.
        if(month == null || year == null) {
            return Optional.empty();
        }

        StatementId id = new StatementId();
        id.setAccount(account);
        id.setMonth(month);
        id.setYear(year);

        return statementRepository.findById(id);
    }
}
