package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.CategoryRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.TransactionWindowDTO;
import com.jbr.middletier.money.manager.AccountTransactionManager;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class TransactionManagerTest {
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private StatementRepository statementRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AccountTransactionManager transactionManager;


    @Test
    @Ignore("This test is not yet ready.")
    public void test() throws ParseException {
        transactionRepository.deleteAll();
        statementRepository.deleteAll();

        Optional<Account> account = accountRepository.findById("BANK");
        Assert.assertTrue(account.isPresent());
        Optional<Category> category = categoryRepository.findById("HSE");
        Assert.assertTrue(category.isPresent());

        List<Transaction> transactions = new ArrayList<>();
        LocalDate nextTransactionDate = LocalDate.of(2012,1,1);
        LocalDate lastTransactionDate = LocalDate.of(2012,4,4);
        while(nextTransactionDate.isBefore(lastTransactionDate)) {
            transactions.add(new Transaction(account.get(),category.get(),nextTransactionDate,1.02, "Test"));
            nextTransactionDate = nextTransactionDate.plusDays(1);
        }

        transactionRepository.saveAll(transactions);

        Statement statement = new Statement();
        statement.setId(new StatementId(account.get(), 2012, 3));
        statement.setLocked(false);
        statement.setOpenBalance(20);
        statementRepository.save(statement);

        LocalDate lockUpTo = LocalDate.of (2012, 3, 4);

        for(Transaction next : transactions) {
            if(next.getDate().isBefore(lockUpTo) || next.getDate().isEqual(lockUpTo)) {
                next.setStatement(statement);
                transactionRepository.save(next);
            }
        }

        statement.setLocked(true);
        statementRepository.save(statement);

        statement = new Statement();
        statement.setId(new StatementId(account.get(), 2012, 4));
        statement.setLocked(false);
        statement.setOpenBalance(20);
        statementRepository.save(statement);

        for(Transaction next : transactions) {
            if(next.getDate().isAfter(lockUpTo)) {
                next.setStatement(statement);
                transactionRepository.save(next);
            }
        }

        LocalDate windowStart = LocalDate.of(2012,3,2);
        Transaction rougeTransaction = new Transaction(account.get(),category.get(),windowStart,1.02, "Test");
        rougeTransaction.setStatement(statement);
        transactionRepository.save(rougeTransaction);

        TransactionWindowDTO transactionWindow = transactionManager.getTransactionsInWindow(windowStart, null);
    }
}
