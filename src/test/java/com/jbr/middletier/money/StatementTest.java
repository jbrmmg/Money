package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.primary.Account;
import com.jbr.middletier.money.data.primary.Statement;
import com.jbr.middletier.money.data.primary.StatementId;
import com.jbr.middletier.money.data.primary.Transaction;
import com.jbr.middletier.money.data.primary.repository.AccountRepository;
import com.jbr.middletier.money.data.primary.repository.ReconciliationRepository;
import com.jbr.middletier.money.data.primary.repository.StatementRepository;
import com.jbr.middletier.money.data.primary.repository.TransactionRepository;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.exceptions.StatementAlreadyLockedException;
import com.jbr.middletier.money.schedule.AdjustmentType;
import com.jbr.middletier.money.manager.AccountTransactionManager;
import com.jbr.middletier.money.manager.StatementManager;
import com.jbr.middletier.money.util.FinancialAmount;
import com.jbr.middletier.money.utils.UtilityMapper;
import com.jbr.middletier.money.exceptions.InvalidStatementIdException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
@ActiveProfiles(value="statement")
public class StatementTest extends Support {
    private static final Logger LOG = LoggerFactory.getLogger(StatementTest.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UtilityMapper utilityMapper;

    @Autowired
    private StatementManager statementManager;

    @Autowired
    private AccountTransactionManager accountTransactionManager;

    @Autowired
    private ReconciliationRepository reconciliationRepository;

    private void cleanUp() {
        transactionRepository.deleteAll();
        statementRepository.deleteAll();

        for(Account next : accountRepository.findAll()) {
            Statement statement = new Statement();
            statement.setId(new StatementId(next,2010,1));
            statement.setLocked(false);
            statement.setOpenBalance(BigDecimal.ZERO);

            statementRepository.save(statement);
        }
    }

    // Test Lock Statement
    @Test
    public void testLockStatement() throws Exception {
        cleanUp();

        TransactionDTO transaction1 = new TransactionDTO();
        transaction1.setAccountId("BANK");
        transaction1.setDate(utilityMapper.map(LocalDate.of(1968,5,24),String.class));
        transaction1.setAmount(BigDecimal.valueOf(1280.32));

        TransactionDTO transaction2 = new TransactionDTO();
        transaction2.setAccountId("AMEX");

        // Add transaction.
        getMockMvc().perform(post("/api/v1/transaction")
                        .content(this.json(Arrays.asList(transaction1,transaction2)))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].amount", containsInAnyOrder(1280.32, -1280.32)))
                .andExpect(jsonPath("$[*].categoryId", containsInAnyOrder("TRF", "TRF")));

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

        // Lock the statement.
        StatementIdDTO statementId = new StatementIdDTO("BANK",1,2010);
        getMockMvc().perform(post("/api/v1/statement/lock")
                        .content(this.json(statementId))
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

                    assertEquals(1280.32, nextStatement.getOpenBalance().getValue().doubleValue(),0.001);
                } else {
                    bankLocked++;

                    assertEquals(0, nextStatement.getOpenBalance().getValue().doubleValue(),0.001);
                }
            } else {
                other++;
            }
        }

        assertEquals(1, amexUnlocked);
        assertEquals(1, bankUnlocked);
        assertEquals(1, bankLocked);
        assertEquals(2, other);

