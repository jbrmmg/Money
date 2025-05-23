package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.primary.Account;
import com.jbr.middletier.money.data.primary.Statement;
import com.jbr.middletier.money.data.primary.StatementId;
import com.jbr.middletier.money.data.primary.repository.StatementRepository;
import com.jbr.middletier.money.dto.StatementDTO;
import com.jbr.middletier.money.dto.StatementIdDTO;
import com.jbr.middletier.money.dto.mapper.StatementMapper;
import com.jbr.middletier.money.events.StatementLockEvent;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.util.FinancialAmount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Controller
public class StatementManager {
    private static final Logger LOG = LoggerFactory.getLogger(StatementManager.class);

    private final StatementRepository statementRepository;
    private final AccountManager accountManager;
    private final StatementMapper statementMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public StatementManager(StatementRepository statementRepository,
                            AccountManager accountManager,
                            StatementMapper statementMapper,
                            ApplicationEventPublisher applicationEventPublisher) {
        this.statementRepository = statementRepository;
        this.accountManager = accountManager;
        this.statementMapper = statementMapper;
        this.applicationEventPublisher = applicationEventPublisher;
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

                // Update an existing statement and create a new one.
                statementRepository.save(statement.get());
                statementRepository.save(newStatement);
                applicationEventPublisher.publishEvent(new StatementLockEvent(this, statement.get()));
                LOG.info("Request statement lock - locked.");

                // Regenerate the age of statements.
                checkStatementAges();
            } else {
                throw new StatementAlreadyLockedException(statementId);
            }
        } else {
            throw new InvalidStatementIdException(statementId);
        }

        return getStatements(statementId.getAccount().getId(),null);
    }

    public void checkStatementAges() {
        Map<String,List<Statement>> statements = new HashMap<>();

        // Check the statement ages.
        for(Statement nextStatement : statementRepository.findAll()){
            // If the account is closed, set the age to a high number, skip the age calculation.
            if(nextStatement.getId().getAccount().getClosed()) {
                if(nextStatement.getAge() != null) {
                    nextStatement.setAge(10000);
                    statementRepository.save(nextStatement);
                }
                continue;
            }

            // Add the statement to the map.
            List<Statement> accountStatements;
            if(statements.containsKey(nextStatement.getId().getAccount().getId())) {
                accountStatements = statements.get(nextStatement.getId().getAccount().getId());
            } else {
                accountStatements = new ArrayList<>();
                statements.put(nextStatement.getId().getAccount().getId(), accountStatements);
            }

            accountStatements.add(nextStatement);
        }

        // Process the list of statements for each account.
        for(String accountId : statements.keySet()){
            List<Statement> accountStatements = statements.get(accountId);

            // Sort the accounts in order of their data.
            accountStatements.sort((s1,s2) -> {
                if(s1.getId().getYear().equals(s2.getId().getYear())) {
                    return s2.getId().getMonth().compareTo(s1.getId().getMonth());
                }

                return Integer.compare(s2.getId().getYear(), s1.getId().getYear());
            });

            // Set the age of each statement.
            int age = 0;
            for(Statement statement : accountStatements){
                boolean update = false;

                // Does the statement need to be updated?
                if(!statement.getLocked()) {
                    if(statement.getAge() == null) {
                        statement.setAge(age);
                        update = true;
                    } else if (statement.getAge() != 0) {
                        statement.setAge(age);
                        update = true;
                    }
                } else {
                    age++;

                    if(statement.getAge() == null) {
                        statement.setAge(age);
                        update = true;
                    } else if(statement.getAge() != age) {
                        statement.setAge(age);
                        update = true;
                    }
                }

                if(update) {
                    statementRepository.save(statement);
                }
            }
        }
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
    public Iterable<StatementDTO> deleteStatement(StatementDTO statement, AccountTransactionManager accountTransactionManager) throws UpdateDeleteAccountException, InvalidStatementIdException, CannotDeleteLockedStatementException, CannotDeleteLastStatementException {
        Account account = getAccount(statement);

        // Get the statements currently available for this account.
        List<StatementDTO> statements = getStatements(account.getId(),null);

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
        penultimate.setLocked(false);
        statementRepository.save(statementMapper.map(penultimate,Statement.class));

        accountTransactionManager.removeTransactionsFromStatement(statementMapper.map(last,Statement.class));

        statementRepository.delete(statementMapper.map(last,Statement.class));

        applicationEventPublisher.publishEvent(new StatementLockEvent(this, account, penultimate));

        return statements;
    }

    public List<Statement> getLatestStatementInternal(Account account) {
        return statementRepository.findByIdAccountAndLocked(account, false);
    }

    public Optional<Statement> getStatement(Account account, Integer month, Integer year) {
        // If there is no year or month, then return null.
        if(month == null || year == null) {
            return Optional.empty();
        }

        StatementId id = new StatementId();
        id.setAccount(account);
        id.setMonth(month);
        id.setYear(year);

        return statementRepository.findById(id);
    }

    public Optional<StatementDTO> getStatementExternal(Account account, Integer month, Integer year) {
        Optional<Statement> statement = getStatement(account,month,year);

        return statement.map(value -> statementMapper.map(value, StatementDTO.class));
    }
}
