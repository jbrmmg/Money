package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.CategoryRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.manager.AccountTransactionManager;
import org.junit.Assert;
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
    public void test() throws ParseException {
        transactionRepository.deleteAll();
        statementRepository.deleteAll();

        Optional<Account> account = accountRepository.findById("BANK");
        Assert.assertTrue(account.isPresent());
        Optional<Category> category = categoryRepository.findById("HSE");
        Assert.assertTrue(category.isPresent());

        List<Transaction> transactions = new ArrayList();
        transactions.add(new Transaction(account.get(),category.get(),LocalDate.of(2012,3,2),102.02,"Test"));
        transactions.add(new Transaction(account.get(),category.get(),LocalDate.of(2012,3,3),102.03,"Test"));
        transactions.add(new Transaction(account.get(),category.get(),LocalDate.of(2012,3,4),102.04,"Test"));
        transactions.add(new Transaction(account.get(),category.get(),LocalDate.of(2012,3,5),102.05,"Test"));
        transactions.add(new Transaction(account.get(),category.get(),LocalDate.of(2012,3,6),102.06,"Test"));
        transactions.add(new Transaction(account.get(),category.get(),LocalDate.of(2012,3,7),102.07,"Test"));
        transactionRepository.saveAll(transactions);
    }
}
