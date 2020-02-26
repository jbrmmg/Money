package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.jbr.middletier.money.dataaccess.AllTransactionSpecifications.*;
import static com.jbr.middletier.money.dataaccess.AllTransactionSpecifications.categoryIn;

/**
 * Created by jason on 08/03/17.
 */
@Controller
@RequestMapping("/jbr")
public class TransactionController {
    final static private Logger LOG = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionRepository transactionRepository;
    private final AllTransactionRepository allTransactionRepository;
    private final RegularRepository regularRepository;
    private final StatementRepository statementRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final ResourceLoader resourceLoader;

    @Autowired
    public TransactionController(TransactionRepository transactionRepository,
                                 AllTransactionRepository allTransactionRepository,
                                 RegularRepository regularRepository,
                                 StatementRepository statementRepository,
                                 AccountRepository accountRepository,
                                 CategoryRepository categoryRepository,
                                 ResourceLoader resourceLoader) {
        this.transactionRepository = transactionRepository;
        this.allTransactionRepository = allTransactionRepository;
        this.regularRepository = regularRepository;
        this.statementRepository = statementRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.resourceLoader = resourceLoader;
    }

    @ExceptionHandler(IllegalStateException.class)
    public void handleIllegalArgumentException(IllegalStateException e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value());
    }

    private Specification<AllTransaction> getReconciledTransactions(String[] accounts, Date statmentDate, String[] categories) {
        // Validate data.
        if((accounts == null) || (accounts.length != 1)) {
            throw new IllegalStateException("Reconciled Transctions - must specify a single account");
        }

        if(statmentDate == null){
            throw new IllegalStateException("Reconciled Transctions - must specify a statement date");
        }

        // Reconciled transactions - for a particular month (statement), single account, list of categories.
        Specification<AllTransaction> search = Specification.where(statement(statmentDate)).and(accountIn(accounts));

        if(categories != null) {
            search = search.and(categoryIn(categories));
        }

        return search;
    }

    private Specification<AllTransaction> getUnreconciledTransactions(String[] accounts, String[] categories) {
        // Not locked transactions - no date, multiple accounts, list of categories
        Specification<AllTransaction> search = Specification.where(statementIsNull());

        if(accounts != null) {
            search = search.and(accountIn(accounts));
        }

        if(categories != null) {
            search = search.and(categoryIn(categories));
        }

        return search;
    }

    private Specification<AllTransaction> getAllTransactions(Date from, Date to, String[] accounts, String[] categories) {
        // Validate data.
        if(from == null){
            throw new IllegalStateException("All Transctions - must specify a from date");
        }
        if(to == null){
            throw new IllegalStateException("All Transctions - must specify a to date");
        }

        // All transactions - between two dates, multiple accounts, list of categories
        // Not locked transactions - no date, multiple accounts, list of categories
        Specification<AllTransaction> search = Specification.where(datesBetween(from,to));

        if(accounts != null) {
            search = search.and(accountIn(accounts));
        }

        if(categories != null) {
            search = search.and(categoryIn(categories));
        }

        return search;
    }

    private Specification<AllTransaction> getUnlockedTransactions(String[] accounts, String[] categories) {
        // Not locked transactions - no date, multiple accounts, list of categories
        Specification<AllTransaction> search = Specification.where(notLocked());

        if(accounts != null) {
            search = search.and(accountIn(accounts));
        }

        if(categories != null) {
            search = search.and(categoryIn(categories));
        }

        return search;
    }

    private Specification<AllTransaction> getTransactionSearch(String type,
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
        String[] categories = null;
        if(!category.equalsIgnoreCase("UNKN")) {
            categories = category.split(",");
        }
        String[] accounts = null;
        if(!account.equalsIgnoreCase("UNKN")) {
            accounts = account.split(",");
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

    private boolean deleteTransaction(int transactionId) {
        LOG.info("Delete transaction.");

        // Get the transaction.
        Optional<Transaction> transaction = transactionRepository.findById(transactionId);

        if(transaction.isPresent()) {
            // If the transaction is not reconciled then it can be deleted.
            if(!transaction.get().reconciled()) {
                transactionRepository.deleteById(transactionId);
                return true;
            }
        }

        return false;
    }

    private List<Transaction> addTransaction(NewTransaction newTransaction) throws ParseException {
        LOG.info("New Transaction.");

        List<Transaction> result = new ArrayList<>();

        // Create transactions.
        Transaction transaction = new Transaction(newTransaction);
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Was this a transfer?
        if(newTransaction.isAccountTransfer()) {
            transaction = new Transaction(newTransaction);
            transaction.setAmount(newTransaction.getAmount() * -1.0);
            transaction.setAccount(newTransaction.getTransferAccount());
            transaction.setCategory(newTransaction.getCategory());
            transaction.setOppositeId(savedTransaction.getId());
            transaction.setDescription(newTransaction.getDescription());

            Transaction oppositeTransaction = transactionRepository.save(transaction);
            savedTransaction.setOppositeId(oppositeTransaction.getId());

            result.add(savedTransaction);
            result.add(oppositeTransaction);

            transactionRepository.save(savedTransaction);
        } else{
            result.add(savedTransaction);
        }


        return result;
    }

    private void updateTransacation(UpdateTransaction transactionRequest) {
        LOG.info("Request transction update.");

        // Get the transaction.
        Optional<Transaction> transaction = transactionRepository.findById(transactionRequest.getId());

        if(transaction.isPresent()) {
            // If the transaction is locked then the amount cannot be updated.
            boolean locked = false;
            StatementId statementId = transaction.get().calculateStatementId();

            if(statementId != null) {
                Optional<Statement> statement = statementRepository.findById(statementId);

                if(statement.isPresent()) {
                    if(statement.get().getLocked()) {
                        locked = true;
                        LOG.info("Update request - locked transaction.");
                    }
                }
            }

            if(!locked) {
                transaction.get().setAmount(transactionRequest.getAmount());
            }

            // If a category is specified, then update it (if not a transfer)
            if(!transaction.get().hasOppositeId()) {
                if (transactionRequest.getCategory().length() > 0) {
                    transaction.get().setCategory(transactionRequest.getCategory());
                }
            }

            // If a description is specified, then update it.
            transaction.get().setDescription(transactionRequest.getDescription());

            transactionRepository.save(transaction.get());
            LOG.info("Request transction updated.");

            if(!locked) {
                if (transaction.get().hasOppositeId()) {
                    transaction = transactionRepository.findById(transaction.get().getOppositeId());

                    if (transaction != null) {
                        transaction.get().setAmount(-1 * transactionRequest.getAmount());
                        transactionRepository.save(transaction.get());
                        LOG.info("Request transction updated (opposite).");
                    }
                }
            }
            return;
        }

        throw new IllegalStateException(String.format("Transaction does not exist %d", transactionRequest.getId()));
    }

    @RequestMapping(path="/ext/money/transaction/add", method= RequestMethod.POST)
    public @ResponseBody Iterable<Transaction>  addTransactionExt(@RequestBody NewTransaction newTransaction) throws ParseException {
        return addTransaction(newTransaction);
    }


    @RequestMapping(path="/int/money/transaction/add", method= RequestMethod.POST)
    public @ResponseBody Iterable<Transaction>  addTransactionInt(@RequestBody NewTransaction newTransaction) throws ParseException {
        return addTransaction(newTransaction);
    }

    @RequestMapping(path="/ext/money/delete", method= RequestMethod.DELETE)
    public @ResponseBody
    StatusResponse deleteExternal(@RequestParam(value="transactionId", defaultValue="0") int transactionId) {
        if(deleteTransaction(transactionId)) {
            return new StatusResponse();
        }

        return new StatusResponse("Failed to delete transaction.");
    }

    @RequestMapping(path="/int/money/delete", method= RequestMethod.DELETE)
    public @ResponseBody StatusResponse deleteInternal( @RequestParam(value="transactionId", defaultValue="0") int transactionId) {
        if(deleteTransaction(transactionId)) {
            return new StatusResponse();
        }

        return new StatusResponse("Failed to delete transaction.");
    }

    @RequestMapping(path="/ext/money/transaction/get",method= RequestMethod.GET)
    public @ResponseBody
    Iterable<AllTransaction> getExtTransactionsExt(@RequestParam(value="type", defaultValue="UNKN") String type,
                                                   @RequestParam(value="from", defaultValue="UNKN") String from,
                                                   @RequestParam(value="to", defaultValue="UNKN") String to,
                                                   @RequestParam(value="category", defaultValue="UNKN")  String category,
                                                   @RequestParam(value="account", defaultValue="UNKN")  String account) throws ParseException {
        return allTransactionRepository.findAll(getTransactionSearch(type,from,to,category,account), new Sort(Sort.Direction.ASC,"date", "account", "amount"));
    }

    @RequestMapping(path="/int/money/transaction/get",method= RequestMethod.GET)
    public @ResponseBody
    Iterable<AllTransaction> getExtTransactionsInt(@RequestParam(value="type", defaultValue="UNKN") String type,
                                                   @RequestParam(value="from", defaultValue="UNKN") String from,
                                                   @RequestParam(value="to", defaultValue="UNKN") String to,
                                                   @RequestParam(value="category", defaultValue="UNKN")  String category,
                                                   @RequestParam(value="account", defaultValue="UNKN")  String account) throws ParseException {
        return allTransactionRepository.findAll(getTransactionSearch(type,from,to,category,account), new Sort(Sort.Direction.ASC,"date", "account", "amount"));
    }

    @RequestMapping(path="/ext/money/transaction/update", method= RequestMethod.PUT)
    public @ResponseBody
    StatusResponse updateTransactionExt(@RequestBody UpdateTransaction transaction) {
        updateTransacation(transaction);
        return new StatusResponse();
    }

    @RequestMapping(path="/int/money/transaction/update", method= RequestMethod.PUT)
    public @ResponseBody
    StatusResponse updateTransactionInt(@RequestBody UpdateTransaction transaction) {
        updateTransacation(transaction);
        return new StatusResponse();
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
    public @ResponseBody StatusResponse sendEmail(  @RequestParam(value="to",defaultValue="jason@jbrmmg.me.uk") String to,
                                                    @RequestParam(value="from",defaultValue="jason@jbrmmg.me.uk") String from,
                                                    @RequestParam(value="username",defaultValue="jason@jbrmmg.me.uk") String username,
                                                    @RequestParam(value="host",defaultValue="smtp.ionos.co.uk") String host,
                                                    @RequestParam(value="password") String password) throws Exception {

        LOG.info("sending email to " + to);

        Iterable<Category> categories = categoryRepository.findAll();

        class EmailTransaction {
            Date date;
            Double amount;
            String description;
            String category;
            String account;

            EmailTransaction(Transaction transaction, Iterable<Category> categories) {
                this.date = transaction.getDate();
                this.amount = transaction.getAmount();
                this.description = transaction.getDescription() == null ? "" : transaction.getDescription().replace("WWW.","");
                this.account = transaction.getAccount();

                for(Category nextCategory : categories) {
                    if(nextCategory.getId().equalsIgnoreCase(transaction.getCategory())) {
                        this.category = nextCategory.getName();
                        break;
                    }
                }
            }

            @Override
            public String toString() {
                return description;
            }
        }

        List<EmailTransaction> emailData = new ArrayList<>();

        // Get the data that we will contain in the email.
        double startAmount = 0.0;
        double endAmount = 0.0;
        double transactionTotal1 = 0;
        double transactionTotal2 = 0;

        // Get the latest statement that is locked for each account.
        Iterable<Account> accounts = accountRepository.findAll();

        for(Account nextAccount : accounts) {
            // Get the latest statement.
            List<Statement> latestStatements = statementRepository.findByAccountAndLocked(nextAccount.getId(),false);
            for(Statement nextStatement: latestStatements) {
                endAmount += nextStatement.getOpenBalance();
                startAmount += nextStatement.getOpenBalance();

                // Get the transactions for this.
                List<Transaction> transactions = transactionRepository.findByAccountAndStatement(nextAccount.getId(), nextStatement.getYearMonthId());
                for(Transaction nextTransaction : transactions) {
//                    LOG.info(">" + nextTransaction.getId());

                    endAmount += nextTransaction.getAmount();
                    transactionTotal1 += nextTransaction.getAmount();

                    emailData.add(new EmailTransaction(nextTransaction,categories));
                }

                transactions = transactionRepository.findByAccountAndStatement(nextAccount.getId(), nextStatement.getPreviousId());
                for(Transaction nextTransaction : transactions) {
  //                  LOG.info("<" + nextTransaction.getId());
                    transactionTotal2 += nextTransaction.getAmount();

                    emailData.add(new EmailTransaction(nextTransaction,categories));
                }
            }
        }

        emailData.sort(new Comparator<EmailTransaction>() {
            @Override
            public int compare(EmailTransaction emailTransaction, EmailTransaction t1) {
                if(emailTransaction.date.before(t1.date)) {
                    return +1;
                } else if (emailTransaction.date.after(t1.date)) {
                    return -1;
                }

                if(emailTransaction.amount > t1.amount) {
                    return +1;
                } else if(emailTransaction.amount < t1.amount) {
                    return -1;
                }

                return 0;
            }
        });

        for(EmailTransaction nextTransaction: emailData) {
            LOG.info(nextTransaction.toString());
        }

        startAmount -= transactionTotal1;

        LOG.info(String.format("%02.2f",startAmount));
        LOG.info(String.format("%02.2f",endAmount));
        LOG.info(String.format("%02.2f",transactionTotal1 + transactionTotal2));

        Properties properties = new Properties();
        properties.put("mail.smtp.auth","true");
        properties.put("mail.smtp.starttls.enable","true");
        properties.put("mail.smtp.host",host);
        properties.put("mail.smtp.port","25");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MMM");

        Session session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username,password);
                    }
                });

        try {
            Message message = new MimeMessage(session);

            message.setFrom(new InternetAddress(from));
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("Credit card bills");

            // Get the email template.
            Resource resource = resourceLoader.getResource("classpath:html/email.html");
            InputStream is = resource.getInputStream();

            if(is== null) {
                throw new Exception("Cannot load resource");
            }

            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader reader = new BufferedReader(isr);

            String template = reader.lines().collect(Collectors.joining(System.lineSeparator()));

            StringBuilder sb = new StringBuilder();

            sb.append("<tr>\n");
            sb.append("<td class=\"date\"></td>\n");
            sb.append("<td class=\"description\"></td>\n");
            sb.append("<td class=\"description\"></td>\n");
            sb.append("<td class=\"description-bal\">Current Balance</td>\n");
            sb.append("<td class=\"amount amount-data db\">" + String.format("%02.2f",endAmount) + "</td>\n");
            sb.append("</tr>\n");
            sb.append("<tr>\n");
            sb.append("<td class=\"date\"></td>\n");
            sb.append("<td class=\"description\"></td>\n");
            sb.append("<td class=\"description\"></td>\n");
            sb.append("<td class=\"description-bal\"></td>\n");
            sb.append("<td class=\"amount amount-data db\"></td>\n");
            sb.append("</tr>\n");
            for(EmailTransaction nextTransaction: emailData) {
                sb.append("<tr>\n");
                sb.append("<td class=\"date\">" + sdf.format(nextTransaction.date) + "</td>\n");
                sb.append("<td class=\"description\">" + nextTransaction.category + "</td>\n");
                sb.append("<td class=\"description\">" + nextTransaction.account + "</td>\n");
                sb.append("<td class=\"description\">" + nextTransaction.description + "</td>\n");
                if(nextTransaction.amount < 0) {
                    sb.append("<td class=\"amount amount-data db\">" + String.format("%02.2f", nextTransaction.amount) + "</td>\n");
                } else {
                    sb.append("<td class=\"amount amount-data\">" + String.format("%02.2f", nextTransaction.amount) + "</td>\n");
                }
                sb.append("</tr>\n");
                LOG.info(nextTransaction.toString());
            }

            message.setContent(template.replace("<!-- TABLEROWS -->", sb.toString()),"text/html");

            Transport.send(message);

            LOG.info("email sent.");
        } catch(MessagingException e) {
            LOG.error(e.getMessage());
            return new StatusResponse("failed email - " + e.getMessage());
        }

        return new StatusResponse();
    }
}
