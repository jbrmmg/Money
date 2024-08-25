package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.primary.*;
import com.jbr.middletier.money.data.primary.repository.*;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.ReconciliationMapper;
import com.jbr.middletier.money.dto.mapper.TransactionMapper;
import com.jbr.middletier.money.events.ReconcileTransactionEvent;
import com.jbr.middletier.money.events.ReconciliationFileLoadEvent;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.dto.MatchDataDTO;
import com.jbr.middletier.money.reconciliation.MatchData;
import org.apache.commons.collections4.IteratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static com.jbr.middletier.money.data.primary.repository.TransactionSpecifications.accountIs;
import static com.jbr.middletier.money.data.primary.repository.TransactionSpecifications.notLocked;

@Controller
public class ReconciliationManager {
    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationManager.class);

    private final ReconciliationRepository reconciliationRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final StatementRepository statementRepository;
    private final TransactionMapper transactionMapper;
    private final ReconciliationMapper reconciliationMapper;
    private final ReconciliationFileTransactionRepository reconciliationFileTransactionRepository;
    private final ReconciliationFileRepository reconciliationFileRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public ReconciliationManager(ReconciliationRepository reconciliationRepository,
                                 CategoryRepository categoryRepository,
                                 TransactionRepository transactionRepository,
                                 StatementRepository statementRepository,
                                 TransactionMapper transactionMapper,
                                 ReconciliationMapper reconciliationMapper,
                                 ReconciliationFileTransactionRepository reconciliationFileTransactionRepository,
                                 ReconciliationFileRepository reconciliationFileRepository,
                                 ApplicationEventPublisher applicationEventPublisher) {
        this.reconciliationRepository = reconciliationRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.statementRepository = statementRepository;
        this.transactionMapper = transactionMapper;
        this.reconciliationMapper = reconciliationMapper;
        this.reconciliationFileTransactionRepository = reconciliationFileTransactionRepository;
        this.reconciliationFileRepository = reconciliationFileRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void clearRepositoryData() {
        reconciliationRepository.deleteAll();

        // Mark all files as not loaded.
        for(ReconciliationFile nextFile : reconciliationFileRepository.findAll()) {
            nextFile.setLoaded(false);
            reconciliationFileRepository.save(nextFile);
        }
    }

    public void loadFile(ReconciliationFileLoadDTO fileLoad) throws IOException {
        clearRepositoryData();

        // Get the reconciliation file.
        Optional<ReconciliationFile> file = reconciliationFileRepository.findById(fileLoad.getFilename());

        if(file.isPresent()) {
            // Copy the transactions (set the account on each)
            for(ReconciliationFileTransaction next : reconciliationFileTransactionRepository.findById_File(file.get())) {
                ReconciliationData newReconciliationData = transactionMapper.map(next,ReconciliationData.class);
                newReconciliationData.setAccountId(file.get().getAccount().getId());

                reconciliationRepository.save(newReconciliationData);
            }

            // Mark the file as loaded.
            file.get().setLoaded(true);
            reconciliationFileRepository.save(file.get());

            // Trigger the event.
            applicationEventPublisher.publishEvent(new ReconciliationFileLoadEvent(this));

            return;
        }

        LOG.warn("{} not found, nothing loaded", fileLoad.getFilename());
        throw new FileNotFoundException("Cannot find " + fileLoad.getFilename());
    }

    public void updateAccount(ReconciliationFileUpdateAccountDTO updateAccount) throws IOException {
        // Get the reconciliation file.
        Optional<ReconciliationFile> file = reconciliationFileRepository.findById(updateAccount.getFilename());

        if(file.isPresent()) {
            // Get the account.
            Account newAccount = transactionMapper.map(updateAccount.getAccountId(), Account.class);

            // Update the account on this file.
            file.get().setAccount(newAccount);
            reconciliationFileRepository.save(file.get());

            // If this is the loaded file, then update the transactions too
            if(Boolean.TRUE.equals(file.get().getLoaded())) {
                for(ReconciliationData transaction :reconciliationRepository.findAll()) {
                    transaction.setAccountId(file.get().getAccount().getId());
                }
            }

            return;
        }

        LOG.warn("{} not found, cannot update account", updateAccount.getFilename());
        throw new FileNotFoundException("Cannot find " + updateAccount.getFilename());
    }

    private void innerLookForMatches(boolean reconciled, int daysAway, List<MatchData> result, List<Transaction> transactions) {
        // For each result, look for a transaction that matches based on the reconciled status and days away.
        for(MatchData next : result) {
            // If this is already reconciled to a transaction skip it.
            if(next.getTransaction() != null) {
                continue;
            }

            boolean found = false;
            int index = 0;
            while(!found && index < transactions.size()) {
                Transaction nextTransaction = transactions.get(index);
                index++;

                // Skip transactions that do not match the criteria.
                if(nextTransaction.reconciled() != reconciled) {
                    continue;
                }

                // Check if this reconciles record matches the transaction
                if(next.transactionMatch(nextTransaction,daysAway)) {
                    next.matchTransaction(nextTransaction);

                    // remove from the list.
                    transactions.remove(nextTransaction);
                    found = true;
                }
            }
        }
    }

    private void lookForMatches(boolean reconciled, List<MatchData> result, List<Transaction> transactions) {
        for(int i = 0; i < 3; i ++) {
            innerLookForMatches(reconciled,i,result,transactions);
        }
    }

    private Account getSelectedAccount() throws NullOrBlankAccountIdException {
        for(ReconciliationFile nextFile : reconciliationFileRepository.findAll()) {
            if(Boolean.TRUE.equals(nextFile.getLoaded())) {
                if(nextFile.getAccount() == null) {
                    throw new NullOrBlankAccountIdException();
                }

                return nextFile.getAccount();
            }
        }

        return null;
    }

    private void updateOpenBalance(Account account, List<MatchData> matchData) {
        // Update the opening balance information.
        List<Statement> unlockedStatement = statementRepository.findByIdAccountAndLocked(account,false);
        if(unlockedStatement.size() != 1) {
            LOG.info("Number of statements was not 1.");
        } else {
            BigDecimal rollingAmount = unlockedStatement.get(0).getOpenBalance().getValue();

            // Set the open balance data.
            for(MatchData nextMatchData : matchData) {
                if(!nextMatchData.getForwardAction().equalsIgnoreCase("UNRECONCILE")) {
                    nextMatchData.setBeforeAmount(rollingAmount);
                    rollingAmount = rollingAmount.add(nextMatchData.getAmount());
                    nextMatchData.setAfterAmount(rollingAmount);
                }
            }
        }
    }

    public List<MatchData> match() throws NullOrBlankAccountIdException {
        // Get the account of the data that is in the reconcile data.
        Account account = getSelectedAccount();

        // If the account is null, then no file is loaded - just return empty.
        List<MatchData> result = new ArrayList<>();
        if(account == null) {
            return result;
        }

        // Get all transactions that are 'unlocked' on the account.
        List<Transaction> transactions = transactionRepository.findAll(Specification.where(notLocked()).and(accountIs(account)), Sort.by(Sort.Direction.ASC, "date", "amount"));

        // Get all the reconciliation data.
        List<ReconciliationData> reconciliationData = reconciliationRepository.findAllByOrderByDateAsc();

        // Create a result for each reconciliation data.
        for(ReconciliationData next : reconciliationData) {
            result.add(new MatchData(next, account));
        }

        // Build up those reconciliations that match (first with reconciled then with un-reconciled).
        lookForMatches(true, result, transactions);
        lookForMatches(false, result, transactions);

        // For any transactions that are reconciled remaining, create a match entry.
        for(Transaction next : transactions) {
            if(next.reconciled()) {
                result.add(new MatchData(next));
            }
        }

        // Sort and update balance information
        Collections.sort(result);
        updateOpenBalance(account,result);

        return result;
    }

    private void transactionCategoryUpdate(ReconcileUpdateDTO reconciliationUpdate) {
        // Get the transaction.
        Optional<Transaction> transaction = transactionRepository.findById(reconciliationUpdate.getId());

        if(transaction.isPresent()) {
            // Is the category valid?
            Optional<Category> category =  categoryRepository.findById(reconciliationUpdate.getCategoryId());

            if(category.isPresent()) {
                if(transaction.get().getOppositeTransactionId() == null) {
                    LOG.info("Category updated for - {}", reconciliationUpdate.getId());
                    transaction.get().setCategory(category.get());
                    transactionRepository.save(transaction.get());
                }
            } else {
                LOG.info("Invalid category.");
            }

        } else {
            LOG.info("Invalid id (transaction).");
        }
    }

    private void reconciliationCategoryUpdate(ReconcileUpdateDTO reconciliationUpdate) {
        // Get the reconciliation data.
        Optional<ReconciliationData> reconciliationData = reconciliationRepository.findById(reconciliationUpdate.getId());

        if(reconciliationData.isPresent()) {
            // Is the category valid?
            Optional<Category> category =  categoryRepository.findById(reconciliationUpdate.getCategoryId());

            if(category.isPresent()) {
                LOG.info("Category updated for - {} (reconciliation)", reconciliationUpdate.getId());
                reconciliationData.get().setCategory(category.get());
                reconciliationRepository.save(reconciliationData.get());
            } else {
                LOG.info("Invalid category (reconciliation).");
            }
        } else {
            LOG.info("Invalid id.");
        }
    }

    public void processReconcileUpdate(ReconcileUpdateDTO reconciliationUpdate) {
        LOG.info("Update category (ext) - {} - {} - {}", reconciliationUpdate.getId(), reconciliationUpdate.getCategoryId(), reconciliationUpdate.getType());

        if(reconciliationUpdate.getType().equalsIgnoreCase("trn")) {
            transactionCategoryUpdate(reconciliationUpdate);
            return;
        }

        reconciliationCategoryUpdate(reconciliationUpdate);
    }

    public void reconcile(ReconcileTransactionDTO reconcileTransactions) throws InvalidTransactionIdException, MultipleUnlockedStatementException {
        LOG.info("Reconcile transactions.");

        // Get the transaction.
        Iterable<Transaction> transactions = transactionRepository.findAllById(reconcileTransactions.getTransactions());
        boolean foundAny = false;

        for(Transaction transaction : transactions) {
            foundAny = true;

            // Then set the reconciliation or remove the flag.
            if (reconcileTransactions.getReconcile()) {
                // Find the statement associated with the transaction.
                List<Statement> statements = statementRepository.findByIdAccountAndLocked(transaction.getAccount(), false);

                if (statements.size() == 1) {
                    // Set the statement.
                    transaction.setStatement(statements.get(0));
                } else {
                    LOG.error("Reconcile transaction - ignored (statement count not 1).");
                    throw new MultipleUnlockedStatementException(transaction.getAccount());
                }
            } else {
                // Remove the statement
                if(!transaction.getStatement().getLocked()) {
                    transaction.clearStatement();
                }
            }
        }

        // Save the transaction.
        if(foundAny) {
            transactionRepository.saveAll(transactions);
            this.applicationEventPublisher.publishEvent(new ReconcileTransactionEvent(this, IteratorUtils.toList(transactions.iterator())));
        } else if(!reconcileTransactions.getTransactions().isEmpty()) {
            throw new InvalidTransactionIdException(reconcileTransactions.getTransactions().get(0));
        }
    }

    public List<MatchDataDTO> matchImpl() throws NullOrBlankAccountIdException {
        List<MatchDataDTO> result = new ArrayList<>();
        for(MatchData next : match()) {
            result.add(reconciliationMapper.map(next,MatchDataDTO.class));
        }

        return result;
    }
}
