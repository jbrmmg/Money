package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.UtilityMapper;
import com.jbr.middletier.money.exceptions.InvalidStatementIdException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;

import static org.hamcrest.Matchers.containsInAnyOrder;
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
    private TransactionRepository transactionRepository;

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UtilityMapper utilityMapper;

    private void cleanUp() {
        transactionRepository.deleteAll();
        statementRepository.deleteAll();

        for(Account next : accountRepository.findAll()) {
            Statement statement = new Statement();
            statement.setId(new StatementId(next,2010,1));
            statement.setLocked(false);
            statement.setOpenBalance(0);

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
        transaction1.setAmount(1280.32);

        TransactionDTO transaction2 = new TransactionDTO();
        transaction2.setAccountId("AMEX");

        // Add transaction.
        getMockMvc().perform(post("/jbr/ext/money/transaction")
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
            reconcileRequest.setId(nextTransaction.getId());
            reconcileRequest.setReconcile(true);
            getMockMvc().perform(put("/jbr/ext/money/reconcile")
                            .content(this.json(reconcileRequest))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // Lock the statement.
        StatementIdDTO statementId = new StatementIdDTO("BANK",1,2010);
        getMockMvc().perform(post("/jbr/ext/money/statement/lock")
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
                        .content(this.json(statementId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Statement already locked BANK201001", error);

        // Delete the statement.
        StatementDTO statement = new StatementDTO();
        statement.setAccountId("BANK");
        statement.setMonth(1);
        statement.setYear(2010);

        error = Objects.requireNonNull(getMockMvc().perform(delete("/jbr/int/money/statement")
                        .content(this.json(statement))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Cannot delete locked statement BANK 1 2010", error);

        statement.setMonth(2);
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
                .andExpect(jsonPath("$[0].accountId", is("AMEX")))
                .andExpect(jsonPath("$[1].accountId", is("BANK")))
                .andExpect(jsonPath("$[0].openBalance", is(0.0)))
                .andExpect(jsonPath("$[1].openBalance", is(0.0)));

        // Check the url.
        getMockMvc().perform(get("/jbr/int/money/statement")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountId", is("AMEX")))
                .andExpect(jsonPath("$[1].accountId", is("BANK")))
                .andExpect(jsonPath("$[0].openBalance", is(0.0)))
                .andExpect(jsonPath("$[1].openBalance", is(0.0)));
    }

    @Test
    public void testLockInvalidAccount() throws Exception {
        cleanUp();

        StatementIdDTO statementId = new StatementIdDTO("FLIP",1,2020);
        statementId.setAccountId("FLIP");
        statementId.setYear(2020);
        statementId.setMonth(1);
        String error = Objects.requireNonNull(getMockMvc().perform(post("/jbr/ext/money/statement/lock")
                        .content(this.json(statementId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Cannot find statement with id FLIP202001", error);
    }

    @Test
    public void testLockInvalidStatementId() throws Exception {
        cleanUp();

        StatementIdDTO statementId = new StatementIdDTO("BANK",1,2012);
        String error = Objects.requireNonNull(getMockMvc().perform(post("/jbr/ext/money/statement/lock")
                        .content(this.json(statementId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Cannot find statement with id BANK201201", error);
    }

    @Test
    public void testDeleteInvalidId() throws Exception {
        cleanUp();

        StatementDTO statement = new StatementDTO();
        statement.setAccountId("FLIP");
        statement.setMonth(1);
        statement.setYear(2020);
        String error = Objects.requireNonNull(getMockMvc().perform(delete("/jbr/int/money/statement")
                        .content(this.json(statement))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Cannot find account with id FLIP", error);

        statement.setAccountId("BANK");
        error = Objects.requireNonNull(getMockMvc().perform(delete("/jbr/int/money/statement")
                        .content(this.json(statement))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Cannot delete last statement BANK 1 2020", error);
    }

    @Test
    public void testStatementAlreadyExists() throws Exception {
        cleanUp();

        StatementDTO statement = new StatementDTO();
        statement.setAccountId("AMEX");
        statement.setMonth(1);
        statement.setYear(2010);
        String error = Objects.requireNonNull(getMockMvc().perform(post("/jbr/int/money/statement")
                        .content(this.json(statement))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Statement already exists - AMEX 1 2010", error);
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
        statement.setOpenBalance(1023.9);
        getMockMvc().perform(post("/jbr/int/money/statement")
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
        statement.setOpenBalance(100);

        InvalidStatementIdException test = new InvalidStatementIdException(statement);
        Assert.assertEquals("Cannot find statement with id AMEX 1 2010", test.getMessage());
    }
}
