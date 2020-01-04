package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.NewTransaction;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jason on 08/03/17.
 */
@Controller
@RequestMapping("/jbr")
public class AddTransactionController {
    final static private Logger LOG = LoggerFactory.getLogger(AddTransactionController.class);

    private final
    TransactionRepository transactionRepository;

    @Autowired
    public AddTransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @ExceptionHandler(IllegalStateException.class)
    public void handleIllegalArgumentException(IllegalStateException e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value());
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

    @RequestMapping(path="/ext/money/transaction/add", method= RequestMethod.POST)
    public @ResponseBody Iterable<Transaction>  addTransactionExt(@RequestBody NewTransaction newTransaction) throws ParseException {
        return addTransaction(newTransaction);
    }


    @RequestMapping(path="/int/money/transaction/add", method= RequestMethod.POST)
    public @ResponseBody Iterable<Transaction>  addTransactionInt(@RequestBody NewTransaction newTransaction) throws ParseException {
        return addTransaction(newTransaction);
    }
}
