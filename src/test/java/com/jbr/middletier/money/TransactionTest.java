package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.dto.mapper.TransactionMapper;
import com.jbr.middletier.money.exceptions.UpdateDeleteCategoryException;
import com.jbr.middletier.money.exceptions.InvalidTransactionIdException;
import com.jbr.middletier.money.manager.AccountTransactionManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.time.LocalDate;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class TransactionTest extends Support {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountTransactionManager accountTransactionManager;

    @Autowired
    private TransactionMapper transactionMapper;

    @Before
    public void cleanUp() {
        transactionRepository.deleteAll();
    }

    @Test
    public void testInvalidSearch() throws Exception {
        String error = Objects.requireNonNull(getMockMvc().perform(get("/jbr/int/money/transaction?type=AL")
                        .contentType(getContentType()))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("must specify a from date", error);
    }

    @Test
    public void testTransactionUpdate() throws InvalidTransactionIdException {
        Account account = new Account();
        account.setId("AMEX");

        Category category = new Category();
        category.setId("HSE");

        Transaction testTransaction = new Transaction();
        testTransaction.setAccount(account);
        testTransaction.setCategory(category);
        testTransaction.setDate(LocalDate.of(2010,5,1));
        testTransaction.setAmount(201.23);

        testTransaction = transactionRepository.save(testTransaction);

        TransactionDTO updateTransaction = transactionMapper.map(testTransaction,TransactionDTO.class);

        updateTransaction.setCategoryId("XXX");

        try {
            accountTransactionManager.updateTransaction(updateTransaction);
            Assert.fail();
        } catch (UpdateDeleteCategoryException ex) {
            Assert.assertEquals("Cannot find category with id XXX", ex.getMessage());
        }
    }
}
