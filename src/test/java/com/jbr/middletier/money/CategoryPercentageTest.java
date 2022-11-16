package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.util.CategoryPercentageHelper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
public class CategoryPercentageTest {
    @Autowired
    TransactionRepository transactionRepository;

    private void createTransaction(String categoryId, double amount) {
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

        createTransaction("HSE", -100);

        List<Transaction> transactions = new ArrayList<>();
        for(Transaction next : transactionRepository.findAll()) {
            transactions.add(next);
        }

        CategoryPercentageHelper categoryPercentageHelper = new CategoryPercentageHelper(transactions);
        Assert.assertEquals(1, categoryPercentageHelper.getCategories().size());
        for(Category next : categoryPercentageHelper.getCategories()) {
            Assert.assertEquals(100.0, categoryPercentageHelper.getPercentage(next),0.1);
        }

        transactionRepository.deleteAll();
    }

    @Test
    public void doubleTest() {
        transactionRepository.deleteAll();

        createTransaction("HSE", -100);
        createTransaction("FDG", -100);

        List<Transaction> transactions = new ArrayList<>();
        for(Transaction next : transactionRepository.findAll()) {
            transactions.add(next);
        }

        CategoryPercentageHelper categoryPercentageHelper = new CategoryPercentageHelper(transactions);
        Assert.assertEquals(2, categoryPercentageHelper.getCategories().size());
        for(Category next : categoryPercentageHelper.getCategories()) {
            Assert.assertEquals(50.0, categoryPercentageHelper.getPercentage(next),0.1);
        }

        transactionRepository.deleteAll();
    }

    @Test
    public void zeroPercentTest() {
        transactionRepository.deleteAll();

        createTransaction("HSE", 10);

        List<Transaction> transactions = new ArrayList<>();
        for(Transaction next : transactionRepository.findAll()) {
            transactions.add(next);
        }

        CategoryPercentageHelper categoryPercentageHelper = new CategoryPercentageHelper(transactions);
        Assert.assertEquals(1, categoryPercentageHelper.getCategories().size());
        for(Category next : categoryPercentageHelper.getCategories()) {
            Assert.assertEquals(0.0, categoryPercentageHelper.getPercentage(next),0.1);
        }

        transactionRepository.deleteAll();
    }

    @Test
    public void zeroPercentTest2() {
        transactionRepository.deleteAll();

        createTransaction("WGS", -100);

        List<Transaction> transactions = new ArrayList<>();
        for(Transaction next : transactionRepository.findAll()) {
            transactions.add(next);
        }

        CategoryPercentageHelper categoryPercentageHelper = new CategoryPercentageHelper(transactions);
        Assert.assertEquals(0, categoryPercentageHelper.getCategories().size());
        for(Category next : categoryPercentageHelper.getCategories()) {
            Assert.assertEquals(0.0, categoryPercentageHelper.getPercentage(next),0.1);
        }

        transactionRepository.deleteAll();
    }

    @Test
    public void multipleTest() {
        transactionRepository.deleteAll();

        createTransaction("HSE", -100);
        createTransaction("HSE", -50);
        createTransaction("HSE", -25);
        createTransaction("HSE", -10);
        createTransaction("FDG", -100);
        createTransaction("FDG", -50);
        createTransaction("FDG", -25);
        createTransaction("FDG", -10);

        List<Transaction> transactions = new ArrayList<>();
        for(Transaction next : transactionRepository.findAll()) {
            transactions.add(next);
        }

        CategoryPercentageHelper categoryPercentageHelper = new CategoryPercentageHelper(transactions);
        Assert.assertEquals(2, categoryPercentageHelper.getCategories().size());
        for(Category next : categoryPercentageHelper.getCategories()) {
            Assert.assertEquals(50.0, categoryPercentageHelper.getPercentage(next),0.1);
        }

        transactionRepository.deleteAll();
    }

    @Test
    public void multipleTest2() {
        transactionRepository.deleteAll();

        createTransaction("HSE", -100);
        createTransaction("HSE", -50);
        createTransaction("HSE", -25);
        createTransaction("HSE", -10);
        createTransaction("FDG", -100);
        createTransaction("FDG", -50);
        createTransaction("WGS", -25);
        createTransaction("FDG", -10);

        List<Transaction> transactions = new ArrayList<>();
        for(Transaction next : transactionRepository.findAll()) {
            transactions.add(next);
        }

        CategoryPercentageHelper categoryPercentageHelper = new CategoryPercentageHelper(transactions);
        Assert.assertEquals(2, categoryPercentageHelper.getCategories().size());
        for(Category next : categoryPercentageHelper.getCategories()) {
            if(next.getId().equals("FDG")) {
                Assert.assertEquals(46.38, categoryPercentageHelper.getPercentage(next), 0.005);
            } else if (next.getId().equals("HSE")) {
                Assert.assertEquals(53.62, categoryPercentageHelper.getPercentage(next), 0.005);
            }
        }

        transactionRepository.deleteAll();
    }
}