        // Check it cannot be locked again.
        String error = Objects.requireNonNull(getMockMvc().perform(post("/api/v1/statement/lock")
                        .content(this.json(statementId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("Statement already locked BANK201001", error);

        // Delete the statement.
        StatementDTO statement = new StatementDTO();
        statement.setAccountId("BANK");
        statement.setMonth(1);
        statement.setYear(2010);
        statement.setOpenBalance(new FinancialAmount());

        error = Objects.requireNonNull(getMockMvc().perform(delete("/api/v1/statement")
                        .content(this.json(statement))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("Cannot delete locked statement BANK 1 2010", error);

        statement.setMonth(2);
        getMockMvc().perform(delete("/api/v1/statement")
                        .content(this.json(statement))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        // JBR-439: check that the previous statement is now unlocked.
    }

    private void incrementLock(StatementIdDTO lockRequest) {
        lockRequest.setMonth(lockRequest.getMonth() + 1);
        if(lockRequest.getMonth() > 12) {
            lockRequest.setMonth(1);
            lockRequest.setYear(lockRequest.getYear() + 1);
        }
    }

    @Test
    public void testStatementAge() throws InvalidStatementIdException, StatementAlreadyLockedException {
        cleanUp();

        // Create statements to test with.
        StatementIdDTO lockRequest = new StatementIdDTO("NWDE",1,2010);
        statementRepository.findAll().forEach(statement -> {
            LOG.info("Statement {} {} {} {}", statement.getId().getAccount().getId(), statement.getId().getYear(), statement.getId().getMonth(), statement.getLocked());

            if(!statement.getLocked() && statement.getId().getAccount().getId().equalsIgnoreCase(lockRequest.getAccountId())) {
                lockRequest.setMonth(statement.getId().getMonth());
                lockRequest.setYear(statement.getId().getYear());
            }
        });

        // Store the expected age of each year, month.
        Map<String,Integer> expectedAges = new HashMap<>();

        statementManager.statementLock(lockRequest, accountTransactionManager);
        expectedAges.put(lockRequest.getYear().toString() + "-" + lockRequest.getMonth().toString(),5);
        incrementLock(lockRequest);
        statementManager.statementLock(lockRequest, accountTransactionManager);
        expectedAges.put(lockRequest.getYear().toString() + "-" + lockRequest.getMonth().toString(),4);
        incrementLock(lockRequest);
        statementManager.statementLock(lockRequest, accountTransactionManager);
        expectedAges.put(lockRequest.getYear().toString() + "-" + lockRequest.getMonth().toString(),3);
        incrementLock(lockRequest);
        statementManager.statementLock(lockRequest, accountTransactionManager);
        expectedAges.put(lockRequest.getYear().toString() + "-" + lockRequest.getMonth().toString(),2);
        incrementLock(lockRequest);
        statementManager.statementLock(lockRequest, accountTransactionManager);
        expectedAges.put(lockRequest.getYear().toString() + "-" + lockRequest.getMonth().toString(),1);

        // Check that the statements have been aged.
        statementRepository.findAll().forEach(statement -> {
            LOG.info("Statement {} {} {} {} {}", statement.getId().getAccount().getId(), statement.getId().getYear(), statement.getId().getMonth(), statement.getLocked(), statement.getAge());

            if(statement.getId().getAccount().getId().equalsIgnoreCase(lockRequest.getAccountId())) {
                Integer expectedAge = expectedAges.get(statement.getId().getYear().toString() + "-" + statement.getId().getMonth().toString());
                if(statement.getAge() != null && statement.getAge() > 0) {
                    Assertions.assertEquals(expectedAge, statement.getAge());
                }
            }
        });
    }

    @Test
    public void testGetStatement() throws Exception {
        cleanUp();

        // Check the url.
        getMockMvc().perform(get("/api/v1/statement")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountId", is("AMEX")))
                .andExpect(jsonPath("$[1].accountId", is("BANK")))
                .andExpect(jsonPath("$[0].openBalance.value", is(0.0)))
                .andExpect(jsonPath("$[1].openBalance.value", is(0.0)))
                .andDo(MockMvcResultHandlers.print());

        // Check the url.
        getMockMvc().perform(get("/api/v1/statement")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountId", is("AMEX")))
                .andExpect(jsonPath("$[1].accountId", is("BANK")))
                .andExpect(jsonPath("$[0].openBalance.value", is(0.0)))
                .andExpect(jsonPath("$[1].openBalance.value", is(0.0)));
    }

    @Test
    public void testLockInvalidAccount() throws Exception {
        cleanUp();

        StatementIdDTO statementId = new StatementIdDTO("FLIP",1,2020);
        statementId.setAccountId("FLIP");
        statementId.setYear(2020);
        statementId.setMonth(1);
        String error = Objects.requireNonNull(getMockMvc().perform(post("/api/v1/statement/lock")
                        .content(this.json(statementId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("Cannot find statement with id FLIP202001", error);
    }

    @Test
    public void testLockInvalidStatementId() throws Exception {
        cleanUp();

        StatementIdDTO statementId = new StatementIdDTO("BANK",1,2012);
        String error = Objects.requireNonNull(getMockMvc().perform(post("/api/v1/statement/lock")
                        .content(this.json(statementId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("Cannot find statement with id BANK201201", error);
    }

    @Test
    public void testDeleteInvalidId() throws Exception {
        cleanUp();

        StatementDTO statement = new StatementDTO();
        statement.setAccountId("FLIP");
        statement.setMonth(1);
        statement.setYear(2020);
        String error = Objects.requireNonNull(getMockMvc().perform(delete("/api/v1/statement")
                        .content(this.json(statement))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("Cannot find account with id FLIP", error);

        statement.setAccountId("BANK");
        error = Objects.requireNonNull(getMockMvc().perform(delete("/api/v1/statement")
                        .content(this.json(statement))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("Cannot delete last statement BANK 1 2020", error);
    }

    @Test
    public void testStatementAlreadyExists() throws Exception {
        cleanUp();

        StatementDTO statement = new StatementDTO();
        statement.setAccountId("AMEX");
        statement.setMonth(1);
        statement.setYear(2010);
        String error = Objects.requireNonNull(getMockMvc().perform(post("/api/v1/statement")
                        .content(this.json(statement))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("Statement already exists - AMEX 1 2010", error);
    }

    @Test
    public void testCreateStatement() throws Exception {
        cleanUp();

        statementRepository.deleteAll();

        StatementDTO statement = new StatementDTO();
        statement.setAccountId("AMEX");
        statement.setMonth(4);
        statement.setYear(2010);
        statement.setLocked(false);
        statement.setOpenBalance(new FinancialAmount(BigDecimal.valueOf(1023.9)));
        getMockMvc().perform(post("/api/v1/statement")
                        .content(this.json(statement))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        for(Statement next : statementRepository.findAll()) {
            if(next.getId().getMonth() == 4) {
                statementRepository.delete(next);
            }
        }
    }

    @Test
    public void testException() {
        StatementDTO statement = new StatementDTO();
        statement.setAccountId("AMEX");
        statement.setMonth(1);
        statement.setYear(2010);
        statement.setLocked(false);
        statement.setOpenBalance(new FinancialAmount(BigDecimal.valueOf(100)));

        InvalidStatementIdException test = new InvalidStatementIdException(statement);
        Assertions.assertEquals("Cannot find statement with id AMEX 1 2010", test.getMessage());
    }

    @Test
    public void testAutoTransfer() throws Exception {
        cleanUp();

        // Set the transfer details on AMEX — transferAccountId=BANK, transferDay=15.
        AccountDTO amexAccount = new AccountDTO();
        amexAccount.setId("AMEX");
        amexAccount.setName("American Express");
        amexAccount.setImagePrefix("amex");
        amexAccount.setColour("4BBAEA");
        amexAccount.setTransferAccountId("BANK");
        amexAccount.setTransferDay(15);

        getMockMvc().perform(put("/api/v1/accounts")
                        .content(this.json(amexAccount))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Create a transaction on AMEX (Jan 24, 2010) — payment date should be Feb 15, 2010.
        TransactionDTO amexTransaction = new TransactionDTO();
        amexTransaction.setAccountId("AMEX");
        amexTransaction.setDate(utilityMapper.map(LocalDate.of(2010, 1, 24), String.class));
        amexTransaction.setAmount(BigDecimal.valueOf(-500.00));
        amexTransaction.setDescription("Test charge");
        amexTransaction.setCategoryId("FDG");

        getMockMvc().perform(post("/api/v1/transaction")
                        .content(this.json(List.of(amexTransaction)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Reconcile the transaction to the AMEX Jan 2010 statement.
        for (Transaction next : transactionRepository.findAll()) {
            ReconcileTransactionDTO req = new ReconcileTransactionDTO();
            req.getTransactions().add(next.getId());
            req.setReconcile(true);
            getMockMvc().perform(put("/api/v1/reconcile")
                            .content(this.json(req))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // Lock the AMEX statement.
        StatementIdDTO statementId = new StatementIdDTO("AMEX", 1, 2010);
        getMockMvc().perform(post("/api/v1/statement/lock")
                        .content(this.json(statementId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify two transfer transactions were created (BANK and AMEX).
        List<Transaction> allTransactions = new ArrayList<>();
        transactionRepository.findAll().forEach(allTransactions::add);

        // Original charge + two auto-transfer legs = 3 transactions.
        assertEquals(3, allTransactions.size());

        // The transfer pair: one BANK leg (amount = -500) and one AMEX leg (amount = +500).
        boolean foundBankTransfer = false;
        boolean foundAmexTransfer = false;
        for (Transaction t : allTransactions) {
            if (t.getAccount().getId().equals("BANK")
                    && t.getAmount().getValue().compareTo(BigDecimal.valueOf(-500.00)) == 0
                    && t.getDate().equals(LocalDate.of(2010, 2, 15))
                    && t.getDescription().startsWith("Automatic transfer ")) {
                foundBankTransfer = true;
            }
            if (t.getAccount().getId().equals("AMEX")
                    && t.getAmount().getValue().compareTo(BigDecimal.valueOf(500.00)) == 0
                    && t.getDate().equals(LocalDate.of(2010, 2, 15))
                    && t.getDescription().startsWith("Automatic transfer ")) {
                foundAmexTransfer = true;
            }
        }
        Assertions.assertTrue(foundBankTransfer, "Expected BANK auto-transfer transaction not found");
        Assertions.assertTrue(foundAmexTransfer, "Expected AMEX auto-transfer transaction not found");

        // Verify reconciliation data was cleared.
        Assertions.assertFalse(reconciliationRepository.findAll().iterator().hasNext(),
                "Reconciliation data should have been cleared on lock");
    }

    @Test
    public void testAutoTransferSkippedWhenNoTransferAccount() throws Exception {
        cleanUp();

        // NWDE has no transfer account set — lock should complete but no transfer created.
        TransactionDTO nwdeTransaction = new TransactionDTO();
        nwdeTransaction.setAccountId("NWDE");
        nwdeTransaction.setDate(utilityMapper.map(LocalDate.of(2010, 1, 10), String.class));
        nwdeTransaction.setAmount(BigDecimal.valueOf(200.00));
        nwdeTransaction.setDescription("Test credit");
        nwdeTransaction.setCategoryId("FDG");

        getMockMvc().perform(post("/api/v1/transaction")
                        .content(this.json(List.of(nwdeTransaction)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        for (Transaction next : transactionRepository.findAll()) {
            if (next.getAccount().getId().equals("NWDE")) {
                ReconcileTransactionDTO req = new ReconcileTransactionDTO();
                req.getTransactions().add(next.getId());
                req.setReconcile(true);
                getMockMvc().perform(put("/api/v1/reconcile")
                                .content(this.json(req))
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk());
            }
        }

        StatementIdDTO statementId = new StatementIdDTO("NWDE", 1, 2010);
        getMockMvc().perform(post("/api/v1/statement/lock")
                        .content(this.json(statementId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Only the original transaction — no auto-transfer created.
        List<Transaction> allTransactions = new ArrayList<>();
        transactionRepository.findAll().forEach(allTransactions::add);
        assertEquals(1, allTransactions.size());
    }

    @Test
    public void testAutoTransferWithWeekendAdjustment() throws Exception {
        cleanUp();

        // transferDay=6, Jan 2010 statement → payment date = Feb 6, 2010 = Saturday.
        // With AT_FORWARD adjustment this should move to Monday Feb 8, 2010.
        AccountDTO amexAccount = new AccountDTO();
        amexAccount.setId("AMEX");
        amexAccount.setName("American Express");
        amexAccount.setImagePrefix("amex");
        amexAccount.setColour("4BBAEA");
        amexAccount.setTransferAccountId("BANK");
        amexAccount.setTransferDay(6);
        amexAccount.setWeekendAdj(AdjustmentType.AT_FORWARD);

        getMockMvc().perform(put("/api/v1/accounts")
                        .content(this.json(amexAccount))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        TransactionDTO amexTransaction = new TransactionDTO();
        amexTransaction.setAccountId("AMEX");
        amexTransaction.setDate(utilityMapper.map(LocalDate.of(2010, 1, 20), String.class));
        amexTransaction.setAmount(BigDecimal.valueOf(-300.00));
        amexTransaction.setDescription("Test charge");
        amexTransaction.setCategoryId("FDG");

        getMockMvc().perform(post("/api/v1/transaction")
                        .content(this.json(List.of(amexTransaction)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        for (Transaction next : transactionRepository.findAll()) {
            ReconcileTransactionDTO req = new ReconcileTransactionDTO();
            req.getTransactions().add(next.getId());
            req.setReconcile(true);
            getMockMvc().perform(put("/api/v1/reconcile")
                            .content(this.json(req))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        StatementIdDTO statementId = new StatementIdDTO("AMEX", 1, 2010);
        getMockMvc().perform(post("/api/v1/statement/lock")
                        .content(this.json(statementId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Feb 6, 2010 = Saturday → adjusted forward to Feb 8, 2010 (Monday).
        List<Transaction> allTransactions = new ArrayList<>();
        transactionRepository.findAll().forEach(allTransactions::add);
        assertEquals(3, allTransactions.size());

        boolean foundTransfer = allTransactions.stream().anyMatch(t ->
                t.getAccount().getId().equals("BANK")
                        && t.getDate().equals(LocalDate.of(2010, 2, 8))
                        && t.getDescription().startsWith("Automatic transfer "));
        Assertions.assertTrue(foundTransfer, "Transfer on Feb 8 2010 (adjusted from Sat Feb 6) not found");
    }
}
