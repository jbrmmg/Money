package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.dataaccess.ReconciliationRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.ReconciliationFileDTO;
import com.jbr.middletier.money.manager.ReconciliationFileManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Objects;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Before
    public void cleanUp() {
        transactionRepository.deleteAll();
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
            if(f.getFilename().contains("test.AMEX.csv")) {
                reconciliationFile.setFilename(f.getFilename());
            }
        });

        getMockMvc().perform(post("/jbr/int/money/reconciliation/load")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));

        getMockMvc().perform(get("/jbr/int/money/match?account=AMEX")
                        .content(this.json(reconciliationFile))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(15)));
        //TODO - do more testing.
    }
}
