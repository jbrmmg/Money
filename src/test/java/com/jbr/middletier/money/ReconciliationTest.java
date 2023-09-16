package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.ReconciliationRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.ReconcileUpdateDTO;
import com.jbr.middletier.money.dto.ReconciliationFileDTO;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.manager.ReconciliationFileManager;
import com.jbr.middletier.money.manager.ReconciliationManager;
import com.jbr.middletier.money.dto.MatchDataDTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class ReconciliationTest extends Support {
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private ReconciliationRepository reconciliationRepository;
    @Autowired
    private ReconciliationFileManager fileManager;
    @Autowired
    private ReconciliationManager reconciliationManager;
    @Autowired
    private ReconciliationFileManager reconciliationFileManager;
    @Autowired
    private StatementRepository statementRepository;

    @Before
    public void cleanUp() {
        transactionRepository.deleteAll();
        reconciliationRepository.deleteAll();
    }

    @Test
    public void testCannotFindFile() throws Exception {
        ReconciliationFileDTO reconciliationFile = new ReconciliationFileDTO();
        reconciliationFile.setFilename("Blah");

        String error = Objects.requireNonNull(getMockMvc().perform(post("/jbr/int/money/reconciliation/load")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Cannot find Blah", error);
    }

    @Test
    public void testGetFiles() throws Exception {
        int files = fileManager.getFiles().size();

        getMockMvc().perform(get("/jbr/int/money/reconciliation/files")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(files)));
    }

    @Test
    public void testLoadFile() throws Exception {
        ReconciliationFileDTO reconciliationFile = new ReconciliationFileDTO();

        fileManager.getFiles().forEach(f -> {
            if(f.getFilename().contains("test.AMEX.match.csv")) {
                reconciliationFile.setFilename(f.getFilename());
            }
        });

        getMockMvc().perform(post("/jbr/int/money/reconciliation/load")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)));

        getMockMvc().perform(get("/jbr/int/money/match?account=AMEX")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].description", containsInAnyOrder("3CPAYMENT*PRET A MANGER LONDON X", "3CPAYMENT*PRET A MANGER LONDON", "AUDIBLE UK ADBL.CO/PYMT")));
    }

    @Test
    public void testAutoReconcile() throws Exception {
        ReconciliationFileDTO reconciliationFile = new ReconciliationFileDTO();

        fileManager.getFiles().forEach(f -> {
            if(f.getFilename().contains("test.AMEX.match.csv")) {
                reconciliationFile.setFilename(f.getFilename());
            }
        });

        getMockMvc().perform(post("/jbr/int/money/reconciliation/load")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)));

        getMockMvc().perform(put("/jbr/ext/money/reconciliation/auto")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(put("/jbr/int/money/reconciliation/auto")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk());
    }

    private ReconciliationFileDTO getReconcileFile() {
        ReconciliationFileDTO reconciliationFile = new ReconciliationFileDTO();

        fileManager.getFiles().forEach(f -> {
            if(f.getFilename().contains("test.AMEX.match.csv")) {
                reconciliationFile.setFilename(f.getFilename());
            }
        });

        return reconciliationFile;
    }

    @Test
    public void testClearReconcile() throws Exception {
        ReconciliationFileDTO reconciliationFile = getReconcileFile();

        getMockMvc().perform(post("/jbr/int/money/reconciliation/load")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)));

        getMockMvc().perform(delete("/jbr/ext/money/reconciliation/clear")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/jbr/int/money/reconciliation/load")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)));

        getMockMvc().perform(delete("/jbr/int/money/reconciliation/clear")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk());
    }

    @Test
    public void testUpdate() throws Exception {
        ReconciliationFileDTO reconciliationFile = new ReconciliationFileDTO();

        fileManager.getFiles().forEach(f -> {
            if(f.getFilename().contains("test.AMEX.match.csv")) {
                reconciliationFile.setFilename(f.getFilename());
            }
        });

        getMockMvc().perform(post("/jbr/int/money/reconciliation/load")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)));

        ReconcileUpdateDTO reconcileUpdate = new ReconcileUpdateDTO();
        reconcileUpdate.setCategoryId("FSE");
        reconcileUpdate.setId(1);

        getMockMvc().perform(put("/jbr/ext/money/reconciliation/update")
                        .content(this.json(reconcileUpdate))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        reconcileUpdate = new ReconcileUpdateDTO();
        reconcileUpdate.setCategoryId("FDG");
        reconcileUpdate.setId(2);

        getMockMvc().perform(put("/jbr/int/money/reconciliation/update")
                        .content(this.json(reconcileUpdate))
                        .contentType(getContentType()))
                .andExpect(status().isOk());
    }

    @Test
    public void testInvalidAccountId() {
        try {
            this.reconciliationManager.matchImpl("XXXX");
            Assert.fail();
        } catch(UpdateDeleteAccountException ex) {
            Assert.assertEquals("Cannot find account with id Invalid account id.XXXX", ex.getMessage());
        }
    }

    @Test
    public void testSetCategoryUpdate() throws IOException {
        // load the file.
        ReconciliationFileDTO reconciliationFile = getReconcileFile();
        this.reconciliationManager.loadFile(reconciliationFile, reconciliationFileManager);

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
                Assert.assertEquals("HSE", r.getCategory().getId());
            }
        });
    }

    private Transaction createTransaction(String accountId, String categoryId, double amount, LocalDate date) {
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
    public void testSetTransactionCategoryUpdate() {
        Transaction testTransaction = createTransaction("AMEX", "HSE", 10, LocalDate.of(2010,5,1));
//        this.transactionRepository.save(testTransaction);

        // Set the category
        ReconcileUpdateDTO reconcileUpdate = new ReconcileUpdateDTO();
        reconcileUpdate.setId(testTransaction.getId());
        reconcileUpdate.setType("trn");
        reconcileUpdate.setCategoryId("FDG");
        this.reconciliationManager.processReconcileUpdate(reconcileUpdate);

        this.transactionRepository.findAll().forEach(t -> {
            if(t.getId() == testTransaction.getId()) {
                Assert.assertEquals("FDG", t.getCategory().getId());
            }
        });
    }

    @Test
    public void testSetTransactionCategoryUpdate2() {
        Transaction testTransaction = createTransaction("AMEX", "HSE", 10, LocalDate.of(2010,5,1));

        // Set the category
        ReconcileUpdateDTO reconcileUpdate = new ReconcileUpdateDTO();
        reconcileUpdate.setId(testTransaction.getId() + 1);
        reconcileUpdate.setType("trn");
        reconcileUpdate.setCategoryId("FDG");
        this.reconciliationManager.processReconcileUpdate(reconcileUpdate);

        this.transactionRepository.findAll().forEach(t -> {
            if(t.getId() == testTransaction.getId()) {
                Assert.assertEquals("HSE", t.getCategory().getId());
            }
        });
    }

    @Test
    public void testSetTransactionCategoryUpdate3() {
        Transaction testTransaction = createTransaction("AMEX", "HSE", 10, LocalDate.of(2010,5,1));

        // Set the category
        ReconcileUpdateDTO reconcileUpdate = new ReconcileUpdateDTO();
        reconcileUpdate.setId(testTransaction.getId());
        reconcileUpdate.setType("trn");
        reconcileUpdate.setCategoryId("XXXX");
        this.reconciliationManager.processReconcileUpdate(reconcileUpdate);

        this.transactionRepository.findAll().forEach(t -> {
            if(t.getId() == testTransaction.getId()) {
                Assert.assertEquals("HSE", t.getCategory().getId());
            }
        });
    }

    @Test
    public void testSetTransactionCategoryUpdate4() {
        Transaction testTransaction = createTransaction("AMEX", "TRF", 10, LocalDate.of(2010,5,1));

        Transaction testTransactionOpposite = createTransaction("AMEX", "TRF", -10, LocalDate.of(2010,5,1));

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
                Assert.assertEquals("TRF", t.getCategory().getId());
            }
        });
    }

    @Test
    public void testSetCategoryUpdateInvalidCategory() throws IOException {
        // load the file.
        ReconciliationFileDTO reconciliationFile = getReconcileFile();
        this.reconciliationManager.loadFile(reconciliationFile, reconciliationFileManager);

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
                Assert.assertNull(r.getCategory());
            }
        });
    }

    @Test
    public void reconcileInvalidId() throws IOException, MultipleUnlockedStatementException {
        // load the file.
        ReconciliationFileDTO reconciliationFile = getReconcileFile();
        this.reconciliationManager.loadFile(reconciliationFile, reconciliationFileManager);

        try {
            // There should be no transactions
            this.reconciliationManager.reconcile(20, true);
            Assert.fail();
        } catch (InvalidTransactionIdException ex) {
            Assert.assertEquals("Cannot find transaction with id 20", ex.getMessage());
        }
    }

    @Test
    public void testMultipleUnlockedException() throws IOException, InvalidTransactionIdException {
        // load the file.
        ReconciliationFileDTO reconciliationFile = getReconcileFile();
        this.reconciliationManager.loadFile(reconciliationFile, reconciliationFileManager);

        // Create a second unlocked statement.
        Statement duplicate = new Statement();
        Statement unlocked = getUnlockedStatement("AMEX");

        duplicate.setOpenBalance(0);
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
        testTransaction.setAmount(10);
        testTransaction.setDate(LocalDate.of(2010,5,1));
        this.transactionRepository.save(testTransaction);

        try {
            // There should be no transactions
            this.reconciliationManager.reconcile(testTransaction.getId(), true);
            Assert.fail();
        } catch (MultipleUnlockedStatementException ex) {
            Assert.assertEquals("There are multiple unlocked statements on AMEX", ex.getMessage());
        }

        this.statementRepository.delete(duplicate);
    }

    private Statement getUnlockedStatement(String accountId) {
        AtomicReference<Statement> unlocked = new AtomicReference<>();
        this.statementRepository.findAll().forEach(s -> {
            if(s.getId().getAccount().getId().equalsIgnoreCase(accountId) && !s.getLocked()) {
                unlocked.set(s);
            }
        });
        Assert.assertNotNull(unlocked.get());
        return unlocked.get();
    }

    @Test
    public void testMatchExactly() throws IOException, UpdateDeleteAccountException {
        // load the file.
        ReconciliationFileDTO reconciliationFile = getReconcileFile();
        this.reconciliationManager.loadFile(reconciliationFile, reconciliationFileManager);

        // Find the statement that is not locked.
        Statement unlocked = getUnlockedStatement("BANK");

        // Create a transaction
        Transaction testTransaction = createTransaction("BANK", "HSE", -1.9, LocalDate.of(2022,10,10));
        testTransaction.setStatement(unlocked);
        this.transactionRepository.save(testTransaction);

        List<MatchDataDTO> matchData = this.reconciliationManager.matchImpl("BANK");
        int setCategory = 0;
        int none = 0;
        for(MatchDataDTO next : matchData) {
            if(next.getForwardAction().equalsIgnoreCase("NONE")) {
                none++;
            } else if(next.getForwardAction().equalsIgnoreCase("SETCATEGORY")) {
                setCategory++;
            }
        }
        Assert.assertEquals(2,setCategory);
        Assert.assertEquals(1,none);
        Assert.assertEquals(3,matchData.size());
    }

    @Test
    public void testMatchExactlyPlusRecon() throws IOException, UpdateDeleteAccountException {
        // load the file.
        ReconciliationFileDTO reconciliationFile = getReconcileFile();
        this.reconciliationManager.loadFile(reconciliationFile, reconciliationFileManager);

        // Create a transaction
        createTransaction("BANK", "HSE", -1.9, LocalDate.of(2022,10,10));

        List<MatchDataDTO> matchData = this.reconciliationManager.matchImpl("BANK");
        int setCategory = 0;
        int reconcile = 0;
        for(MatchDataDTO next : matchData) {
            if(next.getForwardAction().equalsIgnoreCase("RECONCILE")) {
                reconcile++;
            } else if(next.getForwardAction().equalsIgnoreCase("SETCATEGORY")) {
                setCategory++;
            }
        }
        Assert.assertEquals(2,setCategory);
        Assert.assertEquals(1,reconcile);
        Assert.assertEquals(3,matchData.size());
    }

    @Test
    public void testMatchMoreTransactions() throws IOException, UpdateDeleteAccountException {
        // load the file.
        ReconciliationFileDTO reconciliationFile = getReconcileFile();
        this.reconciliationManager.loadFile(reconciliationFile, reconciliationFileManager);

        Statement unlocked = getUnlockedStatement("BANK");

        // Create a transaction
        Transaction transaction = createTransaction("BANK", "HSE", -36, LocalDate.of(2022,10,10));
        transaction.setStatement(unlocked);
        this.transactionRepository.save(transaction);

        List<MatchDataDTO> matchData = this.reconciliationManager.matchImpl("BANK");
        int setCategory = 0;
        int unreconcile = 0;
        for(MatchDataDTO next : matchData) {
            if(next.getForwardAction().equalsIgnoreCase("UNRECONCILE")) {
                unreconcile++;
            } else if(next.getForwardAction().equalsIgnoreCase("SETCATEGORY")) {
                setCategory++;
            }
        }
        Assert.assertEquals(3,setCategory);
        Assert.assertEquals(1,unreconcile);
        Assert.assertEquals(4,matchData.size());
    }

    @Test
    public void testAutomaticRec() throws IOException, UpdateDeleteAccountException, MultipleUnlockedStatementException, UpdateDeleteCategoryException, InvalidTransactionIdException, InvalidTransactionException {
        // load the file.
        ReconciliationFileDTO reconciliationFile = getReconcileFile();
        this.reconciliationManager.loadFile(reconciliationFile, reconciliationFileManager);

        List<MatchDataDTO> matchData = this.reconciliationManager.matchImpl("BANK");
        int reconcile = 0;
        for(MatchDataDTO next : matchData) {
            if(next.getForwardAction().equalsIgnoreCase("SETCATEGORY")) {
                reconcile++;
            }

            ReconcileUpdateDTO reconcileUpdateDTO = new ReconcileUpdateDTO();
            reconcileUpdateDTO.setCategoryId("HSE");
            reconcileUpdateDTO.setType("rec");
            reconcileUpdateDTO.setId(next.getId());
            this.reconciliationManager.processReconcileUpdate(reconcileUpdateDTO);
        }
        Assert.assertEquals(3,reconcile);
        Assert.assertEquals(3,matchData.size());

        this.reconciliationManager.autoReconcileData();
        this.reconciliationManager.autoReconcileData();

        int count = 0;
        for(Transaction next : this.transactionRepository.findAll()) {
            Assert.assertTrue(next.reconciled());
            Assert.assertEquals("HSE", next.getCategory().getId());
            count++;
        }
        Assert.assertEquals(3, count);
    }
}
