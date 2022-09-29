package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.TransactionWindowDTO;
import com.jbr.middletier.money.util.FinancialAmount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
public class AccountTransactionManager {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public AccountTransactionManager(AccountRepository accountRepository,
                                     TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    private void getTransactionsInWindow(TransactionWindowDTO result, Account account, LocalDate end) {
        // Get the account balance at the start date.
        result.setStartBalance(getBalanceAt(account,result.getStart()));

        List<Transaction> transactions = this.transactionRepository.findByAccountAndDateBeforeOrderByDateDesc(account,result.getStart().plusDays(1),page);
        for(Transaction next : transactions) {
            if(next.getDate().equals(result.getStart())) {
                System.out.println(next.getId() + " " + next.getStatement().getId().getMonth() + "-" + next.getStatement().getId().getYear());
            }
        }
    }

    public TransactionWindowDTO getTransactionsInWindow(LocalDate start, LocalDate end) {
        TransactionWindowDTO result = new TransactionWindowDTO();
        result.setStart(start);

        // Perform on each account
        for(Account next : accountRepository.findAll()) {
            getTransactionsInWindow(result, next, end);
        }

        return result;
    }

    public FinancialAmount getBalanceAt(Account account, LocalDate asAtStartOf) {
        Map<String,Statement> possibleStatements = new HashMap<>();

        // Find the earliest statement with the transactions we are interested in.
        boolean keepLooking = true;
        Pageable page = Pageable.ofSize(10);
        while(keepLooking) {
            for (Transaction next : transactionRepository.findByAccountAndDateBeforeOrderByDateDesc(account, asAtStartOf.plusDays(1), page)) {
                if (next.getDate().equals(asAtStartOf)) {
                    if ((next.getStatement() != null) && (!possibleStatements.containsKey(next.getStatement().getId().toString()))) {
                        possibleStatements.put(next.getStatement().getId().toString(),next.getStatement());
                    }
                } else {
                    keepLooking = false;
                }
            }
        }

        // If no statements were found.

        return new FinancialAmount(0);
    }
}
