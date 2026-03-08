package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.primary.Account;
import com.jbr.middletier.money.data.primary.Category;
import com.jbr.middletier.money.data.primary.Transaction;
import com.jbr.middletier.money.data.primary.repository.TransactionRepository;
import com.jbr.middletier.money.util.CategoryPercentageHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(classes = MiddleTier.class)
public class CategoryPercentageTest {
    @Autowired
    TransactionRepository transactionRepository;

    private void createTransaction(String categoryId, BigDecimal amount) {
        Category category = new Category();
        category.setId(categoryId);

        Account account = new Account();
        account.setId("BANK");

        Transaction transaction = new Transaction();
        transaction.setCategory(category);
        transaction.setAccount(account);
        transaction.setAmount(amount);
        transaction.setDescription("Test");

        transactionRepository.save(transaction);
    }

    @Test
    public void singleTest() {
        transactionRepository.deleteAll();

        createTransaction("HSE",  BigDecimal.valueOf(-100));

        List<Transaction> transactions = new ArrayList<>();
        for(Transaction next : transactionRepository.findAll()) {
            transactions.add(next);
        }

        CategoryPercentageHelper categoryPercentageHelper = new CategoryPercentageHelper(transactions);
        Assertions.assertEquals(1, categoryPercentageHelper.getCategories().size());
        for(Category next : categoryPercentageHelper.getCategories()) {
            Assertions.assertEquals(100.0, categoryPercentageHelper.getPercentage(next),0.1);
        }

        transactionRepository.deleteAll();
    }

    @Test
    public void doubleTest() {
        transactionRepository.deleteAll();

        createTransaction("HSE", BigDecimal.valueOf(-100));
        createTransaction("FDG", BigDecimal.valueOf(-100));

        List<Transaction> transactions = new ArrayList<>();
        for(Transaction next : transactionRepository.findAll()) {
            transactions.add(next);
        }

        CategoryPercentageHelper categoryPercentageHelper = new CategoryPercentageHelper(transactions);
        Assertions.assertEquals(2, categoryPercentageHelper.getCategories().size());
        for(Category next : categoryPercentageHelper.getCategories()) {
            Assertions.assertEquals(50.0, categoryPercentageHelper.getPercentage(next),0.1);
        }

        transactionRepository.deleteAll();
    }

    @Test
    public void zeroPercentTest() {
        transactionRepository.deleteAll();

        createTransaction("HSE", BigDecimal.valueOf(10));

        List<Transaction> transactions = new ArrayList<>();
        for(Transaction next : transactionRepository.findAll()) {
            transactions.add(next);
        }

        CategoryPercentageHelper categoryPercentageHelper = new CategoryPercentageHelper(transactions);
        Assertions.assertEquals(1, categoryPercentageHelper.getCategories().size());
        for(Category next : categoryPercentageHelper.getCategories()) {
            Assertions.assertEquals(0.0, categoryPercentageHelper.getPercentage(next),0.1);
        }

        transactionRepository.deleteAll();
    }

    @Test
    public void zeroPercentTest2() {
        transactionRepository.deleteAll();

        createTransaction("WGS", BigDecimal.valueOf(-100));

        List<Transaction> transactions = new ArrayList<>();
        for(Transaction next : transactionRepository.findAll()) {
            transactions.add(next);
        }

        CategoryPercentageHelper categoryPercentageHelper = new CategoryPercentageHelper(transactions);
        Assertions.assertEquals(0, categoryPercentageHelper.getCategories().size());
        for(Category next : categoryPercentageHelper.getCategories()) {
            Assertions.assertEquals(0.0, categoryPercentageHelper.getPercentage(next),0.1);
        }

        transactionRepository.deleteAll();
    }

    @Test
    public void multipleTest() {
        transactionRepository.deleteAll();

        createTransaction("HSE", BigDecimal.valueOf(-100));
        createTransaction("HSE", BigDecimal.valueOf(-50));
        createTransaction("HSE", BigDecimal.valueOf(-25));
        createTransaction("HSE", BigDecimal.valueOf(-10));
        createTransaction("FDG", BigDecimal.valueOf(-100));
        createTransaction("FDG", BigDecimal.valueOf(-50));
        createTransaction("FDG", BigDecimal.valueOf(-25));
        createTransaction("FDG", BigDecimal.valueOf(-10));

        List<Transaction> transactions = new ArrayList<>();
        for(Transaction next : transactionRepository.findAll()) {
            transactions.add(next);
        }

        CategoryPercentageHelper categoryPercentageHelper = new CategoryPercentageHelper(transactions);
        Assertions.assertEquals(2, categoryPercentageHelper.getCategories().size());
        for(Category next : categoryPercentageHelper.getCategories()) {
            Assertions.assertEquals(50.0, categoryPercentageHelper.getPercentage(next),0.1);
        }

        transactionRepository.deleteAll();
    }

    @Test
    public void multipleTest2() {
        transactionRepository.deleteAll();

        createTransaction("HSE", BigDecimal.valueOf(-100));
        createTransaction("HSE", BigDecimal.valueOf(-50));
        createTransaction("HSE", BigDecimal.valueOf(-25));
        createTransaction("HSE", BigDecimal.valueOf(-10));
        createTransaction("FDG", BigDecimal.valueOf(-100));
        createTransaction("FDG", BigDecimal.valueOf(-50));
        createTransaction("WGS", BigDecimal.valueOf(-25));
        createTransaction("FDG", BigDecimal.valueOf(-10));

        List<Transaction> transactions = new ArrayList<>();
        for(Transaction next : transactionRepository.findAll()) {
            transactions.add(next);
        }

        CategoryPercentageHelper categoryPercentageHelper = new CategoryPercentageHelper(transactions);
        Assertions.assertEquals(2, categoryPercentageHelper.getCategories().size());
        for(Category next : categoryPercentageHelper.getCategories()) {
            if(next.getId().equals("FDG")) {
                Assertions.assertEquals(46.38, categoryPercentageHelper.getPercentage(next), 0.005);
            } else if (next.getId().equals("HSE")) {
                Assertions.assertEquals(53.62, categoryPercentageHelper.getPercentage(next), 0.005);
            }
        }

        transactionRepository.deleteAll();
    }
}
