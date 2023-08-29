package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dto.DateRangeDTO;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.manager.AccountTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Created by jason on 08/03/17.
 */
@Controller
@RequestMapping("/jbr")
public class TransactionController {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionController.class);

    private final AccountTransactionManager accountTransactionManager;

    @Autowired
    public TransactionController(AccountTransactionManager accountTransactionManager) {
        this.accountTransactionManager = accountTransactionManager;
    }

    @GetMapping(path="/ext/money/transaction")
    public @ResponseBody Iterable<TransactionDTO> getExtTransactionsExt(@RequestParam(value="type", required = false) String type,
                                                                        @RequestParam(value="from", required = false) String from,
                                                                        @RequestParam(value="to", required = false) String to,
                                                                        @RequestParam(value="category", required = false)  String category,
                                                                        @RequestParam(value="account", required = false)  String account,
                                                                        @RequestParam(value="sortAscending", required = false) Boolean sortAscending) throws InvalidTransactionSearchException {

        LOG.info("Get Transactions {} {} {} {} {} {}", type, from, to, category, account, sortAscending);
        return accountTransactionManager.getTransactions(TransactionRequestType.getTransactionType(type),
                new DateRangeDTO(from, to),
                category == null ? null : Arrays.asList(category.split(",")),
                account == null ? null : Arrays.asList(account.split(",")),
                Boolean.TRUE.equals(sortAscending));
    }

    @GetMapping(path="/int/money/transaction")
    public @ResponseBody Iterable<TransactionDTO> getExtTransactionsInt(@RequestParam(value="type", required = false) String type,
                                                                        @RequestParam(value="from", required = false) String from,
                                                                        @RequestParam(value="to", required = false) String to,
                                                                        @RequestParam(value="category", required = false)  String category,
                                                                        @RequestParam(value="account", required = false) String account,
                                                                        @RequestParam(value="sortAscending", required = false) Boolean sortAscending) throws InvalidTransactionSearchException {
        return this.getExtTransactionsExt(type,from,to,category,account,sortAscending);
    }

    @PostMapping(path="/ext/money/transaction")
    public @ResponseBody Iterable<TransactionDTO>  addTransactionExt(@RequestBody List<TransactionDTO> transaction) throws UpdateDeleteCategoryException, UpdateDeleteAccountException, InvalidTransactionException {
        return this.accountTransactionManager.createTransaction(transaction);
    }

    @PostMapping(path="/int/money/transaction")
    public @ResponseBody Iterable<TransactionDTO>  addTransactionInt(@RequestBody List<TransactionDTO> transaction) throws UpdateDeleteCategoryException, UpdateDeleteAccountException, InvalidTransactionException {
        return this.accountTransactionManager.createTransaction(transaction);
    }

    @PutMapping(path="/ext/money/transaction")
    public @ResponseBody Iterable<TransactionDTO> updateTransactionExt(@RequestBody TransactionDTO transaction) throws InvalidTransactionIdException, UpdateDeleteCategoryException {
        return this.accountTransactionManager.updateTransaction(transaction);
    }

    @PutMapping(path="/int/money/transaction")
    public @ResponseBody Iterable<TransactionDTO> updateTransactionInt(@RequestBody TransactionDTO transaction) throws InvalidTransactionIdException, UpdateDeleteCategoryException {
        return this.accountTransactionManager.updateTransaction(transaction);
    }

    @DeleteMapping(path="/ext/money/transaction")
    public @ResponseBody Iterable<TransactionDTO> deleteExternal(@RequestBody TransactionDTO transaction) throws InvalidTransactionIdException {
        return this.accountTransactionManager.deleteTransaction(transaction);
    }

    @DeleteMapping(path="/int/money/transaction")
    public @ResponseBody Iterable<TransactionDTO> deleteInternal( @RequestBody TransactionDTO transaction) throws InvalidTransactionIdException {
        return this.accountTransactionManager.deleteTransaction(transaction);
    }
}
