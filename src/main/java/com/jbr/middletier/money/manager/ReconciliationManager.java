package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.control.TransactionController;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.*;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.reconciliation.MatchData;
import com.jbr.middletier.money.reconciliation.MatchInformation;
import com.jbr.middletier.money.util.FinancialAmount;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private final ModelMapper modelMapper;
    private Account lastAccount;

    public ReconciliationManager(ReconciliationRepository reconciliationRepository,
                                 CategoryRepository categoryRepository,
                                 AccountRepository accountRepository,
                                 TransactionRepository transactionRepository,
                                 StatementRepository statementRepository,
                                 TransactionController transactionController,
                                 ModelMapper modelMapper) {
        this.reconciliationRepository = reconciliationRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.statementRepository = statementRepository;
        this.transactionController = transactionController;
        this.modelMapper = modelMapper;
        this.lastAccount = null;
    }

    public void clearRepositoryData() {
        reconciliationRepository.deleteAll();
    }

    public void loadFile(ReconciliationFileDTO fileResponse, ReconciliationFileManager reconciliationFileManager) throws IOException {
        clearRepositoryData();

        List<TransactionDTO> transactions = reconciliationFileManager.getFileTransactions(fileResponse);

        for(TransactionDTO next : transactions) {
            ReconciliationData newReconciliationData = modelMapper.map(next,ReconciliationData.class);

            reconciliationRepository.save(newReconciliationData);
        }
    }

    public void autoReconcileData() throws EmptyMatchDataException, InvalidCategoryIdException, InvalidAccountIdException, MultipleUnlockedStatementException, InvalidTransactionIdException, InvalidTransactionException {
        // Get the match data an automatically perform the roll forward action (create or reconcile)
        List<MatchData> matchData = matchFromLastData();

        //noinspection ConstantConditions
        if(matchData == null) {
            LOG.info("Null match data, doing nothing");
            throw new EmptyMatchDataException();
        }

        // Process the data.
        for (MatchData next : matchData ) {
            try {
                // Process the action.
                if (next.getForwardAction().equalsIgnoreCase(MatchData.ForwardActionType.CREATE.toString())) {
                    TransactionDTO newTransaction = new TransactionDTO();

                    newTransaction.setAccount(modelMapper.map(next.getAccount(), AccountDTO.class));
                    newTransaction.setCategory(modelMapper.map(next.getCategory(), CategoryDTO.class));

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Transaction.TRANSACTION_DATE_FORMAT);
                    newTransaction.setDate(LocalDate.parse(next.getDate(),formatter));

                    newTransaction.setAmount(next.getAmount());
                    newTransaction.setDescription(next.getDescription());

                    // Create the transaction.
                    transactionController.addTransactionExt(Collections.singletonList(newTransaction));
                } else if (next.getForwardAction().equalsIgnoreCase(MatchData.ForwardActionType.RECONCILE.toString())) {
                    // Reconcile the transaction
                    reconcile(next.getTransaction().getId(),true);
                }
            } catch (InvalidCategoryIdException ex) {
                LOG.error("Invalid Category Id exception.");
                throw ex;
            } catch (InvalidAccountIdException ex) {
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

    private void checkReconciliationData(ReconciliationData nextReconciliationData, Iterable<Transaction> transactions, Map<Integer, MatchInformation> trnMatches, MatchData matchData, List<ReconciliationData> repeats) {
        // Remember the best potential match.
        long bestDaysAway = -1;
        MatchInformation bestTrnMatch = null;

        // Do any existing transactions match? Or close match (amount)
        for(Transaction nextTransaction : transactions) {
            // Create match information.
            if(!trnMatches.containsKey(nextTransaction.getId())) {
                trnMatches.put(nextTransaction.getId(), new MatchInformation());
            }
            MatchInformation trnMatch = trnMatches.get(nextTransaction.getId());
            trnMatch.setTransaction(nextTransaction);

            // Is this transaction already exactly matched?
            if(trnMatch.exactMatch()) {
                continue;
            }

            // Is this an exact match?
            if(nextReconciliationData.toString().equals(nextTransaction.toString())) {
                trnMatch.setReconciliationData(nextReconciliationData);
                trnMatch.setDaysAway(0);
                matchData.matchTransaction(nextTransaction);
                return;
            }

            // Does the amount match?
            long difference = nextReconciliationData.closeMatch(nextTransaction);
            if((difference == -1) || (difference >= 4)) {
                continue;
            }

            // Set details of the match.
            if ((!trnMatch.closeMatch() || trnMatch.getDaysAway() > difference) && (( bestDaysAway == -1 )  || (difference < bestDaysAway))) {
                bestDaysAway = difference;
                bestTrnMatch = trnMatch;
            }
        }

        // If there was a good match, the use it.
        if(bestTrnMatch != null) {
            // If the transaction was already matched, then need to repeat the search.
            if (bestTrnMatch.getReconciliationData() != null) {
                repeats.add(bestTrnMatch.getReconciliationData());
            }

            bestTrnMatch.setDaysAway(bestDaysAway);
            bestTrnMatch.setReconciliationData(nextReconciliationData);
        }
    }

    private List<MatchData> matchFromLastData() {
        if (lastAccount == null)
            return new ArrayList<>();

        return matchData(lastAccount);
    }

    private void logTransactionData(String type, int id, LocalDate date, Category category, FinancialAmount amount) {
        String logData = type + " - " +
                id + " " +
                date + " " +
                (category == null ? "" : category.getName()) + " " +
                amount.toString();

        LOG.debug(logData);
    }

    private List<MatchData> matchData(Account account) {
        // Attempt to match the reconciliation with the data in the account specified.

        // Get all transactions that are 'unlocked' on the account.
        Iterable<Transaction> transactions = transactionRepository.findAll(Specification.where(notLocked()).and(accountIs(account)), Sort.by(Sort.Direction.ASC, "date", "amount"));

        // Get all the reconciliation data.
        List<ReconciliationData> reconciliationData = reconciliationRepository.findAllByOrderByDateAsc();

        // Log the data.
        if(LOG.isDebugEnabled()) {
            for (ReconciliationData nextReconciliationData : reconciliationData) {
                logTransactionData("Reconcile Data",
                        nextReconciliationData.getId(),
                        nextReconciliationData.getDate(),
                        nextReconciliationData.getCategory(),
                        new FinancialAmount(nextReconciliationData.getAmount()) );
            }

            for (Transaction nextTransaction : transactions) {
                logTransactionData("Transaction",
                        nextTransaction.getId(),
                        nextTransaction.getDate(),
                        nextTransaction.getCategory(),
                        nextTransaction.getAmount() );
            }
        }

        // Create a map for transactions.
        Map<Integer, MatchInformation> trnMatches = new HashMap<>();

        // Create a match data map.
        Map<Integer,Integer> matchDataMap = new HashMap<>();

        // Create the match data.
        List<MatchData> result = new ArrayList<>();
        List<ReconciliationData> repeats = new ArrayList<>();

        LOG.info("Matching data");
        for(ReconciliationData nextReconciliationData : reconciliationData) {
            // Create a match data for this item.
            MatchData newMatchData = new MatchData(nextReconciliationData, account);
            matchDataMap.put(nextReconciliationData.getId(),result.size());
            result.add(newMatchData);

            // Check this instance.
            checkReconciliationData(nextReconciliationData, transactions, trnMatches, newMatchData, repeats);
        }

        // Repeat if necessary.
        while(!repeats.isEmpty()) {
            List<ReconciliationData> prevRepeats = repeats;
            repeats = new ArrayList<>();

            for (ReconciliationData nextReconciliationData : prevRepeats) {
                checkReconciliationData(nextReconciliationData, transactions, trnMatches, result.get(matchDataMap.get(nextReconciliationData.getId())), repeats);
            }
        }

        // Update the result with any partial matches.
        LOG.info("Matching partial data");
        for(MatchInformation nextMatchInfo : trnMatches.values()) {
            if(nextMatchInfo.closeMatch()) {
                // Get the match data index.
                int matchDataIndex = matchDataMap.get(nextMatchInfo.getReconciliationData().getId());

                // Get the Match Data.
                MatchData matchData = result.get(matchDataIndex);

                // Set the close match transaction.
                matchData.matchTransaction(nextMatchInfo.getTransaction());
            }
        }

        // Add a list of any reconciled transaction that is not matched.
        LOG.info("Add reconciled transactions not matched");
        for (Transaction nextTransaction : transactions ) {
            if(nextTransaction.getStatement() != null)  {
                if(trnMatches.containsKey(nextTransaction.getId())){
                    MatchInformation matchInformation = trnMatches.get(nextTransaction.getId());
                    if(matchInformation.getReconciliationData() == null) {
                        result.add(new MatchData(nextTransaction));
                    }
                } else {
                    result.add(new MatchData(nextTransaction));
                }
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

    public List<MatchData> matchImpl(String accountId) throws InvalidAccountIdException {

        Optional<Account> account = accountRepository.findById(accountId);

        if(account.isEmpty()) {
            throw new InvalidAccountIdException("Invalid account id." + accountId);
        }

        lastAccount = account.get();
        return matchData(account.get());
    }
}
