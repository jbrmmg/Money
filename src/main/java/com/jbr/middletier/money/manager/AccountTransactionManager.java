package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.TransactionWindowDTO;
import com.jbr.middletier.money.util.FinancialAmount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;


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
        for(Transaction next: transactionRepository.findByAccountAndDateBeforeOrderByDateDesc(account, asAtStartOf, Pageable.ofSize(10))) {

        }

        return new FinancialAmount(0);
    }
}
