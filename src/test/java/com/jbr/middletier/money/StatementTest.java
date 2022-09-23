package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.StatementDTO;
import com.jbr.middletier.money.dto.StatementIdDTO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.text.SimpleDateFormat;
import java.util.Objects;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
@ActiveProfiles(value="statement")
public class StatementTest extends Support {
    @Autowired
    private
    TransactionRepository transactionRepository;

    @Autowired
    private
    StatementRepository statementRepository;

    private void cleanUp() {
        transactionRepository.deleteAll();
        for(Statement next : statementRepository.findAll()) {
            if(next.getId().getMonth() != 1) {
                statementRepository.delete(next);
            } else if(next.getLocked()) {
                next.setLocked(false);
                statementRepository.save(next);
            }
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

                    assertEquals(1280.32, nextStatement.getOpenBalance().getValue(),0.001);
                } else {
                    bankLocked++;

                    assertEquals(0, nextStatement.getOpenBalance().getValue(),0.001);
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
        String error = Objects.requireNonNull(getMockMvc().perform(post("/jbr/ext/money/statement/lock")
                        .content(this.json(lockReqest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Statement already locked BANK 2010 1", error);

        // Delete the statement.
        AccountDTO account = new AccountDTO();
        account.setId("BANK");
        StatementIdDTO statementId = new StatementIdDTO();
        statementId.setAccount(account);
        statementId.setYear(2010);
        statementId.setMonth(1);
        StatementDTO statement = new StatementDTO();
        statement.setId(statementId);

        error = Objects.requireNonNull(getMockMvc().perform(delete("/jbr/int/money/statement")
                        .content(this.json(statement))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Cannot delete locked statement BANK201001", error);

        statementId.setMonth(2);
        getMockMvc().perform(delete("/jbr/int/money/statement")
                        .content(this.json(statement))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        // TODO check that the previous statement is now unlocked.
    }

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

        // Check the url.
        getMockMvc().perform(get("/jbr/int/money/statement")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id.account.id", is("AMEX")))
                .andExpect(jsonPath("$[1].id.account.id", is("BANK")))
                .andExpect(jsonPath("$[0].openBalance", is(0.0)))
                .andExpect(jsonPath("$[1].openBalance", is(0.0)));
    }

    @Test
    public void testLockInvalidAccount() throws Exception {
        LockStatementRequest lockReqest = new LockStatementRequest();
        lockReqest.setAccountId("XXXX");
        lockReqest.setYear(2010);
        lockReqest.setMonth(1);
        String error = Objects.requireNonNull(getMockMvc().perform(post("/jbr/ext/money/statement/lock")
                        .content(this.json(lockReqest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Cannot find account with id XXXX", error);
    }

    @Test
    public void testLockInvalidStatementId() throws Exception {
        LockStatementRequest lockReqest = new LockStatementRequest();
        lockReqest.setAccountId("BANK");
        lockReqest.setYear(2012);
        lockReqest.setMonth(1);
        String error = Objects.requireNonNull(getMockMvc().perform(post("/jbr/ext/money/statement/lock")
                        .content(this.json(lockReqest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Cannot find statement with id BANK 2012 1", error);
    }

    @Test
    public void testDeleteInvalidId() throws Exception {
        AccountDTO account = new AccountDTO();
        account.setId("XXXX");
        StatementIdDTO statementId = new StatementIdDTO();
        statementId.setAccount(account);
        statementId.setMonth(1);
        statementId.setYear(2010);
        StatementDTO statement = new StatementDTO();
        statement.setId(statementId);
        String error = Objects.requireNonNull(getMockMvc().perform(delete("/jbr/int/money/statement")
                        .content(this.json(statement))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Cannot find statement with id XXXX.201001", error);
    }

    @Test
    public void testUpdateAccount() throws Exception {
        AccountDTO account = new AccountDTO();
        account.setId("XXXX");
        StatementIdDTO statementId = new StatementIdDTO();
        statementId.setAccount(account);
        statementId.setMonth(1);
        statementId.setYear(2010);
        StatementDTO statement = new StatementDTO();
        statement.setId(statementId);
        String error = Objects.requireNonNull(getMockMvc().perform(put("/jbr/int/money/statement")
                        .content(this.json(statement))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Cannot find statement with id XXXX.201001", error);
    }

    @Test
    public void testStatementAlreadyExists() throws Exception {
        AccountDTO account = new AccountDTO();
        account.setId("AMEX");
        StatementIdDTO statementId = new StatementIdDTO();
        statementId.setAccount(account);
        statementId.setMonth(1);
        statementId.setYear(2010);
        StatementDTO statement = new StatementDTO();
        statement.setId(statementId);
        String error = Objects.requireNonNull(getMockMvc().perform(post("/jbr/int/money/statement")
                        .content(this.json(statement))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Statement already exists - AMEX.201001", error);
    }

    @Test
    public void testUpdateStatement() throws Exception {
        AccountDTO account = new AccountDTO();
        account.setId("AMEX");
        StatementIdDTO statementId = new StatementIdDTO();
        statementId.setAccount(account);
        statementId.setMonth(1);
        statementId.setYear(2010);
        StatementDTO statement = new StatementDTO();
        statement.setId(statementId);
        statement.setLocked(false);
        statement.setOpenBalance(1023.9);
        getMockMvc().perform(put("/jbr/int/money/statement")
                        .content(this.json(statement))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResolvedException();
    }
}
