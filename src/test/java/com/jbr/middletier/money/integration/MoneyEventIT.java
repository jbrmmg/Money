package com.jbr.middletier.money.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.Support;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.dto.TransactionDataDTO;
import com.jbr.middletier.money.dto.TransactionFilterDTO;
import com.jbr.middletier.money.dto.TransactionReportDTO;
import com.jbr.middletier.money.exceptions.InvalidTransactionException;
import com.jbr.middletier.money.exceptions.InvalidTransactionIdException;
import com.jbr.middletier.money.exceptions.UpdateDeleteCategoryException;
import com.jbr.middletier.money.manager.AccountTransactionManager;
import com.jbr.middletier.money.manager.TransactionReportManager;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testcontainers.containers.MySQLContainer;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {MoneyEventIT.Initializer.class})
@ActiveProfiles(value="it")
public class MoneyEventIT extends Support {
    private static final Logger LOG = LoggerFactory.getLogger(MoneyEventIT.class);

    @Autowired
    private TransactionReportManager transactionReportManager;

    @Autowired
    private AccountTransactionManager accountTransactionManager;

    @SuppressWarnings("rawtypes")
    @ClassRule
    public static MySQLContainer mysqlContainer = new MySQLContainer("mysql:8.0.28")
            .withDatabaseName("integration-tests-db")
            .withUsername("sa")
            .withPassword("sa");

    static class Initializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + mysqlContainer.getJdbcUrl(),
                    "spring.datasource.username=" + mysqlContainer.getUsername(),
                    "spring.datasource.password=" + mysqlContainer.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    @Test
    public void testCreateTransaction() throws UpdateDeleteCategoryException, InvalidTransactionException, InvalidTransactionIdException {
        LOG.info("Test transaction events.");
        TransactionDTO transaction = new TransactionDTO();
        transaction.setAccountId("BANK");
        transaction.setCategoryId("HSE");
        transaction.setAmount(12.21);
        transaction.setDate("2023-02-12");
        transaction.setDescription("Testing");

        List<TransactionDTO> transactions = new ArrayList<>();
        transactions.add(transaction);

        // Check that creating transaction is replicated to the report.
        List<TransactionDTO> saved = this.accountTransactionManager.createTransaction(transactions);

        TransactionDataDTO reportTransactions = transactionReportManager.getTransactions(new TransactionFilterDTO());

        Assert.assertEquals(1,reportTransactions.getTransactions().size());

        TransactionReportDTO reportTransaction = reportTransactions.getTransactions().get(0);
        Assert.assertEquals(transaction.getAccountId(),reportTransaction.getAccount().getId());
        Assert.assertEquals(transaction.getDate(),reportTransaction.getDate());
        Assert.assertEquals(transaction.getAmount(),reportTransaction.getAmount().getValue(),0.001);
        Assert.assertEquals(transaction.getDescription(),reportTransaction.getDescription());
        Assert.assertEquals(transaction.getCategoryId(),reportTransaction.getCategory().getId());
        Assert.assertEquals(false,reportTransaction.getPredicted());
        Assert.assertEquals(false,reportTransaction.getFromReconciliation());
        Assert.assertNull(reportTransaction.getStatement());

        // Check that amending the transaction is replicated.
        Assert.assertEquals(1,saved.size());
        saved.get(0).setAmount(13.31);
        saved.get(0).setDate("2023-02-11");
        saved.get(0).setCategoryId("UTT");
        this.accountTransactionManager.updateTransaction(saved.get(0));

        reportTransactions = transactionReportManager.getTransactions(new TransactionFilterDTO());
        reportTransaction = reportTransactions.getTransactions().get(0);
        Assert.assertEquals(saved.get(0).getAccountId(),reportTransaction.getAccount().getId());
        Assert.assertEquals(saved.get(0).getDate(),reportTransaction.getDate());
        Assert.assertEquals(saved.get(0).getAmount(),reportTransaction.getAmount().getValue(),0.001);
        Assert.assertEquals(saved.get(0).getDescription(),reportTransaction.getDescription());
        Assert.assertEquals(transaction.getCategoryId(),reportTransaction.getCategory().getId());
        Assert.assertEquals(false,reportTransaction.getPredicted());
        Assert.assertEquals(false,reportTransaction.getFromReconciliation());
        Assert.assertNull(reportTransaction.getStatement());

        // Check that deleting the transaction is replicated
        this.accountTransactionManager.deleteTransaction(saved.get(0));
        reportTransactions = transactionReportManager.getTransactions(new TransactionFilterDTO());
        Assert.assertEquals(0,reportTransactions.getTransactions().size());
    }
}
