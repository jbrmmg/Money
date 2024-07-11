package com.jbr.middletier.money.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.Support;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.manager.*;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

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

    @Autowired
    private ReconciliationManager reconciliationManager;

    @Autowired
    private StatementManager statementManager;

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
    public void testIndividualTransaction() throws UpdateDeleteCategoryException, InvalidTransactionException, InvalidTransactionIdException {
        LOG.info("Test transaction events.");
        TransactionDTO transaction = new TransactionDTO();
        transaction.setAccountId("BANK");
        transaction.setCategoryId("HSE");
        transaction.setAmount(BigDecimal.valueOf(12.21));
        transaction.setDate("2023-02-12");
        transaction.setDescription("Testing");

        // Check that creating transaction is replicated to the report.
        List<TransactionDTO> saved = this.accountTransactionManager.createTransaction(Collections.singletonList(transaction));

        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        List<TransactionReportDTO> reportTransactions = transactionReportManager.getTransactions(filter);

        Assert.assertEquals(1,reportTransactions.size());

        TransactionReportDTO reportTransaction = reportTransactions.get(0);
        Assert.assertEquals(transaction.getAccountId(),reportTransaction.getAccount().getId());
        Assert.assertEquals(transaction.getDate(),reportTransaction.getDate());
        Assert.assertEquals(transaction.getAmount().doubleValue(),reportTransaction.getAmount().getValue().doubleValue(),0.001);
        Assert.assertEquals(transaction.getDescription(),reportTransaction.getDescription());
        Assert.assertEquals(transaction.getCategoryId(),reportTransaction.getCategory().getId());
        Assert.assertEquals(false,reportTransaction.getPredicted());
        Assert.assertEquals(false,reportTransaction.getFromReconciliation());
        Assert.assertNull(reportTransaction.getStatement());

        // Check that amending the transaction is replicated.
        Assert.assertEquals(1,saved.size());
        saved.get(0).setAmount(BigDecimal.valueOf(13.31));
        saved.get(0).setDate("2023-02-11");
        saved.get(0).setCategoryId("UTT");
        saved.get(0).setDescription("Testing 2");
        this.accountTransactionManager.updateTransaction(saved.get(0));

        reportTransactions = transactionReportManager.getTransactions(filter);
        reportTransaction = reportTransactions.get(0);
        Assert.assertEquals(saved.get(0).getAccountId(),reportTransaction.getAccount().getId());
        Assert.assertEquals(saved.get(0).getDate(),reportTransaction.getDate());
        Assert.assertEquals(saved.get(0).getAmount().doubleValue(),reportTransaction.getAmount().getValue().doubleValue(),0.001);
        Assert.assertEquals(saved.get(0).getDescription(),reportTransaction.getDescription());
        Assert.assertEquals(saved.get(0).getCategoryId(),reportTransaction.getCategory().getId());
        Assert.assertEquals(false,reportTransaction.getPredicted());
        Assert.assertEquals(false,reportTransaction.getFromReconciliation());
        Assert.assertNull(reportTransaction.getStatement());

        // Check that deleting the transaction is replicated
        this.accountTransactionManager.deleteTransaction(saved.get(0));
        reportTransactions = transactionReportManager.getTransactions(filter);
        Assert.assertEquals(0,reportTransactions.size());
    }

    @Test
    public void testTransfer() throws UpdateDeleteCategoryException, InvalidTransactionException, InvalidTransactionIdException {
        LOG.info("Test transaction transfer events.");
        List<TransactionDTO> transactions = new ArrayList<>();
        TransactionDTO transaction = new TransactionDTO();
        transaction.setAccountId("BANK");
        transaction.setCategoryId("HSE");
        transaction.setAmount(BigDecimal.valueOf(12.21));
        transaction.setDate("2023-02-12");
        transaction.setDescription("Transfer");
        transactions.add(transaction);

        transaction = new TransactionDTO();
        transaction.setAccountId("AMEX");
        transaction.setCategoryId("HSE");
        transaction.setAmount(BigDecimal.valueOf(-12.21));
        transaction.setDate("2023-02-12");
        transaction.setDescription("Transfer");
        transactions.add(transaction);

        // Check that creating transaction is replicated to the report.
        List<TransactionDTO> saved = this.accountTransactionManager.createTransaction(transactions);

        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);

        List<TransactionReportDTO> reportTransactions = transactionReportManager.getTransactions(filter);

        Assert.assertEquals(2,reportTransactions.size());

        TransactionReportDTO bank;
        TransactionReportDTO amex;
        if(reportTransactions.get(0).getAccount().getId().equals("BANK")) {
            bank = reportTransactions.get(0);
            amex = reportTransactions.get(1);
        } else {
            bank = reportTransactions.get(1);
            amex = reportTransactions.get(0);
        }

        Assert.assertEquals("BANK",bank.getAccount().getId());
        Assert.assertEquals("AMEX",amex.getAccount().getId());
        Assert.assertEquals(transaction.getDate(),bank.getDate());
        Assert.assertEquals(transaction.getDate(),amex.getDate());
        Assert.assertEquals(-1 * transaction.getAmount().doubleValue(),bank.getAmount().getValue().doubleValue(),0.001);
        Assert.assertEquals(transaction.getAmount().doubleValue(),amex.getAmount().getValue().doubleValue(),0.001);
        Assert.assertEquals(transaction.getDescription(),bank.getDescription());
        Assert.assertEquals(transaction.getDescription(),amex.getDescription());
        Assert.assertEquals(CategoryManager.CATEGORY_TRANSFER,bank.getCategory().getId());
        Assert.assertEquals(CategoryManager.CATEGORY_TRANSFER,amex.getCategory().getId());
        Assert.assertEquals(false,bank.getPredicted());
        Assert.assertEquals(false,amex.getPredicted());
        Assert.assertEquals(false,bank.getFromReconciliation());
        Assert.assertEquals(false,amex.getFromReconciliation());
        Assert.assertNull(bank.getStatement());
        Assert.assertNull(amex.getStatement());

        // Check that amending the transaction is replicated.
        Assert.assertEquals(2,saved.size());
        saved.get(0).setAmount(BigDecimal.valueOf(13.31));
        saved.get(0).setDate("2023-02-11");
        saved.get(0).setCategoryId("UTT");
        saved.get(0).setDescription("Transfer 2");
        this.accountTransactionManager.updateTransaction(saved.get(0));

        reportTransactions = transactionReportManager.getTransactions(filter);

        Assert.assertEquals(2,reportTransactions.size());

        if(reportTransactions.get(0).getAccount().getId().equals("BANK")) {
            bank = reportTransactions.get(0);
            amex = reportTransactions.get(1);
        } else {
            bank = reportTransactions.get(1);
            amex = reportTransactions.get(0);
        }

        Assert.assertEquals("BANK",bank.getAccount().getId());
        Assert.assertEquals("AMEX",amex.getAccount().getId());
        Assert.assertEquals(saved.get(0).getDate(),bank.getDate());
        Assert.assertEquals(saved.get(0).getDate(),amex.getDate());
        Assert.assertEquals(saved.get(0).getAmount().doubleValue(),bank.getAmount().getValue().doubleValue(),0.001);
        Assert.assertEquals(-1 * saved.get(0).getAmount().doubleValue(),amex.getAmount().getValue().doubleValue(),0.001);
        Assert.assertEquals(saved.get(0).getDescription(),bank.getDescription());
        Assert.assertEquals(saved.get(0).getDescription(),amex.getDescription());
        Assert.assertEquals(CategoryManager.CATEGORY_TRANSFER,bank.getCategory().getId());
        Assert.assertEquals(CategoryManager.CATEGORY_TRANSFER,amex.getCategory().getId());
        Assert.assertEquals(false,bank.getPredicted());
        Assert.assertEquals(false,amex.getPredicted());
        Assert.assertEquals(false,bank.getFromReconciliation());
        Assert.assertEquals(false,amex.getFromReconciliation());
        Assert.assertNull(bank.getStatement());
        Assert.assertNull(amex.getStatement());

        // Check that deleting the transaction is replicated
        this.accountTransactionManager.deleteTransaction(saved.get(0));
        reportTransactions = transactionReportManager.getTransactions(filter);
        Assert.assertEquals(0,reportTransactions.size());
    }

    @Test
    public void testLockTransaction() throws InvalidTransactionException, MultipleUnlockedStatementException, InvalidTransactionIdException, InvalidStatementIdException, StatementAlreadyLockedException, CannotDeleteLockedStatementException, UpdateDeleteAccountException, CannotDeleteLastStatementException {
        LOG.info("Test lock transaction events.");
        TransactionDTO transaction = new TransactionDTO();
        transaction.setAccountId("BANK");
        transaction.setCategoryId("HSE");
        transaction.setAmount(BigDecimal.valueOf(12.21));
        transaction.setDate("2023-02-12");
        transaction.setDescription("Testing");

        List<TransactionDTO> saved = this.accountTransactionManager.createTransaction(Collections.singletonList(transaction));

        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);

        List<TransactionReportDTO> reportTransactions = transactionReportManager.getTransactions(filter);

        Assert.assertEquals(1,reportTransactions.size());
        Assert.assertEquals(1,saved.size());
        Assert.assertNull(reportTransactions.get(0).getStatement());

        // Reconcile the transaction.
        this.reconciliationManager.reconcile(saved.get(0).getId(),true);

        reportTransactions = transactionReportManager.getTransactions(filter);

        Assert.assertEquals(1,reportTransactions.size());
        Assert.assertNotNull(reportTransactions.get(0).getStatement());
        Assert.assertFalse(reportTransactions.get(0).getStatement().getLocked());

        // Lock the statement.
        StatementIdDTO statementId = new StatementIdDTO();
        statementId.setAccountId(reportTransactions.get(0).getStatement().getAccountId());
        statementId.setYear(reportTransactions.get(0).getStatement().getYear());
        statementId.setMonth(reportTransactions.get(0).getStatement().getMonth());
        Iterable<StatementDTO> statements = this.statementManager.statementLock(statementId,this.accountTransactionManager);

        // Attempt to delete the transaction - should fail.
        try {
            this.accountTransactionManager.deleteTransaction(saved.get(0));
        } catch (InvalidTransactionIdException e) {
            Assert.assertEquals("Cannot find transaction with id ", e.getMessage().substring(0,32));
        }

        // Check report is updated.
        reportTransactions = transactionReportManager.getTransactions(filter);

        Assert.assertEquals(1,reportTransactions.size());
        Assert.assertTrue(reportTransactions.get(0).getStatement().getLocked());

        // The lock request would have created a new statement, delete it to unlock
        for(StatementDTO next : statements) {
            if(!next.getLocked()) {
                this.statementManager.deleteStatement(next,this.accountTransactionManager);
            }
        }

        // Unreconcile the transaction.
        this.reconciliationManager.reconcile(saved.get(0).getId(),false);
        reportTransactions = transactionReportManager.getTransactions(filter);

        Assert.assertEquals(1,reportTransactions.size());
        Assert.assertNull(reportTransactions.get(0).getStatement());

        // delete the transaction
        this.accountTransactionManager.deleteTransaction(saved.get(0));
        reportTransactions = transactionReportManager.getTransactions(filter);

        Assert.assertEquals(0,reportTransactions.size());
    }

    @Test
    public void testDeleteStatement() throws InvalidTransactionException, MultipleUnlockedStatementException, InvalidTransactionIdException, InvalidStatementIdException, StatementAlreadyLockedException, CannotDeleteLockedStatementException, UpdateDeleteAccountException, CannotDeleteLastStatementException {
        LOG.info("Test delete statement transaction events.");
        TransactionDTO transaction = new TransactionDTO();
        transaction.setAccountId("BANK");
        transaction.setCategoryId("UTT");
        transaction.setAmount(BigDecimal.valueOf(156.21));
        transaction.setDate("2023-02-05");
        transaction.setDescription("Testing x");

        List<TransactionDTO> saved = this.accountTransactionManager.createTransaction(Collections.singletonList(transaction));

        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);

        List<TransactionReportDTO> reportTransactions = transactionReportManager.getTransactions(filter);

        Assert.assertEquals(1,reportTransactions.size());
        Assert.assertEquals(1,saved.size());
        Assert.assertNull(reportTransactions.get(0).getStatement());

        // Reconcile the transaction.
        this.reconciliationManager.reconcile(saved.get(0).getId(),true);
        reportTransactions = transactionReportManager.getTransactions(filter);
        Assert.assertEquals(1,reportTransactions.size());

        // Lock the statement.
        StatementIdDTO statementId = new StatementIdDTO();
        statementId.setAccountId(reportTransactions.get(0).getStatement().getAccountId());
        statementId.setYear(reportTransactions.get(0).getStatement().getYear());
        statementId.setMonth(reportTransactions.get(0).getStatement().getMonth());
        Iterable<StatementDTO> statements = this.statementManager.statementLock(statementId,this.accountTransactionManager);

        // Add another transaction
        transaction = new TransactionDTO();
        transaction.setAccountId("BANK");
        transaction.setCategoryId("HSE");
        transaction.setAmount(BigDecimal.valueOf(19.21));
        transaction.setDate("2023-02-25");
        transaction.setDescription("Testing y");

        List<TransactionDTO> saved2 = this.accountTransactionManager.createTransaction(Collections.singletonList(transaction));

        reportTransactions = transactionReportManager.getTransactions(filter);
        Assert.assertEquals(2,reportTransactions.size());
        Assert.assertEquals(1,saved2.size());

        // Reconcile the new transaction.
        this.reconciliationManager.reconcile(saved2.get(0).getId(),true);

        // All transactions should be reconciled.
        reportTransactions = transactionReportManager.getTransactions(filter);
        Assert.assertEquals(2,reportTransactions.size());
        for(TransactionReportDTO next : reportTransactions) {
            Assert.assertNotNull(next.getStatement());
        }

        // Delete the statement and check the transaction becomes unreconciled.
        for(StatementDTO next : statements) {
            if(!next.getLocked()) {
                this.statementManager.deleteStatement(next,this.accountTransactionManager);
            }
        }

        reportTransactions = transactionReportManager.getTransactions(filter);
        Assert.assertEquals(2,reportTransactions.size());
        for(TransactionReportDTO next : reportTransactions) {
            if(next.getDescription().equals("Testing x")) {
                Assert.assertNotNull(next.getStatement());
            } else {
                Assert.assertNull(next.getStatement());
            }
        }

        // Delete the second transaction.
        this.accountTransactionManager.deleteTransaction(saved2.get(0));

        // Unreconcile the original transaction and delete it.
        this.reconciliationManager.reconcile(saved.get(0).getId(),false);
        this.accountTransactionManager.deleteTransaction(saved.get(0));
    }

    @Test
    public void testReconciliationReporting() throws IOException, InvalidTransactionException, MultipleUnlockedStatementException, InvalidTransactionIdException {
        // Create a transaction that will be matched.
        TransactionDTO transaction = new TransactionDTO();
        transaction.setAccountId("AMEX");
        transaction.setCategoryId("HSE");
        transaction.setAmount(BigDecimal.valueOf(-8.99));
        transaction.setDate("2022-10-07");
        transaction.setDescription("OCADO TEST");

        List<TransactionDTO> saved = this.accountTransactionManager.createTransaction(Collections.singletonList(transaction));

        // Reconcile the transaction.
        this.reconciliationManager.reconcile(saved.get(0).getId(),true);

        // There should be one transaction (which is not from the reconciliation).
        List<TransactionReportDTO> reportTransactions = transactionReportManager.getTransactions(new TransactionFilterDTO());
        Assert.assertEquals(1, reportTransactions.size());
        Assert.assertEquals("OCADO TEST",reportTransactions.get(0).getDescription());
        Assert.assertFalse(reportTransactions.get(0).getFromReconciliation());

        ReconciliationFileLoadDTO file = new ReconciliationFileLoadDTO();
        file.setFilename("test.AMEX.csv");
        this.reconciliationManager.loadFile(file);

        reportTransactions = transactionReportManager.getTransactions(new TransactionFilterDTO());
        Assert.assertEquals(15, reportTransactions.size());

        // Check that the reconciled transaction is still there.
        boolean found = false;
        for(TransactionReportDTO next : reportTransactions) {
            if(next.getDescription().equals("OCADO TEST")) {
                found = true;
                Assert.assertEquals(saved.get(0).getId(),(int)next.getTransactionId());
                Assert.assertTrue(next.getFromReconciliation());
            }
        }
        Assert.assertTrue(found);

        // Change to another file.
        file = new ReconciliationFileLoadDTO();
        file.setFilename("test.FirstDirect.csv");
        this.reconciliationManager.loadFile(file);

        reportTransactions = transactionReportManager.getTransactions(new TransactionFilterDTO());
        Assert.assertEquals(19, reportTransactions.size());

        // Check that the original transaction is still there but not from reconciliation.
        found = false;
        for(TransactionReportDTO next : reportTransactions) {
            if(next.getDescription().equals("OCADO TEST")) {
                found = true;
                Assert.assertEquals(saved.get(0).getId(),(int)next.getTransactionId());
                Assert.assertFalse(next.getFromReconciliation());
            }
        }
        Assert.assertTrue(found);

        // Delete the transaction
        this.reconciliationManager.reconcile(saved.get(0).getId(),false);
        this.accountTransactionManager.deleteTransaction(saved.get(0));

        reportTransactions = transactionReportManager.getTransactions(new TransactionFilterDTO());
        Assert.assertEquals(18, reportTransactions.size());
    }
}
