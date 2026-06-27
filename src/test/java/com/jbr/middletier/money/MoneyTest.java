package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.config.DefaultProfileUtil;
import com.jbr.middletier.money.data.primary.*;
import com.jbr.middletier.money.data.primary.repository.*;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.TransactionMapper;
import com.jbr.middletier.money.manager.TransactionReportManager;
import com.jbr.middletier.money.util.FinancialAmount;
import com.jbr.middletier.money.utils.UtilityMapper;
import com.jbr.middletier.money.health.ServiceHealthIndicator;
import com.jbr.middletier.money.schedule.AdjustmentType;
import com.jbr.middletier.money.schedule.RegularCtrl;
import com.jbr.middletier.money.util.TransactionString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import static java.lang.Math.abs;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Created by jason on 27/03/17.
 */

@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class MoneyTest extends Support {
    @Autowired
    private
    TransactionRepository transactionRepository;

    @Autowired
    private
    ReconciliationRepository reconciliationRepository;

    @Autowired
    RegularRepository regularRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    StatementRepository statementRepository;

    @Autowired
    RegularCtrl regularCtrl;

    @Autowired
    ServiceHealthIndicator serviceHealthIndicator;

    @Autowired
    UtilityMapper utilityMapper;

    @Autowired
    TransactionReportManager transactionReportManager;

    @Autowired
    TransactionMapper transactionMapper;

    @Autowired
    ApplicationProperties applicationProperties;

    private void cleanUp() {
        transactionRepository.deleteAll();
        reconciliationRepository.deleteAll();
        regularRepository.deleteAll();
        statementRepository.deleteAll();

        for(Account next : accountRepository.findAll()) {
            Statement statement = new Statement();
            statement.setId(new StatementId(next,2010,1));
            statement.setLocked(false);
            statement.setOpenBalance(BigDecimal.ZERO);

            statementRepository.save(statement);
        }
    }

    @Test
    public void TestDefaultProfile() {
        SpringApplication app = mock(SpringApplication.class);

        Assertions.assertNotNull(app);
        DefaultProfileUtil.addDefaultProfile(app);
    }

    @Test
    public void internalTransactionTest() throws Exception {
        cleanUp();

        TransactionDTO transaction = new TransactionDTO();
        transaction.setAccountId("BANK");
        transaction.setCategoryId("FDW");
        transaction.setDate(utilityMapper.map(LocalDate.of(1968,5,24),String.class));
        transaction.setAmount(BigDecimal.valueOf(1280.32));
        transaction.setDescription("Test transaction");

        getMockMvc().perform(post("/api/v1/transaction")
                .content(this.json(Collections.singletonList(transaction)))
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount",is(1280.32)))
                .andExpect(jsonPath("$[0].description",is("Test transaction")));

        // Amend the transaction.
        Iterable<Transaction> transactions = transactionRepository.findAll();
        for(Transaction nextTransaction : transactions) {
            TransactionReportDTO updateTransaction = transactionMapper.map(nextTransaction,TransactionReportDTO.class);
            updateTransaction.setAmount(new FinancialAmount(BigDecimal.valueOf(1283.21)));

            assertEquals(1280.32, nextTransaction.getAmount().getValue().doubleValue(),0.001);

            List<TransactionReportDTO> updateTransactions = new ArrayList<>();
            updateTransactions.add(updateTransaction);

            getMockMvc().perform(put("/api/v1/transaction")
                    .content(this.json(updateTransactions))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // Delete the transaction.
        transactions = transactionRepository.findAll();
        for(Transaction nextTransaction : transactions) {
            TransactionDTO deleteTransaction = transactionMapper.map(nextTransaction,TransactionDTO.class);

            List<TransactionDTO> deleteTransactions = new ArrayList<>();
            deleteTransactions.add(deleteTransaction);

            // Delete this item.
            assertEquals(1283.21,nextTransaction.getAmount().getValue().doubleValue(),0.001);
            getMockMvc().perform(delete("/api/v1/transaction")
                    .content(this.json(deleteTransactions))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void externalTransactionTest() throws Exception {
        cleanUp();

        TransactionDTO transaction = new TransactionDTO();
        transaction.setAccountId("BANK");
        transaction.setCategoryId("FDG");
        transaction.setDate(utilityMapper.map(LocalDate.of(1968,5,24),String.class));
        transaction.setAmount(BigDecimal.valueOf(1280.32));
        transaction.setDescription("Test transaction");

        // Add transaction.
        getMockMvc().perform(post("/api/v1/transaction")
                .content(this.json(Collections.singletonList(transaction)))
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount", is(1280.32)))
                .andExpect(jsonPath("$[0].description", is("Test transaction")));

        // Edit the transactions (by editing the first transaction).
        Iterable<Transaction> transactions = transactionRepository.findAll();

        Transaction nextTransaction = transactions.iterator().next();
        assertEquals(1280.32, nextTransaction.getAmount().getValue().doubleValue(),0.001);
        TransactionReportDTO updateTransaction = transactionMapper.map(nextTransaction,TransactionReportDTO.class);
        updateTransaction.setAmount(new FinancialAmount(BigDecimal.valueOf(1283.21)));

        List<TransactionReportDTO> updateTransactions = new ArrayList<>();
        updateTransactions.add(updateTransaction);

        assertEquals(1280.32, nextTransaction.getAmount().getValue().doubleValue(),0.001);
        getMockMvc().perform(put("/api/v1/transaction")
                .content(this.json(updateTransactions))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Delete the transactions.
        transactions = transactionRepository.findAll();
        for(Transaction nextTransactionToDelete : transactions) {
            TransactionDTO deleteTransaction = transactionMapper.map(nextTransaction,TransactionDTO.class);

            List<TransactionDTO> deleteTransactions = new ArrayList<>();
            deleteTransactions.add(deleteTransaction);

            // Delete this item.
            assertEquals(1283.21, abs(nextTransactionToDelete.getAmount().getValue().doubleValue()),0.001);
            getMockMvc().perform(delete("/api/v1/transaction")
                    .content(this.json(deleteTransactions))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    // Test Reconcile / Un-reconcile transaction
    @Test
    public void reconcileTransaction() throws Exception {
        cleanUp();

        TransactionDTO transaction1 = new TransactionDTO();
        transaction1.setAccountId("BANK");
        transaction1.setDate(utilityMapper.map(LocalDate.of(1968,5,24),String.class));
        transaction1.setAmount(BigDecimal.valueOf(1280.32));

        TransactionDTO transaction2 = new TransactionDTO();
        transaction2.setAccountId("AMEX");
        transaction2.setDate(utilityMapper.map(LocalDate.of(1968,5,24),String.class));

        // Set-up a transaction.
        getMockMvc().perform(post("/api/v1/transaction")
                .content(this.json(Arrays.asList(transaction1,transaction2)))
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].amount", containsInAnyOrder(1280.32, -1280.32)));

        // Reconcile the transaction.
        Iterable<Transaction> transactions = transactionRepository.findAll();
        for(Transaction nextTransaction : transactions) {
            assertFalse(nextTransaction.reconciled());
            ReconcileTransactionDTO reconcileRequest = new ReconcileTransactionDTO();
            reconcileRequest.getTransactions().add(nextTransaction.getId());
            reconcileRequest.setReconcile(true);
            getMockMvc().perform(put("/api/v1/reconcile")
                    .content(this.json(reconcileRequest))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // Un-reconcile the transaction.
        transactions = transactionRepository.findAll();
        for(Transaction nextTransaction : transactions) {
            assertTrue(nextTransaction.reconciled());
            ReconcileTransactionDTO reconcileRequest = new ReconcileTransactionDTO();
            reconcileRequest.getTransactions().add(nextTransaction.getId());
            reconcileRequest.setReconcile(false);
            getMockMvc().perform(put("/api/v1/reconcile")
                    .content(this.json(reconcileRequest))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // Delete the transactions.
        transactions = transactionRepository.findAll();
        List<Integer> deletedIds = new ArrayList<>();
        for(Transaction nextTransaction : transactions) {
            if(!deletedIds.contains(nextTransaction.getId())) {
                // Delete this item.
                TransactionDTO nextTransactionDTO = transactionMapper.map(nextTransaction, TransactionDTO.class);

                List<TransactionDTO> deleteTransactions = new ArrayList<>();
                deleteTransactions.add(nextTransactionDTO);

                getMockMvc().perform(delete("/api/v1/transaction")
                                .content(this.json(deleteTransactions))
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk());

                // Add to the list.
                deletedIds.add(nextTransactionDTO.getId());
                if (nextTransactionDTO.getOppositeTransactionId() != null) {
                    deletedIds.add(nextTransactionDTO.getOppositeTransactionId());
                }
            }
        }
    }

    // Test Get Transactions
    @Test
    public void testGetTransaction() throws Exception {
        cleanUp();

        TransactionDTO transaction = new TransactionDTO();
        transaction.setAccountId("AMEX");
        transaction.setCategoryId("FDG");
        transaction.setDate(utilityMapper.map(LocalDate.of(1968,5,24),String.class));
        transaction.setAmount(BigDecimal.valueOf(1.23));

        // Create transactions in each account.
        getMockMvc().perform(post("/api/v1/transaction")
                .content(this.json(Collections.singletonList(transaction)))
                .contentType(getContentType()))
                .andExpect(status().isOk());

        transaction.setAccountId("JLPC");
        transaction.setAmount(BigDecimal.valueOf(3.45));
        getMockMvc().perform(post("/api/v1/transaction")
                .content(this.json(Collections.singletonList(transaction)))
                .contentType(getContentType()))
                .andExpect(status().isOk());

        TransactionFilterDTO filter = new TransactionFilterDTO();

        AccountDTO accountSearch = new AccountDTO();
        accountSearch.setId("AMEX");

        List<AccountDTO> accounts = new ArrayList<>();
        accounts.add(accountSearch);

        filter.setAccounts(accounts);

        filter.setPredicted(false);
        filter.setLocked(false);

        getMockMvc().perform(post("/api/v1/transaction/list")
                .content(this.json(filter))
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].amount.value", is(1.23)))
                .andExpect(jsonPath("$", hasSize(3)))
                .andDo(MockMvcResultHandlers.print());

        accountSearch.setId("JLPC");

        getMockMvc().perform(post("/api/v1/transaction/list")
                .content(this.json(filter))
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].amount.value", is(3.45)))
                .andExpect(jsonPath("$", hasSize(3)));

        // Create another transaction
        transaction.setAccountId("JLPC");
        transaction.setCategoryId("UTT");
        transaction.setAmount(BigDecimal.valueOf(2.78));
        getMockMvc().perform(post("/api/v1/transaction")
                .content(this.json(Collections.singletonList(transaction)))
                .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/api/v1/transaction/list")
                .content(this.json(filter))
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].amount.value", containsInAnyOrder(2.78, 3.45)))
                .andExpect(jsonPath("$", hasSize(4)));

        CategoryDTO categorySearch = new CategoryDTO();
        categorySearch.setId("FDG");

        List<CategoryDTO> categories = new ArrayList<>();
        categories.add(categorySearch);

        filter.setCategories(categories);

        getMockMvc().perform(post("/api/v1/transaction/list")
                .content(this.json(filter))
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount.value", is(3.45)))
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(MockMvcResultHandlers.print());

        accountSearch = new AccountDTO();
        accountSearch.setId("AMEX");
        accounts.add(accountSearch);

        filter.setCategories(new ArrayList<>());

        getMockMvc().perform(post("/api/v1/transaction/list")
                .content(this.json(filter))
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].amount.value", containsInAnyOrder(1.23, 3.45, 2.78)))
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    public void testRegular() throws Exception {
        cleanUp();

        LocalDate testDate = applicationProperties.getToday();

        Optional<Category> category = categoryRepository.findById("FDG");
        if(category.isEmpty()) {
            throw new Exception("Cannot find the category FDG");
        }

        Optional<Account> account = accountRepository.findById("BANK");
        if(account.isEmpty()) {
            throw new Exception("Cannot find the account BANK");
        }

        // Create a payment that starts today - should be created immediately.
        Regular testRegularPayment = new Regular();
        testRegularPayment.setAccount(account.get());
        testRegularPayment.setCategory(category.get());
        testRegularPayment.setAmount(BigDecimal.valueOf(10.0));
        testRegularPayment.setFrequency("1W");
        testRegularPayment.setStart(testDate);
        testRegularPayment.setWeekendAdj(AdjustmentType.AT_NONE);
        testRegularPayment.setDescription("Regular 1");

        regularRepository.save(testRegularPayment);

        // Create a payment, for 1 week that starts yesterday - should not create anything.
        testDate = testDate.plusDays(-1);

        testRegularPayment = new Regular();
        testRegularPayment.setAccount(account.get());
        testRegularPayment.setCategory(category.get());
        testRegularPayment.setAmount(BigDecimal.valueOf(11.0));
        testRegularPayment.setFrequency("1W");
        testRegularPayment.setStart(testDate);
        testRegularPayment.setWeekendAdj(AdjustmentType.AT_NONE);

        regularRepository.save(testRegularPayment);

        // Create a payment, for 1 week that starts 1 week ago - should create a new payment today.
        testDate = testDate.plusDays(-6);

        testRegularPayment = new Regular();
        testRegularPayment.setAccount(account.get());
        testRegularPayment.setCategory(category.get());
        testRegularPayment.setAmount(BigDecimal.valueOf(12.0));
        testRegularPayment.setFrequency("1W");
        testRegularPayment.setStart(testDate);
        testRegularPayment.setLastDate(testDate);
        testRegularPayment.setWeekendAdj(AdjustmentType.AT_NONE);

        regularRepository.save(testRegularPayment);

        // Create a payment, invalid - should not create anything.
        testDate = applicationProperties.getToday();
        testDate = testDate.plusDays(-7);

        testRegularPayment = new Regular();
        testRegularPayment.setAccount(account.get());
        testRegularPayment.setCategory(category.get());
        testRegularPayment.setAmount(BigDecimal.valueOf(13.0));
        testRegularPayment.setFrequency("1X");
        testRegularPayment.setStart(testDate);
        testRegularPayment.setLastDate(testDate);
        testRegularPayment.setWeekendAdj(AdjustmentType.AT_NONE);

        regularRepository.save(testRegularPayment);

        testRegularPayment = new Regular();
        testRegularPayment.setAccount(account.get());
        testRegularPayment.setCategory(category.get());
        testRegularPayment.setAmount(BigDecimal.valueOf(14.0));
        testRegularPayment.setFrequency("1M");
        testRegularPayment.setStart(testDate);
        testRegularPayment.setLastDate(testDate);
        testRegularPayment.setWeekendAdj(AdjustmentType.AT_NONE);

        regularRepository.save(testRegularPayment);

        testRegularPayment = new Regular();
        testRegularPayment.setAccount(account.get());
        testRegularPayment.setCategory(category.get());
        testRegularPayment.setAmount(BigDecimal.valueOf(15.0));
        testRegularPayment.setFrequency("1Y");
        testRegularPayment.setStart(testDate);
        testRegularPayment.setLastDate(testDate);
        testRegularPayment.setWeekendAdj(AdjustmentType.AT_NONE);

        regularRepository.save(testRegularPayment);

        // Process regular payments - do it twice, second should do nothing.
        regularCtrl.generateRegularPayments();
        regularCtrl.generateRegularPayments();

        // Get the transactions using the transaction report.
        TransactionFilterDTO filter = new TransactionFilterDTO();

        CategoryDTO categorySearch = new CategoryDTO();
        categorySearch.setId("FDG");

        List<CategoryDTO> categories = new ArrayList<>();
        categories.add(categorySearch);

        filter.setCategories(categories);

        filter.setPredicted(false);
        filter.setLocked(false);

        transactionReportManager.reset();

        // Check that we have 1 transaction.
        getMockMvc().perform(post("/api/v1/transaction/list")
                .content(this.json(filter))
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].amount.value", containsInAnyOrder(10.0, 12.0)))
                .andExpect(jsonPath("$[*].description", containsInAnyOrder("Regular 1", null, null)))
                .andExpect(jsonPath("$", hasSize(3)))
                .andDo(MockMvcResultHandlers.print());

        // Check regular payments.
        getMockMvc().perform(get("/api/v1/transaction/regulars")
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));
    }

    @Test
    public void testRegularWeekendFwd() throws Exception {
        cleanUp();

        // Move date to a saturday.
        LocalDate testDate = applicationProperties.getToday();
        while(testDate.getDayOfWeek() != DayOfWeek.SATURDAY ) {
            testDate = testDate.plusDays(1);
        }

        Optional<Category> category = categoryRepository.findById("FDG");
        if(category.isEmpty()) {
            throw new Exception("Cannot find the category FDG");
        }

        Optional<Account> account = accountRepository.findById("BANK");
        if(account.isEmpty()) {
            throw new Exception("Cannot find the account BANK");
        }

        // Set up a rule that will move the date to after the weekend.
        Regular testRegularPayment = new Regular();
        testRegularPayment.setAccount(account.get());
        testRegularPayment.setCategory(category.get());
        testRegularPayment.setAmount(BigDecimal.valueOf(11.0));
        testRegularPayment.setFrequency("1W");
        testRegularPayment.setStart(testDate);
        testRegularPayment.setWeekendAdj(AdjustmentType.AT_FORWARD);

        regularRepository.save(testRegularPayment);

        regularCtrl.generateRegularPayments(testDate);

        // Move calendar date to the monday, for checking
        testDate = testDate.plusDays(2);

        // Get the transactions using the transaction report.
        TransactionFilterDTO filter = new TransactionFilterDTO();

        AccountDTO accountSearch = new AccountDTO();
        accountSearch.setId("BANK");

        List<AccountDTO> accounts = new ArrayList<>();
        accounts.add(accountSearch);

        filter.setAccounts(accounts);

        CategoryDTO categorySearch = new CategoryDTO();
        categorySearch.setId("FDG");

        List<CategoryDTO> categories = new ArrayList<>();
        categories.add(categorySearch);
        filter.setCategories(categories);

        filter.setPredicted(false);

        transactionReportManager.reset();

        // Check that we have 1 transaction.
        getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].amount.value", is(11.0)))
                .andExpect(jsonPath("$[1].date", startsWith(utilityMapper.map(testDate,String.class))))
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    public void testRegularWeekendBwd() throws Exception {
        cleanUp();

        LocalDate testDate = applicationProperties.getToday();

        // Move date to a saturday.
        while(testDate.getDayOfWeek() != DayOfWeek.SATURDAY) {
            testDate = testDate.plusDays(1);
        }

        Optional<Category> category = categoryRepository.findById("FDG");
        if(category.isEmpty()) {
            throw new Exception("Cannot find the category FDG");
        }

        Optional<Account> account = accountRepository.findById("BANK");
        if(account.isEmpty()) {
            throw new Exception("Cannot find the account BANK");
        }

        // Set up a rule that will move the date to after the weekend.
        Regular testRegularPayment = new Regular();
        testRegularPayment.setAccount(account.get());
        testRegularPayment.setCategory(category.get());
        testRegularPayment.setAmount(BigDecimal.valueOf(10.0));
        testRegularPayment.setFrequency("1W");
        testRegularPayment.setStart(testDate);
        testRegularPayment.setWeekendAdj(AdjustmentType.AT_BACKWARD);

        regularRepository.save(testRegularPayment);

        regularCtrl.generateRegularPayments(testDate);

        // Move calendar date to the friday, for checking
        testDate = testDate.plusDays(-1);

        // Get the transactions using the transaction report.
        TransactionFilterDTO filter = new TransactionFilterDTO();

        AccountDTO accountSearch = new AccountDTO();
        accountSearch.setId("BANK");

        List<AccountDTO> accounts = new ArrayList<>();
        accounts.add(accountSearch);
        filter.setAccounts(accounts);

        CategoryDTO categorySearch = new CategoryDTO();
        categorySearch.setId("FDG");

        List<CategoryDTO> categories = new ArrayList<>();
        categories.add(categorySearch);
        filter.setCategories(categories);

        filter.setPredicted(false);

        transactionReportManager.reset();

        getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("[1].amount.value", is(10.0)))
                .andExpect(jsonPath("[1].date", startsWith(utilityMapper.map(testDate,String.class))))
                .andDo(MockMvcResultHandlers.print());
    }

    private void testReconciliationData(String filename, int expectedCount, double expectedSum, boolean overrideAccount) throws Exception {
        if(overrideAccount) {
            // Override the account on the file
            ReconciliationFileUpdateAccountDTO updateAccount = new ReconciliationFileUpdateAccountDTO();
            updateAccount.setFilename(filename);
            updateAccount.setAccountId("BANK");

            getMockMvc().perform(put("/api/v1/reconciliation/account")
                            .contentType(getContentType())
                            .content(this.json(updateAccount)))
                    .andExpect(status().isOk());
        }

        ReconciliationFileDTO loadFileRequest = new ReconciliationFileDTO();
        loadFileRequest.setFilename (filename);

        getMockMvc().perform(post("/api/v1/reconciliation/load")
                        .contentType(getContentType())
                        .content(this.json(loadFileRequest)))
                .andExpect(status().isOk());

        int actualCount = 0;
        BigDecimal actualSum = BigDecimal.ZERO;
        for(ReconciliationData next : reconciliationRepository.findAll()) {
            actualCount++;
            actualSum = actualSum.add(next.getAmount());
        }

        Assertions.assertEquals(expectedCount,actualCount);
        Assertions.assertEquals(expectedSum,actualSum.doubleValue(),0.001);
    }

    @Test
    public void testLoadReconciliationDataJLP() throws Exception {
        testReconciliationData("test.JLP.csv", 19, -7110.34, true);
    }

    @Test
    public void testLoadReconciliationDataAMEX() throws Exception {
        testReconciliationData("test.AMEX.csv", 15, -132.64, false);
    }

    @Test
    public void testLoadReconciliationDataBARC() throws Exception {
        testReconciliationData("test.BARC.csv", 57, -1142.47, true);
    }

    @Test
    public void testLoadReconciliationDataBank() throws Exception {
        testReconciliationData("test.FirstDirect.csv", 18, -1004.52, false);
    }

    @Test
    public void testStatus() {
        OkStatus okStatus = OkStatus.getOkStatus();
        okStatus.setStatus("Test");
        Assertions.assertEquals("Test", okStatus.getStatus());
    }

    @Test
    public void testHealth() {
        Health health = serviceHealthIndicator.health();
        Assertions.assertEquals(Status.UP, health.getStatus());
    }

    @Test
    public void testHealthFail() {
        CategoryRepository mockRepository = Mockito.mock(CategoryRepository.class);
        when(mockRepository.findAll()).thenReturn(new ArrayList<>());
        ApplicationProperties mockApplicationProperties = Mockito.mock(ApplicationProperties.class);
        when(mockApplicationProperties.getServiceName())
                .thenThrow(IllegalStateException.class)
                .thenReturn("Fred");

        ServiceHealthIndicator healthIndicator = new ServiceHealthIndicator(mockRepository,mockApplicationProperties);
        Health health = healthIndicator.health();
        Assertions.assertEquals(Status.DOWN, health.getStatus());
    }

    @Test
    public void testTransactionString() {
        Assertions.assertEquals("20100522120.32", TransactionString.formattedTransactionString(LocalDate.of(2010,5,22),BigDecimal.valueOf(120.32)));
    }
}
