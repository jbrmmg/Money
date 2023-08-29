package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.control.TransactionController;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.*;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.DtoComplexModelMapper;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.reconciliation.MatchData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import java.io.IOException;
import java.util.*;

import static com.jbr.middletier.money.dataaccess.TransactionSpecifications.accountIs;
import static com.jbr.middletier.money.dataaccess.TransactionSpecifications.notLocked;

@Controller
public class ReconciliationManager {
    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationManager.class);

    private final ReconciliationRepository reconciliationRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final StatementRepository statementRepository;
    private final TransactionController transactionController;
    private final DtoComplexModelMapper complexModelMapper;
    private Account lastAccount;

    public ReconciliationManager(ReconciliationRepository reconciliationRepository,
                                 CategoryRepository categoryRepository,
                                 AccountRepository accountRepository,
                                 TransactionRepository transactionRepository,
                                 StatementRepository statementRepository,
                                 TransactionController transactionController,
                                 DtoComplexModelMapper complexModelMapper) {
        this.reconciliationRepository = reconciliationRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.statementRepository = statementRepository;
        this.transactionController = transactionController;
        this.complexModelMapper = complexModelMapper;
        this.lastAccount = null;
    }

    public void clearRepositoryData() {
        reconciliationRepository.deleteAll();
    }

    public void loadFile(ReconciliationFileDTO fileResponse, ReconciliationFileManager reconciliationFileManager) throws IOException {
        clearRepositoryData();

        List<TransactionDTO> transactions = reconciliationFileManager.getFileTransactions(fileResponse);

        for(TransactionDTO next : transactions) {
            ReconciliationData newReconciliationData = complexModelMapper.map(next,ReconciliationData.class);

            reconciliationRepository.save(newReconciliationData);
        }
    }

    public void autoReconcileData() throws UpdateDeleteCategoryException, UpdateDeleteAccountException, MultipleUnlockedStatementException, InvalidTransactionIdException, InvalidTransactionException {
        // Get the match data an automatically perform the roll forward action (create or reconcile)
        List<MatchData> matchData = matchFromLastData();

        // Process the data.
        for (MatchData next : matchData ) {
            try {
                // Process the action.
                if (next.getForwardAction().equalsIgnoreCase(MatchData.ForwardActionType.CREATE.toString())) {
                    TransactionDTO newTransaction = new TransactionDTO();

                    newTransaction.setAccountId(next.getAccount().getId());
                    newTransaction.setCategoryId(next.getCategory().getId());

                    newTransaction.setDate(next.getDate());

                    newTransaction.setAmount(next.getAmount());
                    newTransaction.setDescription(next.getDescription());

                    // Create the transaction.
                    transactionController.addTransactionExt(Collections.singletonList(newTransaction));
                } else if (next.getForwardAction().equalsIgnoreCase(MatchData.ForwardActionType.RECONCILE.toString())) {
                    // Reconcile the transaction
                    reconcile(next.getTransaction().getId(),true);
                }
            } catch (UpdateDeleteCategoryException ex) {
                LOG.error("Invalid Category Id exception.");
                throw ex;
            } catch (UpdateDeleteAccountException ex) {
                LOG.error("Invalid Account Id exception.");
                throw ex;
            } catch (MultipleUnlockedStatementException ex) {
                LOG.error("Multiple Unlock Statement Exception.");
                throw ex;
            } catch (InvalidTransactionIdException ex) {
                LOG.error("Invalid Transaction Id Exception.");
                throw ex;
            } catch (InvalidTransactionException ex) {
                LOG.error("Invalid Transaction Exception.");
                throw ex;
            }
        }
    }

    private List<MatchData> matchFromLastData() {
        if (lastAccount == null)
            return new ArrayList<>();

        return matchData(lastAccount);
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

    private List<MatchData> matchData(Account account) {
        // Attempt to match the reconciliation with the data in the account specified.

        // Get all transactions that are 'unlocked' on the account.
        List<Transaction> transactions = transactionRepository.findAll(Specification.where(notLocked()).and(accountIs(account)), Sort.by(Sort.Direction.ASC, "date", "amount"));

        // Get all the reconciliation data.
        List<ReconciliationData> reconciliationData = reconciliationRepository.findAllByOrderByDateAsc();

        // Create the match data.
        List<MatchData> result = new ArrayList<>();

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

        Collections.sort(result);

        // Update the opening balance information.
        List<Statement> unlockedStatement = statementRepository.findByIdAccountAndLocked(account,false);
        if(unlockedStatement.size() != 1) {
            LOG.info("Number of statements was not 1.");
        } else {
            double rollingAmount = unlockedStatement.get(0).getOpenBalance().getValue();

            // Set the open balance data.
            for(MatchData nextMatchData : result) {
                if(!nextMatchData.getForwardAction().equalsIgnoreCase("UNRECONCILE")) {
                    nextMatchData.setBeforeAmount(rollingAmount);
                    rollingAmount += nextMatchData.getAmount();
                    nextMatchData.setAfterAmount(rollingAmount);
                }
            }
        }

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
                LOG.info("Category updated for - {}", reconciliationUpdate.getId());
                reconciliationData.get().setCategory(category.get());
                reconciliationRepository.save(reconciliationData.get());
            } else {
                LOG.info("Invalid category.");
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

    public void reconcile(int transactionId, boolean reconcile) throws InvalidTransactionIdException, MultipleUnlockedStatementException {
        LOG.info("Reconcile transaction.");

        // Get the transaction.
        Optional<Transaction> transaction = transactionRepository.findById(transactionId);

        if(transaction.isPresent()) {
            // Then set the reconciliation or remove the flag.
            if (reconcile) {
                // Find the statement associated with the transaction.
                List<Statement> statements = statementRepository.findByIdAccountAndLocked(transaction.get().getAccount(), false);

                if (statements.size() == 1) {
                    // Set the statement.
                    transaction.get().setStatement(statements.get(0));
                } else {
                    LOG.error("Reconcile transaction - ignored (statement count not 1).");
                    throw new MultipleUnlockedStatementException(transaction.get().getAccount());
                }
            } else {
                // Remove the statement
                transaction.get().clearStatement();
            }

            // Save the transaction.
            transactionRepository.save(transaction.get());
            return;
        }

        throw new InvalidTransactionIdException(transactionId);
    }

    public List<MatchData> matchImpl(String accountId) throws UpdateDeleteAccountException {

        Optional<Account> account = accountRepository.findById(accountId);

        if(account.isEmpty()) {
            throw new UpdateDeleteAccountException("Invalid account id." + accountId);
        }

        lastAccount = account.get();
        return matchData(account.get());
    }
}
