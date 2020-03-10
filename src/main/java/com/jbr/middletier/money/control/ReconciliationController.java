package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.*;
import com.jbr.middletier.money.exceptions.EmptyMatchDataException;
import com.jbr.middletier.money.exceptions.InvalidTransactionIdException;
import com.jbr.middletier.money.exceptions.MultipleUnlockedStatementException;
import com.jbr.middletier.money.exceptions.ReconciliationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.jbr.middletier.money.dataaccess.TransactionSpecifications.*;

/**
 * Created by jason on 07/03/17.
 */
@Controller
@RequestMapping("/jbr")
public class ReconciliationController {
    final static private Logger LOG = LoggerFactory.getLogger(ReconciliationController.class);

    private final ReconciliationRepository reconciliationRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final StatementRepository statementRepository;
    private final TransactionController transactionController;
    private Account lastAccount = null;

    @Autowired
    public ReconciliationController(StatementRepository statementRepository,
                                    TransactionRepository transactionRepository,
                                    ReconciliationRepository reconciliationRepository,
                                    CategoryRepository categoryRepository,
                                    AccountRepository accountRepository,
                                    TransactionController transactionController) {
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.categoryRepository = categoryRepository;
        this.transactionController = transactionController;
        this.accountRepository = accountRepository;
    }

