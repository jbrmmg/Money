package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.*;
import com.jbr.middletier.money.schedule.RegularCtrl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import static java.lang.Math.abs;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
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
    StatementRepository statementRepository;

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
    private
    RegularCtrl regularCtrl;

    private void cleanUp() {
        transactionRepository.deleteAll();
        reconciliationRepository.deleteAll();
        regularRepository.deleteAll();
    }

    @Test
    public void accountTest() throws Exception {
        // Get accounts (external), check that both categories are returned and in the correct order..
        getMockMvc().perform(get("/jbr/ext/money/accounts/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("BANK")))
                .andExpect(jsonPath("$[1].id", is ("AMEX")));

        // Get accounts (internal), check that both categories are returned and in the correct order..
        getMockMvc().perform(get("/jbr/int/money/accounts/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("BANK")))
                .andExpect(jsonPath("$[1].id", is ("AMEX")));
    }

    @Test
    public void categoryTest() throws Exception {
        // Get categories (external), check that all three categories are returned and in the correct order..
        getMockMvc().perform(get("/jbr/ext/money/categories/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("FDG")))
                .andExpect(jsonPath("$[1].id", is ("FDW")))
                .andExpect(jsonPath("$[2].id", is ("FDT")));


        // Get categories (internal), check that all three categories are returned and in the correct order..
        getMockMvc().perform(get("/jbr/int/money/categories/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("FDG")))
                .andExpect(jsonPath("$[1].id", is ("FDW")))
                .andExpect(jsonPath("$[2].id", is ("FDT")));
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

            assertEquals(1280.32, nextTransaction.getAmount(),0.001);
            getMockMvc().perform(put("/jbr/int/money/transaction/update")
                    .content(this.json(updateRequest))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // Delete the transaction.
        transactions = transactionRepository.findAll();
        for(Transaction nextTransaction : transactions) {
            // Delete this item.
            assertEquals(1283.21,nextTransaction.getAmount(),0.001);
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
        assertEquals(1280.32, nextTransaction.getAmount(),0.001);
        UpdateTransaction updateRequest = new UpdateTransaction();
        updateRequest.setId(nextTransaction.getId());
        updateRequest.setAmount(1283.21);

        assertEquals(1280.32, nextTransaction.getAmount(),0.001);
        getMockMvc().perform(put("/jbr/int/money/transaction/update")
                .content(this.json(updateRequest))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Delete the transactions.
        transactions = transactionRepository.findAll();
        for(Transaction nextTransactionToDelete : transactions) {
            // Delete this item.
            assertEquals(1283.21, abs(nextTransactionToDelete.getAmount()),0.001);
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

    // Test Lock Statement
    @Test
    public void testLockStatement() throws Exception {
        cleanUp();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // Do more and check reconciled.

        // Add transaction.
        getMockMvc().perform(post("/jbr/ext/money/transaction/add")
                .content(this.json(new NewTransaction("BANK", "FDG", sdf.parse("1968-05-24"), 1280.32, "AMEX", "Test Transaction")))
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount", is(1280.32)))
                .andExpect(jsonPath("$[1].amount", is(-1280.32)));

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

        // Lock the statement.
        LockStatementRequest lockReqest = new LockStatementRequest();
        lockReqest.setAccountId("BANK");
        lockReqest.setYear(2010);
        lockReqest.setMonth(1);
        getMockMvc().perform(post("/jbr/ext/money/statement/lock")
                .content(this.json(lockReqest))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Expect that there is one AMEX, one Bank locked and one Bank unlocked.
        int amexUnlocked = 0;
        int bankUnlocked = 0;
        int bankLocked = 0;
        int other = 0;
        Iterable<Statement> statements = statementRepository.findAllByOrderByIdAccountAsc();
        for(Statement nextStatement : statements) {
            // Check the statements.
            if(nextStatement.getId().getAccount().getId().equalsIgnoreCase("AMEX")) {
                // AMEX statement, should be unlocked.
                if(!nextStatement.getLocked()) {
                    amexUnlocked++;
                } else {
                    other++;
                }
            } else if(nextStatement.getId().getAccount().getId().equalsIgnoreCase( "BANK")) {
                // Should have one of each
                if(!nextStatement.getLocked()) {
                    bankUnlocked++;

                    assertEquals(1280.32, nextStatement.getOpenBalance(),0.001);
                } else {
                    bankLocked++;

                    assertEquals(0, nextStatement.getOpenBalance(),0.001);
                }
            } else {
                other++;
            }
        }

        assertEquals(1, amexUnlocked);
        assertEquals(1, bankUnlocked);
        assertEquals(1, bankLocked);
        assertEquals(2, other);
    }

    // Test Get Statement
    @Test
    public void testGetStatement() throws Exception {
        cleanUp();

        // Check the url.
        getMockMvc().perform(get("/jbr/ext/money/statement")
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id.account.id", is("AMEX")))
                .andExpect(jsonPath("$[1].id.account.id", is("BANK")))
                .andExpect(jsonPath("$[0].openBalance", is(0.0)))
                .andExpect(jsonPath("$[1].openBalance", is(0.0)));


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

    @Test
    public void testLoadReconcilationDataJLP() throws Exception {
        String path = "src/test/resources";

        File file = new File(path);
        String absolutePath = file.getAbsolutePath();

        getMockMvc().perform(post("/jbr/int/money/reconciliation/load")
                .contentType(getContentType())
                .content("{ \"path\":\"" + absolutePath + "/test.JLP.csv\", \"type\":\"JOHNLEWIS\" }"))
                .andExpect(status().isOk());
    }

    @Test
    public void testLoadReconcilationDataAMEX() throws Exception {
        String path = "src/test/resources";

        File file = new File(path);
        String absolutePath = file.getAbsolutePath();

        getMockMvc().perform(post("/jbr/int/money/reconciliation/load")
                .contentType(getContentType())
                .content("{ \"path\":\"" + absolutePath + "/test.AMEX.csv\", \"type\":\"AMEX\" }"))
                .andExpect(status().isOk());
    }

    @Test
    public void testLoadReconcilationDataBank() throws Exception {
        String path = "src/test/resources";

        File file = new File(path);
        String absolutePath = file.getAbsolutePath();

        getMockMvc().perform(post("/jbr/int/money/reconciliation/load")
                .contentType(getContentType())
                .content("{ \"path\":\"" + absolutePath + "/test.FirstDirect.csv\", \"type\":\"FIRSTDIRECT\" }"))
                .andExpect(status().isOk());
    }
}
