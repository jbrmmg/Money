package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.primary.*;
import com.jbr.middletier.money.data.primary.repository.ReconciliationFileRepository;
import com.jbr.middletier.money.data.primary.repository.ReconciliationRepository;
import com.jbr.middletier.money.data.primary.repository.StatementRepository;
import com.jbr.middletier.money.data.primary.repository.TransactionRepository;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.manager.ReconcileFileLine;
import com.jbr.middletier.money.manager.ReconciliationFileManager;
import com.jbr.middletier.money.manager.ReconciliationManager;
import com.jbr.middletier.money.reconciliation.FileFormatDescription;
import com.jbr.middletier.money.reconciliation.FileFormatException;
import com.jbr.middletier.money.reconciliation.MatchData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
class ReconciliationTest extends Support {
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private ReconciliationRepository reconciliationRepository;
    @Autowired
    private ReconciliationFileManager fileManager;
    @Autowired
    private ReconciliationManager reconciliationManager;
    @Autowired
    private StatementRepository statementRepository;
    @Autowired
    private ReconciliationFileRepository reconciliationFileRepository;

    @BeforeEach
    void cleanUp() {
        transactionRepository.deleteAll();
        reconciliationRepository.deleteAll();
    }

    @Test
    void testCannotFindFile() throws Exception {
        ReconciliationFileDTO reconciliationFile = new ReconciliationFileDTO();
        reconciliationFile.setFilename("Blah");

        String error = Objects.requireNonNull(getMockMvc().perform(post("/api/v1/reconciliation/load")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("Cannot find Blah", error);
    }

    @Test
    void testGetFiles() throws Exception {
        int files = fileManager.getFiles().size();

        getMockMvc().perform(get("/api/v1/reconciliation/files")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(files)));
    }

    @Test
    void testLoadFile() throws Exception {
        ReconciliationFileDTO reconciliationFile = new ReconciliationFileDTO();

        fileManager.getFiles().forEach(f -> {
            if(f.getFilename().contains("test.AMEX.match.csv")) {
                reconciliationFile.setFilename(f.getFilename());
            }
        });

        getMockMvc().perform(post("/api/v1/reconciliation/load")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)));
    }

    private ReconciliationFileLoadDTO getReconcileFile() {
        ReconciliationFileLoadDTO reconciliationFile = new ReconciliationFileLoadDTO();

        fileManager.getFiles().forEach(f -> {
            if(f.getFilename().contains("test.AMEX.match.csv")) {
                reconciliationFile.setFilename(f.getFilename());
            }
        });

        return reconciliationFile;
    }

    @Test
    void testClearReconcile() throws Exception {
        ReconciliationFileLoadDTO reconciliationFile = getReconcileFile();

        getMockMvc().perform(post("/api/v1/reconciliation/load")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)));

        getMockMvc().perform(delete("/api/v1/reconciliation/clear")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/api/v1/reconciliation/load")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)));

        getMockMvc().perform(delete("/api/v1/reconciliation/clear")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk());
    }

    @Test
    void testInvalidAccountId() {
        this.reconciliationManager.clearRepositoryData();

        // Temporarily break the reconciliation file data.
        ReconciliationFile updateFile = null;
        for(ReconciliationFile next : reconciliationFileRepository.findAll()) {
            updateFile = next;
        }
        Assertions.assertNotNull(updateFile);
        boolean loaded = updateFile.getLoaded();
        updateFile.setLoaded(true);

        Account account = updateFile.getAccount();
        updateFile.setAccount(null);
        reconciliationFileRepository.save(updateFile);

        final ReconciliationFile finalUpdateFile = updateFile;
        NullOrBlankAccountIdException ex = assertThrows(NullOrBlankAccountIdException.class, () -> this.reconciliationManager.match());
        Assertions.assertEquals("Account ID not specified, reconciliation transactions required.", ex.getMessage());

        // Restore the file
        finalUpdateFile.setLoaded(loaded);
        finalUpdateFile.setAccount(account);
        reconciliationFileRepository.save(finalUpdateFile);
    }

    @Test
    void testSetCategoryUpdate() throws IOException {
        // load the file.
        ReconciliationFileLoadDTO reconciliationFile = getReconcileFile();
        this.reconciliationManager.loadFile(reconciliationFile);

        final int[] id = {0};
        this.reconciliationRepository.findAll().forEach(r -> {
            if(r.getDescription().equals("3CPAYMENT*PRET A MANGER LONDON X")) {
                id[0] = r.getId();
            }
        });

        // Set the category
        ReconcileUpdateDTO reconcileUpdate = new ReconcileUpdateDTO();
        reconcileUpdate.setId(id[0]);
        reconcileUpdate.setType("rec");
        reconcileUpdate.setCategoryId("HSE");
        this.reconciliationManager.processReconcileUpdate(reconcileUpdate);

        this.reconciliationRepository.findAll().forEach(r -> {
            if(r.getDescription().equals("3CPAYMENT*PRET A MANGER LONDON X")) {
                Assertions.assertEquals("HSE", r.getCategory().getId());
            }
        });
    }

    private Transaction createTransaction(String accountId, String categoryId, BigDecimal amount, LocalDate date) {
        Account account = new Account();
        account.setId(accountId);

        Category category = new Category();
        category.setId(categoryId);

        Transaction testTransaction = new Transaction();
        testTransaction.setAccount(account);
        testTransaction.setCategory(category);
        testTransaction.setAmount(amount);
        testTransaction.setDate(date);

        return this.transactionRepository.save(testTransaction);
    }

    @Test
    void testSetTransactionCategoryUpdate() {
        Transaction testTransaction = createTransaction("AMEX", "HSE", BigDecimal.valueOf(11), LocalDate.of(2010, Month.MAY,1));

        // Set the category
        ReconcileUpdateDTO reconcileUpdate = new ReconcileUpdateDTO();
        reconcileUpdate.setId(testTransaction.getId());
        reconcileUpdate.setType("trn");
        reconcileUpdate.setCategoryId("FDG");
        this.reconciliationManager.processReconcileUpdate(reconcileUpdate);

        this.transactionRepository.findAll().forEach(t -> {
            if(t.getId() == testTransaction.getId()) {
                Assertions.assertEquals("FDG", t.getCategory().getId());
            }
        });
    }

    @Test
    void testSetTransactionCategoryUpdate2() {
        Transaction testTransaction = createTransaction("AMEX", "HSE", BigDecimal.valueOf(12), LocalDate.of(2010, Month.MAY,1));

        // Set the category
        ReconcileUpdateDTO reconcileUpdate = new ReconcileUpdateDTO();
        reconcileUpdate.setId(testTransaction.getId() + 1);
        reconcileUpdate.setType("trn");
        reconcileUpdate.setCategoryId("FDG");
        this.reconciliationManager.processReconcileUpdate(reconcileUpdate);

        this.transactionRepository.findAll().forEach(t -> {
            if(t.getId() == testTransaction.getId()) {
                Assertions.assertEquals("HSE", t.getCategory().getId());
            }
        });
    }

    @Test
    void testSetTransactionCategoryUpdate3() {
        Transaction testTransaction = createTransaction("AMEX", "HSE", BigDecimal.valueOf(13), LocalDate.of(2010, Month.MAY,1));

        // Set the category
        ReconcileUpdateDTO reconcileUpdate = new ReconcileUpdateDTO();
        reconcileUpdate.setId(testTransaction.getId());
        reconcileUpdate.setType("trn");
        reconcileUpdate.setCategoryId("XXXX");
        this.reconciliationManager.processReconcileUpdate(reconcileUpdate);

        this.transactionRepository.findAll().forEach(t -> {
            if(t.getId() == testTransaction.getId()) {
                Assertions.assertEquals("HSE", t.getCategory().getId());
            }
        });
    }

    @Test
    void testSetTransactionCategoryUpdate4() {
        Transaction testTransaction = createTransaction("AMEX", "TRF", BigDecimal.valueOf(10), LocalDate.of(2010, Month.MAY,1));

        Transaction testTransactionOpposite = createTransaction("AMEX", "TRF", BigDecimal.valueOf(-10), LocalDate.of(2010, Month.MAY,1));

        testTransaction.setOppositeTransactionId(testTransactionOpposite.getId());
        this.transactionRepository.save(testTransaction);

        testTransactionOpposite.setOppositeTransactionId(testTransaction.getId());
        this.transactionRepository.save(testTransactionOpposite);

        // Set the category
        ReconcileUpdateDTO reconcileUpdate = new ReconcileUpdateDTO();
        reconcileUpdate.setId(testTransaction.getId());
        reconcileUpdate.setType("trn");
        reconcileUpdate.setCategoryId("FDG");
        this.reconciliationManager.processReconcileUpdate(reconcileUpdate);

        this.transactionRepository.findAll().forEach(t -> {
            if(t.getId() == testTransaction.getId()) {
                Assertions.assertEquals("TRF", t.getCategory().getId());
            }
        });
    }

    @Test
    void testSetCategoryUpdateInvalidCategory() throws IOException {
        // load the file.
        ReconciliationFileLoadDTO reconciliationFile = getReconcileFile();
        this.reconciliationManager.loadFile(reconciliationFile);

        final int[] id = {0};
        this.reconciliationRepository.findAll().forEach(r -> {
            if(r.getDescription().equals("3CPAYMENT*PRET A MANGER LONDON X")) {
                id[0] = r.getId();
            }
        });

        // Set the category
        ReconcileUpdateDTO reconcileUpdate = new ReconcileUpdateDTO();
        reconcileUpdate.setId(id[0]);
        reconcileUpdate.setType("rec");
        reconcileUpdate.setCategoryId("XXXX");
        this.reconciliationManager.processReconcileUpdate(reconcileUpdate);

        this.reconciliationRepository.findAll().forEach(r -> {
            if(r.getDescription().equals("3CPAYMENT*PRET A MANGER LONDON X")) {
                Assertions.assertNull(r.getCategory());
            }
        });
    }

    @Test
    void reconcileInvalidId() throws IOException {
        // load the file.
        ReconciliationFileLoadDTO reconciliationFile = getReconcileFile();
        this.reconciliationManager.loadFile(reconciliationFile);

        // There should be no transactions
        ReconcileTransactionDTO reconcileTransaction = new ReconcileTransactionDTO();
        reconcileTransaction.setReconcile(true);
        reconcileTransaction.getTransactions().add(20);
        InvalidTransactionIdException ex = assertThrows(InvalidTransactionIdException.class, () -> this.reconciliationManager.reconcile(reconcileTransaction));
        Assertions.assertEquals("Cannot find transaction with id 20", ex.getMessage());
    }

    @Test
    void testMultipleUnlockedException() throws IOException {
        // load the file.
        ReconciliationFileLoadDTO reconciliationFile = getReconcileFile();
        this.reconciliationManager.loadFile(reconciliationFile);

        // Create a second unlocked statement.
        Statement duplicate = new Statement();
        Statement unlocked = getUnlockedStatement("AMEX");

        duplicate.setOpenBalance(BigDecimal.ZERO);
        duplicate.setLocked(false);
        duplicate.setId(unlocked.getId());
        duplicate.getId().setMonth(unlocked.getId().getMonth()+1);
        if(duplicate.getId().getMonth() > 12) {
            duplicate.getId().setMonth(1);
            duplicate.getId().setYear(unlocked.getId().getYear()+1);
        }
        this.statementRepository.save(duplicate);

        Account account = new Account();
        account.setId("AMEX");

        Category category = new Category();
        category.setId("HSE");

        Transaction testTransaction = new Transaction();
        testTransaction.setAccount(account);
        testTransaction.setCategory(category);
        testTransaction.setAmount(BigDecimal.valueOf(1554));
        testTransaction.setDate(LocalDate.of(2010, Month.MAY,1));
        testTransaction = this.transactionRepository.save(testTransaction);

        // There should be no transactions
        ReconcileTransactionDTO reconcileTransaction = new ReconcileTransactionDTO();
        reconcileTransaction.setReconcile(true);
        reconcileTransaction.getTransactions().add(testTransaction.getId());
        MultipleUnlockedStatementException multiEx = assertThrows(MultipleUnlockedStatementException.class, () -> this.reconciliationManager.reconcile(reconcileTransaction));
        Assertions.assertEquals("There are multiple unlocked statements on AMEX", multiEx.getMessage());

        this.statementRepository.delete(duplicate);
        this.transactionRepository.delete(testTransaction);
    }

    private Statement getUnlockedStatement(String accountId) {
        AtomicReference<Statement> unlocked = new AtomicReference<>();
        this.statementRepository.findAll().forEach(s -> {
            if(s.getId().getAccount().getId().equalsIgnoreCase(accountId) && !s.getLocked()) {
                unlocked.set(s);
            }
        });
        Assertions.assertNotNull(unlocked.get());
        return unlocked.get();
    }

    @Test
    void testMatchExactly() throws IOException, NullOrBlankAccountIdException {
        // load the file.
        ReconciliationFileLoadDTO reconciliationFile = getReconcileFile();
        this.reconciliationManager.loadFile(reconciliationFile);

        // Find the statement that is not locked.
        Statement unlocked = getUnlockedStatement("AMEX");

        // Create a transaction
        Transaction testTransaction = createTransaction("AMEX", "HSE", BigDecimal.valueOf(-1.9), LocalDate.of(2022, Month.OCTOBER,10));
        testTransaction.setStatement(unlocked);
        this.transactionRepository.save(testTransaction);

        List<MatchData> matchData = this.reconciliationManager.match();
        int setCategory = 0;
        int none = 0;
        for(MatchData next : matchData) {
            if(next.getCategory() != null) {
                none++;
            } else {
                setCategory++;
            }
        }
        Assertions.assertEquals(2,setCategory);
        Assertions.assertEquals(1,none);
        Assertions.assertEquals(3,matchData.size());
    }

    @Test
    void testMatchExactlyPlusRecon() throws IOException, NullOrBlankAccountIdException {
        // load the file.
        ReconciliationFileLoadDTO reconciliationFile = getReconcileFile();
        this.reconciliationManager.loadFile(reconciliationFile);

        // Create a transaction
        createTransaction("AMEX", "HSE", BigDecimal.valueOf(-1.9), LocalDate.of(2022, Month.OCTOBER,10));

        List<MatchData> matchData = this.reconciliationManager.match();
        int setCategory = 0;
        int reconcile = 0;
        for(MatchData next : matchData) {
            if(next.getCategory() != null) {
                reconcile++;
            } else {
                setCategory++;
            }
        }
        Assertions.assertEquals(2,setCategory);
        Assertions.assertEquals(1,reconcile);
        Assertions.assertEquals(3,matchData.size());
    }

    @Test
    void testMatchMoreTransactions() throws IOException, NullOrBlankAccountIdException {
        // load the file.
        ReconciliationFileLoadDTO reconciliationFile = getReconcileFile();
        this.reconciliationManager.loadFile(reconciliationFile);

        Statement unlocked = getUnlockedStatement("AMEX");

        // Create a transaction
        Transaction transaction = createTransaction("AMEX", "HSE", BigDecimal.valueOf(-36), LocalDate.of(2022, Month.OCTOBER,10));
        transaction.setStatement(unlocked);
        this.transactionRepository.save(transaction);

        List<MatchData> matchData = this.reconciliationManager.match();
        int setCategory = 0;
        int unreconcile = 0;
        for(MatchData next : matchData) {
            if(next.getCategory() != null) {
                unreconcile++;
            } else {
                setCategory++;
            }
        }
        Assertions.assertEquals(3,setCategory);
        Assertions.assertEquals(0,unreconcile);
        Assertions.assertEquals(3,matchData.size());
    }

    @Test
    void testFileFormatException() {
        ReconcileFormat format = new ReconcileFormat();
        format.setId("TEST");
        format.setDescriptionColumn(6);

        FileFormatDescription description = new FileFormatDescription(format);

        FileFormatException ex = assertThrows(FileFormatException.class, () -> description.getDescription(new ReconcileFileLine(1,"x,y,z")));
        Assertions.assertEquals("Required index out of range on line 1",ex.getMessage());
    }
}
