package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dto.DateRangeDTO;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.manager.AccountTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Pattern;
import java.util.*;

/**
 * Created by jason on 08/03/17.
 */
@RestController
@RequestMapping("/jbr")
public class TransactionController {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionController.class);

    private final AccountTransactionManager accountTransactionManager;

    @Autowired
    public TransactionController(AccountTransactionManager accountTransactionManager) {
        this.accountTransactionManager = accountTransactionManager;
    }

    @GetMapping(path="/ext/money/transaction")
    public Iterable<TransactionDTO> getExtTransactionsExt(@RequestParam(value="type", required = false) String type,
                                                                        @RequestParam(value="from", required = false) String from,
                                                                        @RequestParam(value="to", required = false) String to,
                                                                        @RequestParam(value="category", required = false)  String category,
                                                                        @RequestParam(value="account", required = false)  String account,
                                                                        @RequestParam(value="sortAscending", required = false) Boolean sortAscending) throws InvalidTransactionSearchException {

        @Pattern(regexp = "^[a-zA-Z]{2}$", message = "Type must be a two letter code")
        String sanitizedType = type;
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "From must be a valid date format.")
        String sanitizedFrom = from;
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "To must be a valid date format.")
        String sanitizedTo = to;
        @Pattern(regexp = "^[a-zA-Z,]*$", message = "Category must be a comma separated list of ids")
        String sanitizedCategory = category;
        @Pattern(regexp = "^[a-zA-Z,]*$", message = "Account must be a comma separated list of ids")
        String sanitizedAccount = account;

        LOG.info("Get Transactions {} {} {} {} {} {}", sanitizedType, sanitizedFrom, sanitizedTo, sanitizedCategory, sanitizedAccount, sortAscending);
        return accountTransactionManager.getTransactions(TransactionRequestType.getTransactionType(sanitizedType),
                new DateRangeDTO(sanitizedFrom, sanitizedTo),
                category == null ? null : Arrays.asList(sanitizedCategory.split(",")),
                account == null ? null : Arrays.asList(sanitizedAccount.split(",")),
                Boolean.TRUE.equals(sortAscending));
    }

    @GetMapping(path="/int/money/transaction")
    public Iterable<TransactionDTO> getExtTransactionsInt(@RequestParam(value="type", required = false) String type,
                                                                        @RequestParam(value="from", required = false) String from,
                                                                        @RequestParam(value="to", required = false) String to,
                                                                        @RequestParam(value="category", required = false)  String category,
                                                                        @RequestParam(value="account", required = false) String account,
                                                                        @RequestParam(value="sortAscending", required = false) Boolean sortAscending) throws InvalidTransactionSearchException {
        return this.getExtTransactionsExt(type,from,to,category,account,sortAscending);
    }

    @PostMapping(path="/ext/money/transaction")
    public Iterable<TransactionDTO>  addTransactionExt(@RequestBody List<TransactionDTO> transaction) throws InvalidTransactionException {
        return this.accountTransactionManager.createTransaction(transaction);
    }

    @PostMapping(path="/int/money/transaction")
    public Iterable<TransactionDTO>  addTransactionInt(@RequestBody List<TransactionDTO> transaction) throws InvalidTransactionException {
        return this.accountTransactionManager.createTransaction(transaction);
    }

    @PutMapping(path="/ext/money/transaction")
    public Iterable<TransactionDTO> updateTransactionExt(@RequestBody TransactionDTO transaction) throws InvalidTransactionIdException, UpdateDeleteCategoryException {
        return this.accountTransactionManager.updateTransaction(transaction);
    }

    @PutMapping(path="/int/money/transaction")
    public Iterable<TransactionDTO> updateTransactionInt(@RequestBody TransactionDTO transaction) throws InvalidTransactionIdException, UpdateDeleteCategoryException {
        return this.accountTransactionManager.updateTransaction(transaction);
    }

    @DeleteMapping(path="/ext/money/transaction")
    public Iterable<TransactionDTO> deleteExternal(@RequestBody TransactionDTO transaction) throws InvalidTransactionIdException {
        return this.accountTransactionManager.deleteTransaction(transaction);
    }

    @DeleteMapping(path="/int/money/transaction")
    public Iterable<TransactionDTO> deleteInternal( @RequestBody TransactionDTO transaction) throws InvalidTransactionIdException {
        return this.accountTransactionManager.deleteTransaction(transaction);
    }
}