    @ExceptionHandler(Exception.class)
    public void handleException(Exception e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value());
    }

    private class MatchInformation {
        ReconciliationData recociliationData;
        public Transaction transaction;
        long daysAway;

        MatchInformation() {
            this.recociliationData = null;
            this.transaction = null;
            this.daysAway = 0;
        }

        boolean exactMatch() {
            return this.recociliationData != null && this.daysAway == 0;
        }

        boolean closeMatch() {
            return this.recociliationData != null && this.daysAway != 0;
        }
    }

    private void clearRepositoryData() {
        reconciliationRepository.deleteAll();
    }

    private void autoReconcileData() throws Exception {
        // Get the match data an automatically perform the roll forward action (create or reconcile)
        List<MatchData> matchData = matchFromLastData();

        if(matchData == null)
        {
            LOG.info("Null match data, doing nothing");
            throw new EmptyMatchDataException();
        }

        // Process the data.
        for (MatchData next : matchData ) {
            try {
                // Process the action.
                if (next.getForwardAction().equalsIgnoreCase(MatchData.ForwardActionType.CREATE.toString())) {
                    // Create the transaction.
                    transactionController.addTransactionExt(new NewTransaction(next));
                } else if (next.getForwardAction().equalsIgnoreCase(MatchData.ForwardActionType.RECONCILE.toString())) {
                    // Reconcile the transaction
                    ReconcileTransaction reconcileRequest = new ReconcileTransaction();
                    reconcileRequest.setId(next.getTransactionId());
                    reconcileRequest.setReconcile(true);
                    reconcileExt(reconcileRequest);
                }
            } catch (Exception ex)
            {
                LOG.error("Failed to process match data.",ex);
                throw ex;
            }
        }
    }

    private void checkReconciliationData(ReconciliationData nextReconciliationData, Iterable<Transaction> transactions, Map<Integer, ReconciliationController.MatchInformation> trnMatches, MatchData matchData, List<ReconciliationData> repeats) {
        // Remember best potential match.
        long bestDaysAway = -1;
        ReconciliationController.MatchInformation bestTrnMatch = null;

        // Do any existing transactions match? Or close match (amount)
        for(Transaction nextTransaction : transactions) {
            Integer transactionId = nextTransaction.getId();

            // Create match information.
            if(!trnMatches.containsKey(nextTransaction.getId())) {
                trnMatches.put(nextTransaction.getId(),new ReconciliationController.MatchInformation());
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
                trnMatch.recociliationData = nextReconciliationData;
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
            if(!trnMatch.closeMatch() || trnMatch.daysAway > difference) {
                if( ( bestDaysAway == -1 )  || (difference < bestDaysAway) ) {
                    bestDaysAway = difference;
                    bestTrnMatch = trnMatch;
                }
            }
        }

        // If there was a good match, the use it.
        if(bestTrnMatch != null) {
            // If the transaction was already matched, then need to repeat the search.
            if (bestTrnMatch.recociliationData != null) {
                repeats.add(bestTrnMatch.recociliationData);
            }

            bestTrnMatch.daysAway = bestDaysAway;
            bestTrnMatch.recociliationData = nextReconciliationData;
        }
    }

    private List<MatchData> matchFromLastData() throws Exception {
        if (lastAccount == null)
            return null;

        return matchData(lastAccount);
    }

    private List<MatchData> matchData(Account account) throws Exception {
        // Attempt to match the reconciliation with the data in the account specified.

        // Get all transactions that are 'unlocked' on the account.
        Iterable<Transaction> transactions = transactionRepository.findAll(Specification.where(notLocked()).and(accountIs(account)), new Sort(Sort.Direction.ASC,"date", "amount"));

        // Get all the reconciliation data.
        List<ReconciliationData> reconciliationData = reconciliationRepository.findAllByOrderByDateAsc();

        // Log the data.
        if(LOG.isDebugEnabled()) {
            for (ReconciliationData nextReconciliationData : reconciliationData) {
                StringBuilder logData = new StringBuilder();

                logData.append("Reconcile Data - ");

                logData.append(nextReconciliationData.getId());
                logData.append(" ");

                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
                logData.append(sdf.format(nextReconciliationData.getDate()));
                logData.append(" ");

                logData.append(nextReconciliationData.getCategory());
                logData.append(" ");

                DecimalFormat df = new DecimalFormat("#,##0.00");
                logData.append(df.format(nextReconciliationData.getAmount()));

                LOG.debug(logData.toString());
            }

            for (Transaction nextTransaction : transactions) {
                StringBuilder logData = new StringBuilder();

                logData.append("Transaction - ");

                logData.append(nextTransaction.getId());
                logData.append(" ");

                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
                logData.append(sdf.format(nextTransaction.getDate()));
                logData.append(" ");

                logData.append(nextTransaction.getCategory());
                logData.append(" ");

                DecimalFormat df = new DecimalFormat("#,##0.00");
                logData.append(df.format(nextTransaction.getAmount()));

                LOG.debug(logData.toString());
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
        while(repeats.size() > 0) {
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
                int matchDataIndex = matchDataMap.get(nextMatchInfo.recociliationData.getId());

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
                    if(matchInformation.recociliationData == null) {
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
            double rollingAmount = unlockedStatement.get(0).getOpenBalance();

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
                    LOG.info("Category updated for - " + Integer.toString(reconciliationUpdate.getId()));
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
                LOG.info("Category updated for - " + Integer.toString(reconciliationUpdate.getId()));
                reconciliationData.get().setCategory(category.get(), category.get().getColour());
                reconciliationRepository.save(reconciliationData.get());
            } else {
                LOG.info("Invalid category.");
            }
        } else {
            LOG.info("Invalid id.");
        }
    }

    private void processReconcileUpdate(ReconcileUpdate reconciliationUpdate) {
        LOG.info("Update category (ext) - " + Integer.toString(reconciliationUpdate.getId()) + " - " + reconciliationUpdate.getCategoryId() + " - " + reconciliationUpdate.getType());

        if(reconciliationUpdate.getType().equalsIgnoreCase("trn")) {
            transactionCategoryUpdate(reconciliationUpdate);
            return;
        }

        reconciliationCategoryUpdate(reconciliationUpdate);
        return;
    }

    private void reconcile (int transactionId, boolean reconcile) throws InvalidTransactionIdException, MultipleUnlockedStatementException, ReconciliationException {
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

        ReconciliationData getReconcileData(String[] columns) throws Exception;
    }

    private class AmexFileProcessor implements  IReconcileFileProcessor {
        public boolean skipLine(String line) {
            return false;
        }

        public ReconciliationData getReconcileData(String[] columns) throws Exception {
            if(columns.length < 4) {
                throw new Exception("Unexpected line");
            }

            // Column 1 = date.
            Date transactionDate = getRecocillationDateDate(columns[0],"dd/MM/yy");

            // Column 3 = amount * -1
            Double transactionAmount = Double.parseDouble(columns[2]);
            transactionAmount *= -1;

            // Column 4 = description.
            String description = columns[3].length() > 40 ? columns[3].substring(0,40) : columns[3];

            LOG.info("Got a valid record - inserting.");
            return new ReconciliationData(transactionDate, transactionAmount, null, description);
        }
    }

    private class JohnLewisFileProcessor implements  IReconcileFileProcessor {
        public boolean skipLine(String line) {
            return false;
        }

        public ReconciliationData getReconcileData(String[] columns) {
            if(columns.length < 3) {
                return null;
            }

            if(columns[2].equalsIgnoreCase("amount")) {
                return null;
            }

            // Column 1 = date.
            Date transactionDate = getRecocillationDateDate(columns[0],"dd-MMM-yyyy");

            if(transactionDate != null) {
                // Column 3 = amount * -1, remove £
                String amountString = columns[2];

                amountString = amountString.replace("£","");
                amountString = amountString.replace(" ","");

                double multiplier = -1;
                if(amountString.substring(0,1).equals("+"))
                {
                    multiplier = 1;
                }

                Double transactionAmount = Double.parseDouble(amountString.replace(",",""));
                transactionAmount *= multiplier;

                // Column 4 = description.
                String description = columns[1].length() > 40 ? columns[1].substring(0, 40) : columns[1];

                LOG.info("Got a valid record - inserting.");
                return new ReconciliationData(transactionDate, transactionAmount, null, description);
            }

            return null;
        }
    }

    private class FirstDirectFileProcessor implements  IReconcileFileProcessor {
        public boolean skipLine(String line) {
            return false;
        }

        public ReconciliationData getReconcileData(String[] columns) throws Exception {
            if(columns.length < 4) {
                return null;
            }

            if(columns[2].equalsIgnoreCase("amount")) {
                return null;
            }

            // Column 1 = date.
            Date transactionDate = getRecocillationDateDate(columns[0],"dd/MM/yyyy");

            // Column 3 = amount * -1
            Double transactionAmount = Double.parseDouble(columns[2]);

            // Column 4 = description.
            String description = columns[1].length() > 40 ? columns[1].substring(0,40) : columns[1];

            LOG.info("Got a valid record - inserting.");
            return new ReconciliationData(transactionDate, transactionAmount, null, description);
        }
    }

    private Date getRecocillationDateDate(String elementDate, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(elementDate));
            calendar.set(Calendar.HOUR_OF_DAY,12);
            calendar.set(Calendar.MINUTE,0);
            calendar.set(Calendar.SECOND,0);
            calendar.set(Calendar.MILLISECOND,0);

            return calendar.getTime();
        } catch (Exception ignored) {
        }

        return null;
    }

    private Date getReconcilationDataDate(String elementDate) {
        String[] dateFormats = new String[] {"dd-MM-yyyy","dd-MMM-yyyy","yyyy-MM-dd","dd/MM/yyyy", "dd/MMM/yyyy"};

        for(String nextDateFormat : dateFormats) {
            Date nextDate = getRecocillationDateDate(elementDate, nextDateFormat);

            if(nextDate != null) {
                return nextDate;
            }
        }

        return null;
    }

    private Double getReconcilationDataAmount(String elementAmount) {
        try {
            // Attempt to parse the value.
            return Double.parseDouble(elementAmount);
        } catch (Exception ignored) {
        }

        return null;
    }

    private void addReconcilationDataRecord(String record) {
        try {
            // Process the elements (CSV)
            String[] elements = record.split("\t|,");

            // Minimum of 2 (date and amount).
            if(elements.length < 2) {
                throw new Exception("Too few elements.");
            }

            // Process the elements.
            Date transactionDate = null;
            Double transactionAmount = null;
            Category category = null;
            String description = "";

            for(String nextElement : elements) {
                // If we don't have a date, try to get one.
                if(transactionDate == null) {
                    transactionDate = getReconcilationDataDate(nextElement);

                    if(transactionDate != null) {
                        // Check, if the year is less than 100
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(transactionDate);
                        int year = calendar.get(Calendar.YEAR);
                        if(year < 100) {
                            int day = calendar.get(Calendar.DAY_OF_MONTH);
                            int month = calendar.get(Calendar.MONTH);
                            calendar.set(year + 2000, month, day);
                            transactionDate = calendar.getTime();
                        }

                        continue;
                    }
                }

                if(transactionAmount == null) {
                    transactionAmount = getReconcilationDataAmount(nextElement);

                    if(transactionAmount != null) {
                        continue;
                    }
                }

                // Is it a category id?
                if(category == null) {
                    if (nextElement.length() == 3) {
                        Optional<Category> maybeCategory = categoryRepository.findById(nextElement);

                        if (maybeCategory.isPresent()) {
                            category = maybeCategory.get();
                            continue;
                        }
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
            LOG.info("Failed to process record - " + record);
            LOG.info("Error - " + ex.getMessage());
        }
    }

    private void addReconcilationData(String data) {
        // Each line is a record - split by CR/LF
        String[] records = data.split("\n");

        LOG.info("Records - " + records.length);

        // Insert data into the table.
        for(String nextRecord : records) {
            addReconcilationDataRecord(nextRecord);
        }
    }

    private String[] splitDataLine(String line) {
        String[] intermediate = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

        String[] result = new String[intermediate.length];

        for(int i = 0; i < intermediate.length; i++) {
            result[i] = intermediate[i].replace("\"","");
        }

        return result;
    }


    private void loadReconcileFile(File recFile, IReconcileFileProcessor lineProcessor) throws Exception {
        // Clear existing data.
        LOG.info("Clear the reconciliation data.");
        reconciliationRepository.deleteAll();

        // Load the AMEX file.
        LOG.info("About to process an AMEX file - " + recFile.getPath());

        // AMEX File is a CSV
        // Date (dd/mm/yy), Reference, Amount *-1, Description, additional
        BufferedReader reader = new BufferedReader(new FileReader(recFile.getPath()));
        String line;
        while((line = reader.readLine()) != null) {
            // Clean the line.
            while(line.contains("  ") || line.contains("\t")) {
                line = line.replace("  ", " ");
                line = line.replace("\t", " ");
            }

            line = line.trim();

            // Get the reconciliation data.
            if(!lineProcessor.skipLine(line)) {
                ReconciliationData recLine = lineProcessor.getReconcileData(splitDataLine(line));

                if(recLine != null) {
                    reconciliationRepository.save(recLine);
                }
            }
        }
        reader.close();
    }

    private void loadFile(LoadFileRequest loadFileRequest) throws Exception {
        // Load the file.
        File recFile = new File(loadFileRequest.getPath());

        if(!recFile.exists()) {
            throw new Exception("Cannot find file " + loadFileRequest.getPath());
        }

        // Process the file of the specified type.
        switch(loadFileRequest.getType()) {
            case "AMEX":
                loadReconcileFile(recFile, new AmexFileProcessor() );
                break;
            case "JOHNLEWIS":
                loadReconcileFile(recFile, new JohnLewisFileProcessor() );
                break;
            case "FIRSTDIRECT":
                loadReconcileFile(recFile, new FirstDirectFileProcessor() );
                break;

            default:
                throw new Exception("Unexpected file type " + loadFileRequest.getType());
        }
    }

    @RequestMapping(path="/ext/money/reconcile", method= RequestMethod.PUT)
    public @ResponseBody OkStatus reconcileExt(@RequestBody ReconcileTransaction reconcileTransaction) throws InvalidTransactionIdException, MultipleUnlockedStatementException, ReconciliationException {
        reconcile(reconcileTransaction.getTransactionId(),reconcileTransaction.getReconcile());
        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/int/money/reconcile", method= RequestMethod.PUT)
    public @ResponseBody OkStatus reconcileInt(@RequestBody ReconcileTransaction reconcileTransaction) throws InvalidTransactionIdException, MultipleUnlockedStatementException, ReconciliationException {
        reconcile(reconcileTransaction.getTransactionId(),reconcileTransaction.getReconcile());
        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/ext/money/reconciliation/add", method= RequestMethod.POST)
    public @ResponseBody
    OkStatus  reconcileDataExt( @RequestBody String reconciliationData) {
        LOG.info("Adding Reconcilation Data (ext) - " + reconciliationData.length());
        addReconcilationData(reconciliationData);
        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/int/money/reconciliation/add", method= RequestMethod.POST)
    public @ResponseBody
    OkStatus  reconcileDataInt( @RequestBody String reconciliationData) {
        LOG.info("Adding Reconcilation Data - " + reconciliationData.length());
        addReconcilationData(reconciliationData);
        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="int/money/reconciliation/load", method= RequestMethod.POST)
    public @ResponseBody
    OkStatus reconcileDataLoadInt(@RequestBody LoadFileRequest loadFileRequest) throws Exception {
        LOG.info("Request to load file - " + loadFileRequest.getPath() + " " + loadFileRequest.getType());
        loadFile(loadFileRequest);
        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="int/money/reconciliation/files", method= RequestMethod.GET)
    public @ResponseBody
    Iterable<FileResponse> getListOfFiles() {
        LOG.info("Request to get list of files");

        final File folder = new File("/home/jason/Downloads");

        List<FileResponse> result = new ArrayList<>();

        for(final File fileEntry : folder.listFiles()) {
            if(!fileEntry.isDirectory()) {
                if(fileEntry.getPath().endsWith(".csv")) {
                    result.add(new FileResponse(fileEntry.getPath()));
                }
            }
        }

        return result;
    }

    @RequestMapping(path="/ext/money/reconciliation/update", method= RequestMethod.PUT)
    public @ResponseBody OkStatus reconcileCategoryExt(@RequestBody ReconcileUpdate reconciliationUpdate ) {
        processReconcileUpdate(reconciliationUpdate);
        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/int/money/reconciliation/update", method= RequestMethod.PUT)
    public @ResponseBody OkStatus reconcileCategoryInt(@RequestBody ReconcileUpdate reconciliationUpdate) {
        processReconcileUpdate(reconciliationUpdate);
        return OkStatus.getOkStatus();
    }

    private List<MatchData> matchImpl(String accountId) throws Exception {

        Optional<Account> account = accountRepository.findById(accountId);

        if(!account.isPresent()) {
            throw new Exception("Invalid account id.");
        }

        lastAccount = account.get();
        return matchData(account.get());
    }

    @RequestMapping(path="/ext/money/match", method= RequestMethod.GET)
    public @ResponseBody
    List<MatchData> matchExt(@RequestParam(value="account", defaultValue="UNKN") String accountId) throws Exception {
        LOG.info("External match data - reconciliation data with reconciled transactions");
        return matchImpl(accountId);
    }

    @RequestMapping(path="/int/money/match", method= RequestMethod.GET)
    public @ResponseBody
    List<MatchData>  matchInt(@RequestParam(value="account", defaultValue="UNKN") String accountId) throws Exception {
        LOG.info("Internal match data - reconciliation data with reconciled transactions");
        return matchImpl(accountId);
    }

    @RequestMapping(path="/ext/money/reconciliation/auto", method= RequestMethod.PUT)
    public @ResponseBody OkStatus reconcileDataExt() throws Exception {
        LOG.info("Auto Reconcilation Data (ext) ");
        autoReconcileData();
        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/int/money/reconciliation/auto", method= RequestMethod.PUT)
    public @ResponseBody OkStatus reconcileDataInt() throws Exception {
        LOG.info("Auto Reconcilation Data ");
        autoReconcileData();
        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/ext/money/reconciliation/clear", method= RequestMethod.DELETE)
    public @ResponseBody OkStatus reconcileDataDeleteExt() {
        LOG.info("Clear Reconcilation Data (ext) ");
        clearRepositoryData();
        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/int/money/reconciliation/clear", method= RequestMethod.DELETE)
    public @ResponseBody OkStatus reconcileDataDeleteInt() {
        LOG.info("Clear Reconcilation Data ");
        clearRepositoryData();
        return OkStatus.getOkStatus();
    }
}
