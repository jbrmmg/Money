package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.dataaccess.ReconciliationRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.ReconcileUpdateDTO;
import com.jbr.middletier.money.dto.ReconciliationFileDTO;
import com.jbr.middletier.money.exceptions.InvalidAccountIdException;
import com.jbr.middletier.money.manager.ReconciliationFileManager;
import com.jbr.middletier.money.manager.ReconciliationManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;
import java.util.Objects;

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
                .andExpect(jsonPath("$", hasSize(6)));

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
                .andExpect(jsonPath("$", hasSize(6)));

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
                .andExpect(jsonPath("$", hasSize(6)));

        getMockMvc().perform(delete("/jbr/ext/money/reconciliation/clear")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/jbr/int/money/reconciliation/load")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));

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
                .andExpect(jsonPath("$", hasSize(6)));

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
        } catch(InvalidAccountIdException ex) {
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
}
