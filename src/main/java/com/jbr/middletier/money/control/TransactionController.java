package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.primary.TransactionRequestType;
import com.jbr.middletier.money.dto.DateRangeDTO;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.manager.AccountTransactionManager;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Pattern;
import java.util.*;

/**
 * Created by jason on 08/03/17.
 */
@RestController
@RequestMapping("/jbr")
@Validated
public class TransactionController {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionController.class);

    private final AccountTransactionManager accountTransactionManager;

    @Autowired
    public TransactionController(AccountTransactionManager accountTransactionManager) {
        this.accountTransactionManager = accountTransactionManager;
    }

    @GetMapping(path="/ext/money/transaction")
    public Iterable<TransactionDTO> getExtTransactionsExt(@RequestParam(value="type", required = false) @Pattern(regexp="^[a-zA-Z]{2}$",message="Type must be a two letter code") String type,
                                                                        @RequestParam(value="from", required = false) @Pattern(regexp="^\\d{4}-\\d{2}-\\d{2}$",message="From must be valid date in format yyyy-dd-mm") String from,
                                                                        @RequestParam(value="to", required = false) @Pattern(regexp="^\\d{4}-\\d{2}-\\d{2}$",message="To must be valid date in format yyyy-dd-mm") String to,
                                                                        @RequestParam(value="category", required = false) @Pattern(regexp="^[a-zA-Z,]*$",message="Category must be a comma separated list of ids") String category,
                                                                        @RequestParam(value="account", required = false) @Pattern(regexp="^[\\da-zA-Z,]*$",message="Account must be a comma separated list of ids") String account,
                                                                        @RequestParam(value="sortAscending", required = false) Boolean sortAscending) throws InvalidTransactionSearchException {

        LOG.info("Get Transactions {} {} {} {} {} {}", type, from, to, category, account, sortAscending);
        return accountTransactionManager.getTransactions(TransactionRequestType.getTransactionType(type),
                new DateRangeDTO(from, to),
                category == null ? null : Arrays.asList(category.split(",")),
                account == null ? null : Arrays.asList(account.split(",")),
                Boolean.TRUE.equals(sortAscending));
    }

    @GetMapping(path="/int/money/transaction")
    public Iterable<TransactionDTO> getExtTransactionsInt(@RequestParam(value="type", required = false) @Pattern(regexp="^[a-zA-Z]{2}$",message="Type must be a two letter code") String type,
                                                          @RequestParam(value="from", required = false) @Pattern(regexp="^\\d{4}-\\d{2}-\\d{2}$",message="From must be valid date in format yyyy-dd-mm") String from,
                                                          @RequestParam(value="to", required = false) @Pattern(regexp="^\\d{4}-\\d{2}-\\d{2}$",message="To must be valid date in format yyyy-dd-mm") String to,
                                                          @RequestParam(value="category", required = false) @Pattern(regexp="^[a-zA-Z,]*$",message="Category must be a comma separated list of ids") String category,
                                                          @RequestParam(value="account", required = false) @Pattern(regexp="^[\\da-zA-Z,]*$",message="Account must be a comma separated list of ids") String account,
                                                          @RequestParam(value="sortAscending", required = false) Boolean sortAscending) throws InvalidTransactionSearchException {
        return this.getExtTransactionsExt(type,from,to,category,account,sortAscending);
    }

    @PostMapping(path="/ext/money/transaction")
    public Iterable<TransactionDTO>  addTransactionExt(@Valid @RequestBody List<TransactionDTO> transaction) throws InvalidTransactionException {
        return this.accountTransactionManager.createTransaction(transaction);
    }

    @PostMapping(path="/int/money/transaction")
    public Iterable<TransactionDTO>  addTransactionInt(@Valid @RequestBody List<TransactionDTO> transaction) throws InvalidTransactionException {
        return this.accountTransactionManager.createTransaction(transaction);
    }

    @PutMapping(path="/ext/money/transaction")
    public Iterable<TransactionDTO> updateTransactionExt(@Valid @RequestBody TransactionDTO transaction) throws InvalidTransactionIdException, UpdateDeleteCategoryException {
        return this.accountTransactionManager.updateTransaction(transaction);
    }

    @PutMapping(path="/int/money/transaction")
    public Iterable<TransactionDTO> updateTransactionInt(@Valid @RequestBody TransactionDTO transaction) throws InvalidTransactionIdException, UpdateDeleteCategoryException {
        return this.accountTransactionManager.updateTransaction(transaction);
    }

    @DeleteMapping(path="/ext/money/transaction")
    public Iterable<TransactionDTO> deleteExternal(@Valid @RequestBody TransactionDTO transaction) throws InvalidTransactionIdException {
        return this.accountTransactionManager.deleteTransaction(transaction);
    }

    @DeleteMapping(path="/int/money/transaction")
    public Iterable<TransactionDTO> deleteInternal(@Valid @RequestBody TransactionDTO transaction) throws InvalidTransactionIdException {
        return this.accountTransactionManager.deleteTransaction(transaction);
    }
}
