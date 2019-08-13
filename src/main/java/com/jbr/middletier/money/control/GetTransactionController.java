package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.AllTransaction;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.AllTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.jbr.middletier.money.dataaccess.AllTransactionSpecifications.*;

/**
 * Created by jason on 11/03/17.
 */

@SuppressWarnings("unchecked")
@Controller
@RequestMapping("/jbr")
public class GetTransactionController {
    final static private Logger LOG = LoggerFactory.getLogger(GetTransactionController.class);

    private final
    AllTransactionRepository allTransactionRepository;

    @Autowired
    public GetTransactionController(AllTransactionRepository allTransactionRepository) {
        this.allTransactionRepository = allTransactionRepository;
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
}
