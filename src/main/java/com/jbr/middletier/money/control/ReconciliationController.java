package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.*;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.dto.ReconciliationFileDTO;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.manager.ReconciliationFileManager;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.jbr.middletier.money.dataaccess.TransactionSpecifications.*;

/**
 * Created by jason on 07/03/17.
 */
@Controller
@RequestMapping("/jbr")
public class ReconciliationController {
    // TODO split out into reconciliation manager
    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationController.class);

    private final ReconciliationRepository reconciliationRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final StatementRepository statementRepository;
    private final TransactionController transactionController;
    private final ModelMapper modelMapper;
    private final ReconciliationFileManager reconciliationFileManager;
    private Account lastAccount = null;

    @Autowired
    public ReconciliationController(StatementRepository statementRepository,
                                    TransactionRepository transactionRepository,
                                    ReconciliationRepository reconciliationRepository,
                                    CategoryRepository categoryRepository,
                                    AccountRepository accountRepository,
                                    TransactionController transactionController,
                                    ModelMapper modelMapper,
                                    ReconciliationFileManager reconciliationFileManager) {
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.categoryRepository = categoryRepository;
        this.transactionController = transactionController;
        this.accountRepository = accountRepository;
        this.modelMapper = modelMapper;
        this.reconciliationFileManager = reconciliationFileManager;
    }

    private static class MatchInformation {
        ReconciliationData reconciliationData;
        public Transaction transaction;
        long daysAway;

        MatchInformation() {
            this.reconciliationData = null;
            this.transaction = null;
            this.daysAway = 0;
        }

        boolean exactMatch() {
            return this.reconciliationData != null && this.daysAway == 0;
        }

        boolean closeMatch() {
            return this.reconciliationData != null && this.daysAway != 0;
        }
    }

    private void clearRepositoryData() {
        reconciliationRepository.deleteAll();
    }

    private void autoReconcileData() throws EmptyMatchDataException, ParseException, InvalidCategoryIdException, InvalidAccountIdException, MultipleUnlockedStatementException, InvalidTransactionIdException, InvalidTransactionException {
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
                    ReconcileTransaction reconcileRequest = new ReconcileTransaction();
                    reconcileRequest.setId(next.getTransaction().getId());
                    reconcileRequest.setReconcile(true);
                    reconcileExt(reconcileRequest);
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

    private void checkReconciliationData(ReconciliationData nextReconciliationData, Iterable<Transaction> transactions, Map<Integer, ReconciliationController.MatchInformation> trnMatches, MatchData matchData, List<ReconciliationData> repeats) {
        // Remember the best potential match.
        long bestDaysAway = -1;
        ReconciliationController.MatchInformation bestTrnMatch = null;

        // Do any existing transactions match? Or close match (amount)
        for(Transaction nextTransaction : transactions) {
            // Create match information.
            if(!trnMatches.containsKey(nextTransaction.getId())) {
                trnMatches.put(nextTransaction.getId(), new MatchInformation());
            }
            ReconciliationController.MatchInformation trnMatch = trnMatches.get(nextTransaction.getId());
            trnMatch.transaction = nextTransaction;

            // Is this transaction already exactly matched?
            if(trnMatch.exactMatch()) {
                continue;
            }

            // Is this an exact match?
            //noinspection EqualsBetweenInconvertibleTypes
            if(nextReconciliationData.equals(nextTransaction)) {
                trnMatch.reconciliationData = nextReconciliationData;
                trnMatch.daysAway = 0;
                matchData.matchTransaction(nextTransaction);
                return;
            }

            // Does the amount match?
            long difference = nextReconciliationData.closeMatch(nextTransaction);
            if((difference == -1) || (difference >= 4)) {
                continue;
            }

            // Set details of the match.
            if ((!trnMatch.closeMatch() || trnMatch.daysAway > difference) && (( bestDaysAway == -1 )  || (difference < bestDaysAway))) {
                bestDaysAway = difference;
                bestTrnMatch = trnMatch;
            }
        }

        // If there was a good match, the use it.
        if(bestTrnMatch != null) {
            // If the transaction was already matched, then need to repeat the search.
            if (bestTrnMatch.reconciliationData != null) {
                repeats.add(bestTrnMatch.reconciliationData);
            }

            bestTrnMatch.daysAway = bestDaysAway;
            bestTrnMatch.reconciliationData = nextReconciliationData;
        }
    }

    private List<MatchData> matchFromLastData() {
        if (lastAccount == null)
            return new ArrayList<>();

        return matchData(lastAccount);
    }

    private void logTransactionData(String type, int id, LocalDate date, Category category, double amount) {
        //TODO use financial amount here
        DecimalFormat df = new DecimalFormat("#,##0.00");

        String logData = type + " - " +
                id + " " +
                date + " " +
                (category == null ? "" : category.getName()) + " " +
                df.format(amount);

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
                        nextReconciliationData.getAmount() );
            }

            for (Transaction nextTransaction : transactions) {
                logTransactionData("Transaction",
                        nextTransaction.getId(),
                        nextTransaction.getDate(),
                        nextTransaction.getCategory(),
                        nextTransaction.getAmount().getValue() );
            }
        }

        // Create a map for transactions.
        Map<Integer, ReconciliationController.MatchInformation> trnMatches = new HashMap<>();

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
        for(ReconciliationController.MatchInformation nextMatchInfo : trnMatches.values()) {
            if(nextMatchInfo.closeMatch()) {
                // Get the match data index.
                int matchDataIndex = matchDataMap.get(nextMatchInfo.reconciliationData.getId());

                // Get the Match Data.
                MatchData matchData = result.get(matchDataIndex);

                // Set the close match transaction.
                matchData.matchTransaction(nextMatchInfo.transaction);
            }
        }

        // Add a list of any reconciled transaction that is not matched.
        LOG.info("Add reconciled transactions not matched");
        for (Transaction nextTransaction : transactions ) {
            if(nextTransaction.getStatement() != null)  {
                if(trnMatches.containsKey(nextTransaction.getId())){
                    ReconciliationController.MatchInformation matchInformation = trnMatches.get(nextTransaction.getId());
                    if(matchInformation.reconciliationData == null) {
                        result.add(new MatchData(nextTransaction));
                    }
                } else {
                    result.add(new MatchData(nextTransaction));
                }
            }
        }

        //noinspection unchecked
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

    private void transactionCategoryUpdate(ReconcileUpdate reconciliationUpdate) {
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

    private void reconciliationCategoryUpdate(ReconcileUpdate reconciliationUpdate) {
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

    private void processReconcileUpdate(ReconcileUpdate reconciliationUpdate) {
        LOG.info("Update category (ext) - {} - {} - {}", reconciliationUpdate.getId(), reconciliationUpdate.getCategoryId(), reconciliationUpdate.getType());

        if(reconciliationUpdate.getType().equalsIgnoreCase("trn")) {
            transactionCategoryUpdate(reconciliationUpdate);
            return;
        }

        reconciliationCategoryUpdate(reconciliationUpdate);
    }

    private void reconcile (int transactionId, boolean reconcile) throws InvalidTransactionIdException, MultipleUnlockedStatementException {
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

    // File Processors.
    private interface IReconcileFileProcessor {
        boolean skipLine(String line);

        ReconciliationData getReconcileData(String[] columns);
    }

    private LocalDate getReconciliationDateDate(String elementDate, String format) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            return LocalDate.parse(elementDate,formatter);
        } catch (Exception ignored) {
            LOG.info("Problem converting date, ignored.",ignored);
        }

        return null;
    }

    private LocalDate getReconciliationDataDate(String elementDate) {
        String[] dateFormats = new String[] {"dd-MM-yyyy","dd-MMM-yyyy","yyyy-MM-dd","dd/MM/yyyy", "dd/MMM/yyyy"};

        for(String nextDateFormat : dateFormats) {
            LocalDate nextDate = getReconciliationDateDate(elementDate, nextDateFormat);

            if(nextDate != null) {
                return nextDate;
            }
        }

        return null;
    }

    private Double getReconciliationDataAmount(String elementAmount) {
        try {
            // Attempt to parse the value.
            return Double.parseDouble(elementAmount);
        } catch (Exception ignored) {
            LOG.info("Problem converting amount, ignored.",ignored);
        }

        return null;
    }

    private void addReconciliationDataRecord(String record) {
        try {
            // Process the elements (CSV)
            String[] elements = record.split("[\t,]");

            // Minimum of 2 (date and amount).
            if(elements.length < 2) {
                throw new IllegalStateException("Too few elements in record.");
            }

            // Process the elements.
            LocalDate transactionDate = null;
            Double transactionAmount = null;
            Category category = null;
            String description = "";

            for(String nextElement : elements) {
                // If we don't have a date, try to get one.
                if(transactionDate == null) {
                    transactionDate = getReconciliationDataDate(nextElement);

                    if(transactionDate != null) {
                        // Check, if the year is less than 100
                        if(transactionDate.getYear() < 100) {
                            transactionDate = transactionDate.plusYears(2000);
                        }

                        continue;
                    }
                }

                if(transactionAmount == null) {
                    transactionAmount = getReconciliationDataAmount(nextElement);

                    if(transactionAmount != null) {
                        continue;
                    }
                }

                // Is it a category id?
                if((category == null) && (nextElement.length() == 3)) {
                    Optional<Category> maybeCategory = categoryRepository.findById(nextElement);

                    if (maybeCategory.isPresent()) {
                        category = maybeCategory.get();
                        continue;
                    }
                }

                // Otherwise use it as the description.
                if(nextElement.length() > description.length()) {
                    if(nextElement.length() > 40) {
                        description = nextElement.substring(0, 39);
                    } else {
                        description = nextElement;
                    }
                }
            }

            // If we had a value date and amount.
            if((transactionDate != null) && (transactionAmount != null)) {
                LOG.info("Got a valid record - inserting.");
                ReconciliationData newReconciliationData = new ReconciliationData(transactionDate, transactionAmount, category, description);

                reconciliationRepository.save(newReconciliationData);
            }
        } catch (Exception ex) {
            LOG.info("Failed to process record - {}", record);
            LOG.info("Error - {}", ex.getMessage());
        }
    }

    private void addReconciliationData(String data) {
        // Each line is a record - split by CR/LF
        String[] records = data.split("\n");

        LOG.info("Records - {}", records.length);

        // Insert data into the table.
        for(String nextRecord : records) {
            addReconciliationDataRecord(nextRecord);
        }
    }

    private void loadFile(ReconciliationFileDTO fileResponse) throws IOException {
        List<TransactionDTO> transactions = reconciliationFileManager.getFileTransactions(fileResponse);

        for(TransactionDTO next : transactions) {
            ReconciliationData newReconciliationData = modelMapper.map(next,ReconciliationData.class);

            reconciliationRepository.save(newReconciliationData);
        }
    }

    @PutMapping(path="/ext/money/reconcile")
    public @ResponseBody OkStatus reconcileExt(@RequestBody ReconcileTransaction reconcileTransaction) throws InvalidTransactionIdException, MultipleUnlockedStatementException {
        reconcile(reconcileTransaction.getTransactionId(),reconcileTransaction.getReconcile());
        return OkStatus.getOkStatus();
    }

    @PutMapping(path="/int/money/reconcile")
    public @ResponseBody OkStatus reconcileInt(@RequestBody ReconcileTransaction reconcileTransaction) throws InvalidTransactionIdException, MultipleUnlockedStatementException {
        reconcile(reconcileTransaction.getTransactionId(),reconcileTransaction.getReconcile());
        return OkStatus.getOkStatus();
    }

    @PostMapping(path="/ext/money/reconciliation/add")
    public @ResponseBody
    OkStatus  reconcileDataExt( @RequestBody String reconciliationData) {
        LOG.info("Adding Reconciliation Data (ext) - {}", reconciliationData.length());
        addReconciliationData(reconciliationData);
        return OkStatus.getOkStatus();
    }

    @PostMapping(path="/int/money/reconciliation/add")
    public @ResponseBody
    OkStatus  reconcileDataInt( @RequestBody String reconciliationData) {
        LOG.info("Adding Reconciliation Data - {}", reconciliationData.length());
        addReconciliationData(reconciliationData);
        return OkStatus.getOkStatus();
    }

    @PostMapping(path="int/money/reconciliation/load")
    public @ResponseBody OkStatus reconcileDataLoadInt(@RequestBody ReconciliationFileDTO reconciliationFile) throws IOException, InvalidAccountIdException {
        LOG.info("Request to load file - {}", reconciliationFile.getFilename());
        loadFile(reconciliationFile);
        return OkStatus.getOkStatus();
    }

    @GetMapping(path="int/money/reconciliation/files")
    public @ResponseBody Iterable<ReconciliationFileDTO> getListOfFiles() {
        LOG.info("Request to get list of files");

        return reconciliationFileManager.getFiles();
    }

    @PutMapping(path="/ext/money/reconciliation/update")
    public @ResponseBody OkStatus reconcileCategoryExt(@RequestBody ReconcileUpdate reconciliationUpdate ) {
        processReconcileUpdate(reconciliationUpdate);
        return OkStatus.getOkStatus();
    }

    @PutMapping(path="/int/money/reconciliation/update")
    public @ResponseBody OkStatus reconcileCategoryInt(@RequestBody ReconcileUpdate reconciliationUpdate) {
        processReconcileUpdate(reconciliationUpdate);
        return OkStatus.getOkStatus();
    }

    private List<MatchData> matchImpl(String accountId) throws InvalidAccountIdException {

        Optional<Account> account = accountRepository.findById(accountId);

        if(!account.isPresent()) {
            throw new InvalidAccountIdException("Invalid account id." + accountId);
        }

        lastAccount = account.get();
        return matchData(account.get());
    }

    @GetMapping(path="/ext/money/match")
    public @ResponseBody
    List<MatchData> matchExt(@RequestParam(value="account", defaultValue="UNKN") String accountId) throws InvalidAccountIdException {
        LOG.info("External match data - reconciliation data with reconciled transactions");
        return matchImpl(accountId);
    }

    @GetMapping(path="/int/money/match")
    public @ResponseBody
    List<MatchData>  matchInt(@RequestParam(value="account", defaultValue="UNKN") String accountId) throws InvalidAccountIdException {
        LOG.info("Internal match data - reconciliation data with reconciled transactions");
        return matchImpl(accountId);
    }

    @PutMapping(path="/ext/money/reconciliation/auto")
    public @ResponseBody OkStatus reconcileDataExt() throws MultipleUnlockedStatementException, InvalidCategoryIdException, InvalidAccountIdException, InvalidTransactionIdException, EmptyMatchDataException, ParseException, InvalidTransactionException {
        LOG.info("Auto Reconciliation Data (ext) ");
        autoReconcileData();
        return OkStatus.getOkStatus();
    }

    @PutMapping(path="/int/money/reconciliation/auto")
    public @ResponseBody OkStatus reconcileDataInt() throws MultipleUnlockedStatementException, InvalidCategoryIdException, InvalidAccountIdException, InvalidTransactionIdException, EmptyMatchDataException, ParseException, InvalidTransactionException {
        LOG.info("Auto Reconciliation Data ");
        autoReconcileData();
        return OkStatus.getOkStatus();
    }

    @DeleteMapping(path="/ext/money/reconciliation/clear")
    public @ResponseBody OkStatus reconcileDataDeleteExt() {
        LOG.info("Clear Reconciliation Data (ext) ");
        clearRepositoryData();
        return OkStatus.getOkStatus();
    }

    @DeleteMapping(path="/int/money/reconciliation/clear")
    public @ResponseBody OkStatus reconcileDataDeleteInt() {
        LOG.info("Clear Reconciliation Data ");
        clearRepositoryData();
        return OkStatus.getOkStatus();
    }
}
