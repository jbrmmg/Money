package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.*;
import com.jbr.middletier.money.exceptions.InvalidCategoryIdException;
import com.jbr.middletier.money.exceptions.InvalidTransactionIdException;
import com.jbr.middletier.money.manage.WebLogManager;
import com.jbr.middletier.money.reporting.EmailGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.mail.*;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.jbr.middletier.money.dataaccess.TransactionSpecifications.*;
import static com.jbr.middletier.money.dataaccess.TransactionSpecifications.categoryIn;

/**
 * Created by jason on 08/03/17.
 */
@Controller
@RequestMapping("/jbr")
public class TransactionController {
    final static private Logger LOG = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionRepository transactionRepository;
    private final RegularRepository regularRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final WebLogManager webLogManager;
    private final EmailGenerator emailGenerator;

    @Autowired
    public TransactionController(TransactionRepository transactionRepository,
                                 RegularRepository regularRepository,
                                 AccountRepository accountRepository,
                                 CategoryRepository categoryRepository,
                                 WebLogManager webLogManager,
                                 EmailGenerator emailGenerator) {
        this.transactionRepository = transactionRepository;
        this.regularRepository = regularRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.webLogManager = webLogManager;
        this.emailGenerator = emailGenerator;
    }

    @ExceptionHandler(Exception.class)
    public void handleException(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value());
    }

    private Specification<Transaction> getReconciledTransactions(Iterable<Account> accounts, Date statmentDate, Iterable<Category> categories) {
        // Validate data.
        if((accounts == null)) {
            throw new IllegalStateException("Reconciled Transctions - must specify a single account");
        }

        if(statmentDate == null){
            throw new IllegalStateException("Reconciled Transctions - must specify a statement date");
        }

        // Reconciled transactions - for a particular month (statement), single account, list of categories.
        Specification<Transaction> search = Specification.where(statementDate(statmentDate)).and(accountIn(accounts));

        if(categories != null) {
            search = search.and(categoryIn(categories));
        }

        return search;
    }

    private Specification<Transaction> getUnreconciledTransactions(Iterable<Account> accounts, Iterable<Category> categories) {
        // Not locked transactions - no date, multiple accounts, list of categories
        Specification<Transaction> search = Specification.where(statementIsNull());

        if(accounts != null) {
            search = search.and(accountIn(accounts));
        }

        if(categories != null) {
            search = search.and(categoryIn(categories));
        }

        return search;
    }

    private Specification<Transaction> getAllTransactions(Date from, Date to, Iterable<Account> accounts, Iterable<Category> categories) {
        // Validate data.
        if(from == null){
            throw new IllegalStateException("All Transctions - must specify a from date");
        }
        if(to == null){
            throw new IllegalStateException("All Transctions - must specify a to date");
        }

        // All transactions - between two dates, multiple accounts, list of categories
        // Not locked transactions - no date, multiple accounts, list of categories
        Specification<Transaction> search = Specification.where(datesBetween(from,to));

        if(accounts != null) {
            search = search.and(accountIn(accounts));
        }

        if(categories != null) {
            search = search.and(categoryIn(categories));
        }

        return search;
    }

    private Specification<Transaction> getUnlockedTransactions(Iterable<Account> accounts, Iterable<Category> categories) {
        // Not locked transactions - no date, multiple accounts, list of categories
        Specification<Transaction> search = Specification.where(notLocked());

        if(accounts != null) {
            search = search.and(accountIn(accounts));
        }

        if(categories != null) {
            search = search.and(categoryIn(categories));
        }

        return search;
    }

    private Specification<Transaction> getTransactionSearch(String type,
                                                               String from,
                                                               String to,
                                                               String category,
                                                               String account) throws ParseException, IllegalStateException {
        // Get date values for the from and to values.
        Date fromDate = null;
        Date toDate = null;

        // Get the from date value if specified.
        SimpleDateFormat formatter = new SimpleDateFormat(Transaction.TransactionDateFormat);

        if (!from.equals("UNKN")) {
            fromDate = formatter.parse(from);
        }
        if (!to.equals("UNKN")) {
            toDate = formatter.parse(to);
        }

        // Check for unknown in category and account values.
        Iterable<Category> categories = null;
        if(!category.equalsIgnoreCase("UNKN")) {
            categories = categoryRepository.findAllById(Arrays.asList(category.split(",")));
        }
        Iterable<Account> accounts = null;
        if(!account.equalsIgnoreCase("UNKN")) {
            accounts = accountRepository.findAllById(Arrays.asList(account.split(",")));
        }

        // Process depending on type of transaction.
        //    UN - Unreconciled
        //    RC - Reconciled
        //    AL - All
        //    UL - Unlocked

        switch (type) {
            case "UN":
                LOG.info("Get Transaction - un reconciled");
                return getUnreconciledTransactions(accounts, categories);
            case "RC":
                LOG.info("Get Transaction - reconciled");
                return getReconciledTransactions(accounts, fromDate, categories);
            case "AL":
                LOG.info("Get Transaction - all");
                return getAllTransactions(fromDate, toDate, accounts, categories);
            case "UL":
                LOG.info("Get Transaction - unlocked");
                return getUnlockedTransactions(accounts, categories);
        }

        throw new IllegalStateException("Get Transactions invalid type value");
    }

    private void deleteTransaction(int transactionId) throws InvalidTransactionIdException {
        LOG.info("Delete transaction.");

        // Get the transaction.
        Optional<Transaction> transaction = transactionRepository.findById(transactionId);

        if(transaction.isPresent()) {
            // If the transaction is not reconciled then it can be deleted.
            if(!transaction.get().reconciled()) {
                transactionRepository.deleteById(transactionId);
                webLogManager.postWebLog(WebLogManager.webLogLevel.INFO,"Delete transaction.");
                return;
            }
        }

        throw new InvalidTransactionIdException(transactionId);
    }

    private List<Transaction> addTransaction(NewTransaction newTransaction) throws Exception {
        LOG.info("New Transaction.");

        List<Transaction> result = new ArrayList<>();

        // Get the account and category
        Optional<Account> account = accountRepository.findById(newTransaction.getAccountId());
        if(!account.isPresent()) {
            throw new Exception("Invalid account specified.");
        }

        Optional<Category> category = categoryRepository.findById(newTransaction.getCategoryId());
        if(!category.isPresent()) {
            throw new Exception("Invalid category specified.");
        }

        // Create transactions.
        Transaction transaction = new Transaction ( account.get(),
                                                    category.get(),
                                                    newTransaction.getDate(),
                                                    newTransaction.getAmount(),
                                                    newTransaction.getDescription() );
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Was this a transfer?
        if(newTransaction.isAccountTransfer()) {
            Optional<Account> transferAccount = accountRepository.findById(newTransaction.getTransferAccountId());
            if(!transferAccount.isPresent()) {
                throw new Exception("Invalid transfer account specified.");
            }

            transaction = new Transaction(  transferAccount.get(),
                                            category.get(),
                                            newTransaction.getDate(),
                                            newTransaction.getAmount() * -1,
                                            newTransaction.getDescription());
            transaction.setOppositeTransactionId(savedTransaction.getId());

            Transaction oppositeTransaction = transactionRepository.save(transaction);
            savedTransaction.setOppositeTransactionId(oppositeTransaction.getId());

            transactionRepository.save(savedTransaction);

            result.add(savedTransaction);
            result.add(oppositeTransaction);
        } else{
            result.add(savedTransaction);
        }

        webLogManager.postWebLog(WebLogManager.webLogLevel.INFO,"New transaction has been created.");
        return result;
    }

    private void updateTransacation(UpdateTransaction transactionRequest) throws InvalidTransactionIdException, InvalidCategoryIdException {
        LOG.info("Request transction update.");

        // Get the transaction.
        Optional<Transaction> transaction = transactionRepository.findById(transactionRequest.getId());

        if(transaction.isPresent()) {
            // If the transaction is locked then the amount cannot be updated.
            boolean locked = false;

            if(transaction.get().getStatement() != null) {
                if(transaction.get().getStatement().getLocked()) {
                    locked = true;
                    LOG.info("Update request - locked transaction.");
                }
            }

            if(!locked) {
                transaction.get().setAmount(transactionRequest.getAmount());
            }

            // If a category is specified, then update it (if not a transfer)
            if(transaction.get().getOppositeTransactionId() == null) {
                if (transactionRequest.getCategoryId().length() > 0) {
                    Optional<Category> category = categoryRepository.findById(transactionRequest.getCategoryId());
                    if(!category.isPresent()) {
                        throw new InvalidCategoryIdException(transactionRequest.getCategoryId());
                    }

                    transaction.get().setCategory(category.get());
                }
            }

            // If a description is specified, then update it.
            transaction.get().setDescription(transactionRequest.getDescription());

            transactionRepository.save(transaction.get());
            LOG.info("Request transction updated.");

            // Is there an opposite transaction?
            if (transaction.get().getOppositeTransactionId() != null) {
                Optional<Transaction> oppositeTransaction = transactionRepository.findById(transaction.get().getOppositeTransactionId());

                if (oppositeTransaction.isPresent()) {
                    oppositeTransaction.get().setDescription(transactionRequest.getDescription());

                    if (!locked) {
                        oppositeTransaction.get().setAmount(transaction.get().getAmount() * -1);
                    }

                    transactionRepository.save(oppositeTransaction.get());
                    LOG.info("Request transction updated (opposite).");
                }
            }


            return;
        }

        throw new InvalidTransactionIdException(transactionRequest.getId());
    }

    @RequestMapping(path="/ext/money/transaction/add", method= RequestMethod.POST)
    public @ResponseBody Iterable<Transaction>  addTransactionExt(@RequestBody NewTransaction newTransaction) throws Exception {
        return addTransaction(newTransaction);
    }


    @RequestMapping(path="/int/money/transaction/add", method= RequestMethod.POST)
    public @ResponseBody Iterable<Transaction>  addTransactionInt(@RequestBody NewTransaction newTransaction) throws Exception {
        return addTransaction(newTransaction);
    }

    @RequestMapping(path="/ext/money/delete", method= RequestMethod.DELETE)
    public @ResponseBody OkStatus deleteExternal(@RequestParam(value="transactionId", defaultValue="0") int transactionId) throws InvalidTransactionIdException {
        deleteTransaction(transactionId);
        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/int/money/delete", method= RequestMethod.DELETE)
    public @ResponseBody OkStatus deleteInternal( @RequestParam(value="transactionId", defaultValue="0") int transactionId) throws InvalidTransactionIdException {
        deleteTransaction(transactionId);
        return OkStatus.getOkStatus();
    }

    private Iterable<Transaction> getTransactionsImpl(String type, String from, String to, String category, String account, Boolean sortAscending) throws ParseException {

        Sort transactionSort = new Sort(Sort.Direction.ASC,"date", "account", "amount");

        if(sortAscending != null) {
            if(!sortAscending) {
                transactionSort = new Sort(Sort.Direction.DESC,"date", "account", "amount");
            }
        }

        return transactionRepository.findAll(getTransactionSearch(type,from,to,category,account), transactionSort);
    }

    @RequestMapping(path="/ext/money/transaction/get",method= RequestMethod.GET)
    public @ResponseBody
    Iterable<Transaction> getExtTransactionsExt(@RequestParam(value="type", defaultValue="UNKN") String type,
                                                   @RequestParam(value="from", defaultValue="UNKN") String from,
                                                   @RequestParam(value="to", defaultValue="UNKN") String to,
                                                   @RequestParam(value="category", defaultValue="UNKN")  String category,
                                                   @RequestParam(value="account", defaultValue="UNKN")  String account,
                                                   @RequestParam(value="sortAscending", defaultValue="true") Boolean sortAscending ) throws ParseException {
        return getTransactionsImpl(type,from,to,category,account,sortAscending);
    }

    @RequestMapping(path="/int/money/transaction/get",method= RequestMethod.GET)
    public @ResponseBody
    Iterable<Transaction> getExtTransactionsInt(@RequestParam(value="type", defaultValue="UNKN") String type,
                                                   @RequestParam(value="from", defaultValue="UNKN") String from,
                                                   @RequestParam(value="to", defaultValue="UNKN") String to,
                                                   @RequestParam(value="category", defaultValue="UNKN")  String category,
                                                   @RequestParam(value="account", defaultValue="UNKN") String account,
                                                   @RequestParam(value="sortAscending", defaultValue="true") Boolean sortAscending) throws ParseException {
        return getTransactionsImpl(type,from,to,category,account,sortAscending);
    }

    @RequestMapping(path="/ext/money/transaction/update", method= RequestMethod.PUT)
    public @ResponseBody OkStatus updateTransactionExt(@RequestBody UpdateTransaction transaction) throws InvalidTransactionIdException, InvalidCategoryIdException {
        updateTransacation(transaction);
        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/int/money/transaction/update", method= RequestMethod.PUT)
    public @ResponseBody OkStatus updateTransactionInt(@RequestBody UpdateTransaction transaction) throws InvalidTransactionIdException, InvalidCategoryIdException {
        updateTransacation(transaction);
        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/ext/money/transaction/regulars",method= RequestMethod.GET)
    public @ResponseBody
    Iterable<Regular> getRegularPaymentsExt() {
        LOG.info("Get the regular payments. (ext)");
        return regularRepository.findAll();
    }

    @RequestMapping(path="/int/money/transaction/regulars",method= RequestMethod.GET)
    public @ResponseBody
    Iterable<Regular> getRegularPaymentsInt() {
        LOG.info("Get the regular payments.(int)");
        return regularRepository.findAll();
    }

    @RequestMapping(path="/int/money/transaction/regulars",method= RequestMethod.POST)
    public @ResponseBody
    Iterable<Regular> getRegularPaymentsCreateInt(@RequestBody Regular regular) {
        LOG.info("Create a regular payment");
        regularRepository.save(regular);

        return regularRepository.findAll();
    }

    @RequestMapping(path="/int/money/transaction/regulars",method= RequestMethod.PUT)
    public @ResponseBody
    Iterable<Regular> getRegularPaymentsUpdateInt(@RequestBody Regular regular) {
        LOG.info("Update a regular payment");
        Optional<Regular> existingRegular = regularRepository.findById(regular.getId());

        if(existingRegular.isPresent()) {
            existingRegular.get().setDescription(regular.getDescription());
            existingRegular.get().setWeekendAdj(regular.getWeekendAdj());
            existingRegular.get().setFrequency(regular.getFrequency());
            existingRegular.get().setCategory(regular.getCategory());
            existingRegular.get().setAccount(regular.getAccount());
            existingRegular.get().setAmount(regular.getAmount());
            existingRegular.get().setStart(regular.getStart());

            regularRepository.save(existingRegular.get());
        } else {
            throw new IllegalStateException(String.format("Regular payment does not exist %d", regular.getId()));
        }

        return regularRepository.findAll();
    }

    @RequestMapping(path="/int/money/transaction/regulars",method= RequestMethod.DELETE)
    public @ResponseBody
    Iterable<Regular> getRegularPaymentsDeleteInt(@RequestBody Regular regular) {
        LOG.info("Delete a regular payment.");
        Optional<Regular> existingRegular = regularRepository.findById(regular.getId());

        if(existingRegular.isPresent()) {
            regularRepository.deleteById(existingRegular.get().getId());
        } else {
            throw new IllegalStateException(String.format("Regular payment does not exist %d", regular.getId()));
        }

        return regularRepository.findAll();
    }

    @RequestMapping(path="/int/money/transaction/email",method=RequestMethod.POST)
    public @ResponseBody OkStatus sendEmail(  @RequestParam(value="to", defaultValue="jason@jbrmmg.me.uk") String to,
                                                    @RequestParam(value="from", defaultValue="creditcards@jbrmmg.me.uk") String from,
                                                    @RequestParam(value="username", defaultValue="creditcards@jbrmmg.me.uk") String username,
                                                    @RequestParam(value="host", defaultValue="smtp.ionos.co.uk") String host,
                                                    @RequestParam(value="password") String password,
                                                    @RequestParam(value="weeks", defaultValue="6") int weeks ) throws Exception {

        LOG.info("sending email to " + to);

        try {
            this.emailGenerator.generateReport(to,from,username,host,password,weeks);
        } catch(MessagingException e) {
            LOG.error(e.getMessage());
            throw e;
        }

        return OkStatus.getOkStatus();
    }
}
