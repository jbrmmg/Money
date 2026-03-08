package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.primary.Account;
import com.jbr.middletier.money.data.primary.Category;
import com.jbr.middletier.money.data.primary.Transaction;
import com.jbr.middletier.money.data.primary.repository.TransactionRepository;
import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.dto.TransactionReportDTO;
import com.jbr.middletier.money.dto.mapper.TransactionMapper;
import com.jbr.middletier.money.exceptions.InvalidTransactionException;
import com.jbr.middletier.money.manager.AccountTransactionManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class TransactionTest extends Support {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountTransactionManager accountTransactionManager;

    @Autowired
    private TransactionMapper transactionMapper;

    @BeforeEach
    public void cleanUp() {
        transactionRepository.deleteAll();
    }

    @Test
    public void testTransactionUpdate() {
        Account account = new Account();
        account.setId("AMEX");

        Category category = new Category();
        category.setId("HSE");

        Transaction testTransaction = new Transaction();
        testTransaction.setAccount(account);
        testTransaction.setCategory(category);
        testTransaction.setDate(LocalDate.of(2010,5,1));
        testTransaction.setAmount(BigDecimal.valueOf(201.23));

        testTransaction = transactionRepository.save(testTransaction);

        TransactionReportDTO updateTransaction = transactionMapper.map(testTransaction,TransactionReportDTO.class);

        CategoryDTO badCategory = new CategoryDTO();
        badCategory.setId("XXX");
        updateTransaction.setCategory(badCategory);

        List<TransactionReportDTO> transactions = new ArrayList<>();
        transactions.add(updateTransaction);

        try {
            accountTransactionManager.updateTransactions(transactions);
            Assertions.fail();
        } catch (InvalidTransactionException ex) {
            Assertions.assertEquals("None of the updates were process successfully.", ex.getMessage());
        }
    }
}
