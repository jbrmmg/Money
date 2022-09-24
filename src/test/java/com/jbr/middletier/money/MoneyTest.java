package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.config.DefaultProfileUtil;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.*;
import com.jbr.middletier.money.health.ServiceHealthIndicator;
import com.jbr.middletier.money.schedule.RegularCtrl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import static java.lang.Math.abs;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Created by jason on 27/03/17.
 */

@RunWith(SpringRunner.class)
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

    private void cleanUp() {
        transactionRepository.deleteAll();
        reconciliationRepository.deleteAll();
        regularRepository.deleteAll();
        for(Statement next: statementRepository.findAll()) {
            if(next.getId().getMonth() != 1) {
                statementRepository.delete(next);
            } else if(next.getLocked()) {
                next.setLocked(false);
                statementRepository.save(next);
            }
        }
    }

    @Test
    public void TestDefaultProfile() {
        SpringApplication app = mock(SpringApplication.class);

        Assert.assertNotNull(app);
        DefaultProfileUtil.addDefaultProfile(app);
    }

    @Test
    public void internalTransactionTest() throws Exception {
        cleanUp();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        getMockMvc().perform(post("/jbr/int/money/transaction/add")
                .content(this.json(new NewTransaction("BANK", "FDW", sdf.parse("1968-05-24"), 1280.32, "Test transaction")))
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount",is(1280.32)))
                .andExpect(jsonPath("$[0].description",is("Test transaction")));

        // Amend the transaction.
        Iterable<Transaction> transactions = transactionRepository.findAll();
        for(Transaction nextTransaction : transactions) {
            UpdateTransaction updateRequest = new UpdateTransaction();
            updateRequest.setId(nextTransaction.getId());
            updateRequest.setAmount(1283.21);

            assertEquals(1280.32, nextTransaction.getAmount().getValue(),0.001);
            getMockMvc().perform(put("/jbr/int/money/transaction/update")
                    .content(this.json(updateRequest))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // Delete the transaction.
        transactions = transactionRepository.findAll();
        for(Transaction nextTransaction : transactions) {
            // Delete this item.
            assertEquals(1283.21,nextTransaction.getAmount().getValue(),0.001);
            getMockMvc().perform(delete("/jbr/int/money/delete?transactionId=" + nextTransaction.getId()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void externalTranasactionTest() throws Exception {
        cleanUp();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // Add transaction.
        getMockMvc().perform(post("/jbr/ext/money/transaction/add")
                .content(this.json(new NewTransaction("BANK", "FDG", sdf.parse("1968-05-24"), 1280.32, "AMEX", "Test Transaction")))
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount", is(1280.32)))
                .andExpect(jsonPath("$[0].description", is("Test Transaction")))
                .andExpect(jsonPath("$[1].amount", is(-1280.32)))
                .andExpect(jsonPath("$[1].description", is("Test Transaction")));

        // Edit the transactions (by editing the first transaction).
        Iterable<Transaction> transactions = transactionRepository.findAll();

        Transaction nextTransaction = transactions.iterator().next();
        assertEquals(1280.32, nextTransaction.getAmount().getValue(),0.001);
        UpdateTransaction updateRequest = new UpdateTransaction();
        updateRequest.setId(nextTransaction.getId());
        updateRequest.setAmount(1283.21);

        assertEquals(1280.32, nextTransaction.getAmount().getValue(),0.001);
        getMockMvc().perform(put("/jbr/int/money/transaction/update")
                .content(this.json(updateRequest))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Delete the transactions.
        transactions = transactionRepository.findAll();
        for(Transaction nextTransactionToDelete : transactions) {
            // Delete this item.
            assertEquals(1283.21, abs(nextTransactionToDelete.getAmount().getValue()),0.001);
            getMockMvc().perform(delete("/jbr/ext/money/delete?transactionId=" + nextTransactionToDelete.getId()))
                    .andExpect(status().isOk());
        }
    }

    // Test Reconcile / Un-reconcile transaction
    @Test
    public void reconcileTransaction() throws Exception {
        cleanUp();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // Setup a transaction.
        getMockMvc().perform(post("/jbr/ext/money/transaction/add")
                .content(this.json(new NewTransaction("AMEX", "FDG", sdf.parse("1968-05-25"), 1281.32, "BANK", "Test Transaction")))
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount", is(1281.32)))
                .andExpect(jsonPath("$[1].amount", is(-1281.32)));

        // Reconcile the transaction..
        Iterable<Transaction> transactions = transactionRepository.findAll();
        for(Transaction nextTransaction : transactions) {
            assertFalse(nextTransaction.reconciled());
            ReconcileTransaction reconcileRequest = new ReconcileTransaction();
            reconcileRequest.setId(nextTransaction.getId());
            reconcileRequest.setReconcile(true);
            getMockMvc().perform(put("/jbr/ext/money/reconcile")
                    .content(this.json(reconcileRequest))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // Un-reconcile the transaction..
        transactions = transactionRepository.findAll();
        for(Transaction nextTransaction : transactions) {
            assertTrue(nextTransaction.reconciled());
            ReconcileTransaction reconcileRequest = new ReconcileTransaction();
            reconcileRequest.setId(nextTransaction.getId());
            reconcileRequest.setReconcile(false);
            getMockMvc().perform(put("/jbr/ext/money/reconcile")
                    .content(this.json(reconcileRequest))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // Delete the transactions.
        transactions = transactionRepository.findAll();
        for(Transaction nextTransaction : transactions) {
            // Delete this item.
            getMockMvc().perform(delete("/jbr/ext/money/delete?transactionId=" + nextTransaction.getId()))
                    .andExpect(status().isOk());
        }
    }

    // Test load reconciliation data.
    @Test
    public void testLoadReconciationData() throws Exception {
        cleanUp();

        // Check reconciliation data load.
        getMockMvc().perform(post("/jbr/int/money/reconciliation/add")
                .content("12/12/2015,21.1\n12/12/2015,21.31"))
                .andExpect(status().isOk());
    }

    // Test Get Transactions
    @Test
    public void testGetTransaction() throws Exception {
        cleanUp();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // Create transactions in each account.
        getMockMvc().perform(post("/jbr/int/money/transaction/add")
                .content(this.json(new NewTransaction("BANK", "FDG", sdf.parse("1968-05-24"), 1.23, "Test Transaction")))
                .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/jbr/int/money/transaction/add")
                .content(this.json(new NewTransaction("JLPC", "FDG", sdf.parse("1968-05-25"), 3.45, "Test Transaction")))
                .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(get("/jbr/ext/money/transaction/get?type=UN&account=BANK")
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount", is(1.23)))
                .andExpect(jsonPath("$", hasSize(1)));

        getMockMvc().perform(get("/jbr/ext/money/transaction/get?type=UN&account=JLPC")
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount", is(3.45)))
                .andExpect(jsonPath("$", hasSize(1)));

        // Create another transaction
        getMockMvc().perform(post("/jbr/int/money/transaction/add")
                .content(this.json(new NewTransaction("BANK", "UTT", sdf.parse("1968-05-26"), 1.53, "Test Transaction")))
                .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(get("/jbr/ext/money/transaction/get?type=UN&account=BANK")
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount", is(1.23)))
                .andExpect(jsonPath("$[1].amount", is(1.53)))
                .andExpect(jsonPath("$", hasSize(2)));

        getMockMvc().perform(get("/jbr/ext/money/transaction/get?type=UN&account=BANK&category=FDG")
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount", is(1.23)))
                .andExpect(jsonPath("$", hasSize(1)));


        getMockMvc().perform(get("/jbr/ext/money/transaction/get?type=UL&account=BANK,JLPC")
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount", is(1.23)))
                .andExpect(jsonPath("$[1].amount", is(3.45)))
                .andExpect(jsonPath("$[2].amount", is(1.53)))
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    // Test match - multiple exact match.
    public void testMatch1() throws Exception {
        cleanUp();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        getMockMvc().perform(post("/jbr/int/money/transaction/add")
                .content(this.json(new NewTransaction("AMEX", "FDG", sdf.parse("1968-05-24"), 1.23, "Test Transaction")))
                .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/jbr/int/money/transaction/add")
                .content(this.json(new NewTransaction("AMEX", "FDG", sdf.parse("1968-05-24"), 1.23, "Test Transaction")))
                .contentType(getContentType()))
                .andExpect(status().isOk());

        // Reconcile the transaction..
        Iterable<Transaction> transactions = transactionRepository.findAll();
        for(Transaction nextTransaction : transactions) {
            assertFalse(nextTransaction.reconciled());
            ReconcileTransaction reconcileRequest = new ReconcileTransaction();
            reconcileRequest.setId(nextTransaction.getId());
            reconcileRequest.setReconcile(true);
            getMockMvc().perform(put("/jbr/ext/money/reconcile")
                    .content(this.json(reconcileRequest))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // Load recon data.
        getMockMvc().perform(post("/jbr/int/money/reconciliation/add")
                .content("24/05/1968,1.23,FDG,What are we saying\n24/05/1968,1.23\n24/05/1968,1.23"))
                .andExpect(status().isOk());

        // Match - should be 2 exact match and 1 not matched.
        getMockMvc().perform(get("/jbr/int/money/match?account=AMEX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[2].amount", is(1.23)))
                .andExpect(jsonPath("$[2].forwardAction", is("NONE")))
                .andExpect(jsonPath("$[2].category.id", is("FDG")))
                .andExpect(jsonPath("$[1].amount", is(1.23)))
                .andExpect(jsonPath("$[1].description", is("What are we saying")))
                .andExpect(jsonPath("$[1].forwardAction", is("NONE")))
                .andExpect(jsonPath("$[0].amount", is(1.23)))
                .andExpect(jsonPath("$[0].forwardAction", is("SETCATEGORY")));
    }

    // Test match wrong dates - cause repeat.
    @Test
    public void testMatch2() throws Exception {
        cleanUp();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // Load transactions.
        getMockMvc().perform(post("/jbr/int/money/transaction/add")
                .content(this.json(new NewTransaction("AMEX", "FDG", sdf.parse("1968-05-24"), 1.23,"Test Transaction")))
                .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/jbr/int/money/transaction/add")
                .content(this.json(new NewTransaction("AMEX", "FDG", sdf.parse("1968-06-27"), 1.23,"Test Transaction")))
                .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/jbr/int/money/transaction/add")
                .content(this.json(new NewTransaction("AMEX", "FDG", sdf.parse("1968-06-26"), 1.23,"Test Transaction")))
                .contentType(getContentType()))
                .andExpect(status().isOk());

        // Reconcile the transaction..
        Iterable<Transaction> transactions = transactionRepository.findAll();
        for(Transaction nextTransaction : transactions) {
            assertFalse(nextTransaction.reconciled());
            ReconcileTransaction reconcileRequest = new ReconcileTransaction();
            reconcileRequest.setId(nextTransaction.getId());
            reconcileRequest.setReconcile(true);
            getMockMvc().perform(put("/jbr/ext/money/reconcile")
                    .content(this.json(reconcileRequest))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // Load recon data.
        getMockMvc().perform(post("/jbr/int/money/reconciliation/add")
                .content("24/05/1968,1.23\n24/06/1968,1.23\n24/06/1968,1.23"))
                .andExpect(status().isOk());

        // Match - should be 2 exact match and 1 not matched.
        getMockMvc().perform(get("/jbr/int/money/match?account=AMEX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].amount", is(1.23)))
                .andExpect(jsonPath("$[0].forwardAction", is("NONE")))
                .andExpect(jsonPath("$[1].amount", is(1.23)))
                .andExpect(jsonPath("$[1].forwardAction", is("NONE")))
                .andExpect(jsonPath("$[2].amount", is(1.23)))
                .andExpect(jsonPath("$[2].forwardAction", is("NONE")));
    }

    @Test
    public void testRegular() throws Exception {
        cleanUp();

        Calendar calendar = Calendar.getInstance();
        Date today = new Date();

        Optional<Category> category = categoryRepository.findById("FDG");
        if(!category.isPresent()) {
            throw new Exception("Cannot find the category FDG");
        }

        Optional<Account> account = accountRepository.findById("BANK");
        if(!account.isPresent()) {
            throw new Exception("Cannot find the account BANK");
        }

        // Create a payment that starts today - should be created immediately.
        Regular testRegularPayment = new Regular();
        testRegularPayment.setAccount(account.get());
        testRegularPayment.setCategory(category.get());
        testRegularPayment.setAmount(10.0);
        testRegularPayment.setFrequency("1W");
        testRegularPayment.setStart(today);
        testRegularPayment.setWeekendAdj("NO");
        testRegularPayment.setDescription("Regular 1");

        regularRepository.save(testRegularPayment);

        // Create a payment, for 1 week that starts yesterday - should not create anything.
        calendar.setTime(today);
        calendar.add(Calendar.DATE, -1);

        testRegularPayment = new Regular();
        testRegularPayment.setAccount(account.get());
        testRegularPayment.setCategory(category.get());
        testRegularPayment.setAmount(11.0);
        testRegularPayment.setFrequency("1W");
        testRegularPayment.setStart(calendar.getTime());
        testRegularPayment.setWeekendAdj("NO");

        regularRepository.save(testRegularPayment);

        // Create a payment, for 1 week that starts 1 week ago - should create a new payment today.
        calendar.setTime(today);
        calendar.add(Calendar.DATE, -7);

        testRegularPayment = new Regular();
        testRegularPayment.setAccount(account.get());
        testRegularPayment.setCategory(category.get());
        testRegularPayment.setAmount(12.0);
        testRegularPayment.setFrequency("1W");
        testRegularPayment.setStart(calendar.getTime());
        testRegularPayment.setLastDate(calendar.getTime());
        testRegularPayment.setWeekendAdj("NO");

        regularRepository.save(testRegularPayment);

        // Create a payment, invalid - should not create anything.
        calendar.setTime(today);
        calendar.add(Calendar.DATE, -7);

        testRegularPayment = new Regular();
        testRegularPayment.setAccount(account.get());
        testRegularPayment.setCategory(category.get());
        testRegularPayment.setAmount(13.0);
        testRegularPayment.setFrequency("1X");
        testRegularPayment.setStart(calendar.getTime());
        testRegularPayment.setLastDate(calendar.getTime());
        testRegularPayment.setWeekendAdj("NO");

        regularRepository.save(testRegularPayment);

        testRegularPayment = new Regular();
        testRegularPayment.setAccount(account.get());
        testRegularPayment.setCategory(category.get());
        testRegularPayment.setAmount(14.0);
        testRegularPayment.setFrequency("1M");
        testRegularPayment.setStart(calendar.getTime());
        testRegularPayment.setLastDate(calendar.getTime());
        testRegularPayment.setWeekendAdj("NO");

        regularRepository.save(testRegularPayment);

        testRegularPayment = new Regular();
        testRegularPayment.setAccount(account.get());
        testRegularPayment.setCategory(category.get());
        testRegularPayment.setAmount(15.0);
        testRegularPayment.setFrequency("1Y");
        testRegularPayment.setStart(calendar.getTime());
        testRegularPayment.setLastDate(calendar.getTime());
        testRegularPayment.setWeekendAdj("NO");

        regularRepository.save(testRegularPayment);

        // Process regular payments - do it twice, second should do nothing.
        regularCtrl.generateRegularPayments();
        regularCtrl.generateRegularPayments();

        // Check that we have 1 transaction.
        getMockMvc().perform(get("/jbr/ext/money/transaction/get?type=UN&account=BANK&category=FDG")
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount", is(10.0)))
                .andExpect(jsonPath("$[0].description", is("Regular 1")))
                .andExpect(jsonPath("$[1].amount", is(12.0)))
                .andExpect(jsonPath("$", hasSize(2)));


        // Check regular payments.
        getMockMvc().perform(get("/jbr/ext/money/transaction/regulars")
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));
    }

    @Test
    public void testRegularWeekendFwd() throws Exception {
        cleanUp();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        // Move date to a saturday.
        while(calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            calendar.add(Calendar.DATE,1);
        }

        Optional<Category> category = categoryRepository.findById("FDG");
        if(!category.isPresent()) {
            throw new Exception("Cannot find the category FDG");
        }

        Optional<Account> account = accountRepository.findById("BANK");
        if(!account.isPresent()) {
            throw new Exception("Cannot find the account BANK");
        }

        // Setup a rule that will move the date to after the weekend.
        Regular testRegularPayment = new Regular();
        testRegularPayment.setAccount(account.get());
        testRegularPayment.setCategory(category.get());
        testRegularPayment.setAmount(10.0);
        testRegularPayment.setFrequency("1W");
        testRegularPayment.setStart(calendar.getTime());
        testRegularPayment.setWeekendAdj("FW");

        regularRepository.save(testRegularPayment);

        regularCtrl.generateRegularPayments(calendar.getTime());

        // Move calendar date to the monday, for checking
        calendar.add(Calendar.DATE,2);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // Check that we have 1 transaction.
        getMockMvc().perform(get("/jbr/ext/money/transaction/get?type=UN&account=BANK&category=FDG")
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount", is(10.0)))
                .andExpect(jsonPath("$[0].date", startsWith(sdf.format(calendar.getTime()))))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    public void testRegularWeekendBwd() throws Exception {
        cleanUp();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        // Move date to a saturday.
        while(calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            calendar.add(Calendar.DATE,1);
        }

        Optional<Category> category = categoryRepository.findById("FDG");
        if(!category.isPresent()) {
            throw new Exception("Cannot find the category FDG");
        }

        Optional<Account> account = accountRepository.findById("BANK");
        if(!account.isPresent()) {
            throw new Exception("Cannot find the account BANK");
        }

        // Setup a rule that will move the date to after the weekend.
        Regular testRegularPayment = new Regular();
        testRegularPayment.setAccount(account.get());
        testRegularPayment.setCategory(category.get());
        testRegularPayment.setAmount(10.0);
        testRegularPayment.setFrequency("1W");
        testRegularPayment.setStart(calendar.getTime());
        testRegularPayment.setWeekendAdj("BW");

        regularRepository.save(testRegularPayment);

        regularCtrl.generateRegularPayments(calendar.getTime());

        // Move calendar date to the friday, for checking
        calendar.add(Calendar.DATE,-1);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // Check that we have 1 transaction.
        getMockMvc().perform(get("/jbr/ext/money/transaction/get?type=UN&account=BANK&category=FDG")
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount", is(10.0)))
                .andExpect(jsonPath("$[0].date", startsWith(sdf.format(calendar.getTime()))))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    private void testReconciliationData(String filename, String type) throws Exception {
        String path = "src/test/resources";

        File file = new File(path);
        String absolutePath = file.getAbsolutePath();

        LoadFileRequest loadFileRequest = new LoadFileRequest();
        loadFileRequest.setPath(absolutePath + "/" + filename);
        loadFileRequest.setType(type);

        getMockMvc().perform(post("/jbr/int/money/reconciliation/load")
                        .contentType(getContentType())
                        .content(this.json(loadFileRequest)))
                .andExpect(status().isOk());

        //TODO check this load worked.
    }

    @Test
    public void testLoadReconcilationDataJLP() throws Exception {
        testReconciliationData("test.JLP.csv","JOHNLEWIS");
    }

    @Test
    public void testLoadReconcilationDataAMEX() throws Exception {
        testReconciliationData("test.AMEX.csv","AMEX");
    }

    @Test
    public void testLoadReconcilationDataBank() throws Exception {
        testReconciliationData("test.FirstDirect.csv","FIRSTDIRECT");
    }

    @Test
    public void testStatus() {
        OkStatus okStatus = OkStatus.getOkStatus();
        okStatus.setStatus("Test");
        Assert.assertEquals("Test", okStatus.getStatus());
    }

    @Test
    public void testHealth() {
        Health health = serviceHealthIndicator.health();
        Assert.assertEquals(Status.UP, health.getStatus());
    }

    @Test
    public void testHealthFail() {
        CategoryRepository mockReposity = Mockito.mock(CategoryRepository.class);
        when(mockReposity.findAll()).thenReturn(new ArrayList<>());
        ApplicationProperties applicationProperties = Mockito.mock(ApplicationProperties.class);
        when(applicationProperties.getServiceName())
                .thenThrow(IllegalStateException.class)
                .thenReturn("Fred");

        ServiceHealthIndicator healthIndicator = new ServiceHealthIndicator(mockReposity,applicationProperties);
        Health health = healthIndicator.health();
        Assert.assertEquals(Status.DOWN, health.getStatus());
    }
}
