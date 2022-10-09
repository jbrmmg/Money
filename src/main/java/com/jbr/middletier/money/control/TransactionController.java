package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.*;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.manager.AccountTransactionManager;
import com.jbr.middletier.money.reporting.EmailGenerator;
import org.modelmapper.ModelMapper;
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
    public @ResponseBody Iterable<TransactionDTO> getExtTransactionsExt(@RequestParam(value="type") String type,
                                                                        @RequestParam(value="from") String from,
                                                                        @RequestParam(value="to") String to,
                                                                        @RequestParam(value="category")  String category,
                                                                        @RequestParam(value="account")  String account,
                                                                        @RequestParam(value="sortAscending") Boolean sortAscending) throws InvalidTransactionSearchException {
        return accountTransactionManager.getTransactions(TransactionRequestType.getTransactionType(type),
                new DateRange(from, to),
                Arrays.asList(category.split(",")),
                Arrays.asList(account.split(",")),
                Boolean.TRUE.equals(sortAscending));
    }

    @GetMapping(path="/int/money/transaction")
    public @ResponseBody Iterable<TransactionDTO> getExtTransactionsInt(@RequestParam(value="type") String type,
                                                                        @RequestParam(value="from") String from,
                                                                        @RequestParam(value="to") String to,
                                                                        @RequestParam(value="category")  String category,
                                                                        @RequestParam(value="account") String account,
                                                                        @RequestParam(value="sortAscending") Boolean sortAscending) throws InvalidTransactionSearchException {
        return this.getExtTransactionsExt(type,from,to,category,account,sortAscending);
    }

    @PostMapping(path="/ext/money/transaction")
    public @ResponseBody Iterable<TransactionDTO>  addTransactionExt(@RequestBody List<TransactionDTO> transaction) throws InvalidCategoryIdException, InvalidAccountIdException, InvalidTransactionException {
        return this.accountTransactionManager.createTransaction(transaction);
    }

    @PostMapping(path="/int/money/transaction")
    public @ResponseBody Iterable<TransactionDTO>  addTransactionInt(@RequestBody List<TransactionDTO> transaction) throws InvalidCategoryIdException, InvalidAccountIdException, InvalidTransactionException {
        return this.accountTransactionManager.createTransaction(transaction);
    }

    @PutMapping(path="/ext/money/transaction")
    public @ResponseBody Iterable<TransactionDTO> updateTransactionExt(@RequestBody TransactionDTO transaction) throws InvalidTransactionIdException, InvalidCategoryIdException {
        return this.accountTransactionManager.updateTransaction(transaction);
    }

    @PutMapping(path="/int/money/transaction")
    public @ResponseBody Iterable<TransactionDTO> updateTransactionInt(@RequestBody TransactionDTO transaction) throws InvalidTransactionIdException, InvalidCategoryIdException {
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
