package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.ArchiveOrReportRequestDTO;
import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.dto.TransactionDTO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
@ActiveProfiles(value="report")
public class ReportTest extends Support {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private ModelMapper modelMapper;

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

    @Test
    public void testReport() throws Exception {
        cleanUp();

        // Clear the directories.
        deleteDirectoryContents(new File(applicationProperties.getReportWorking()).toPath());
        deleteDirectoryContents(new File(applicationProperties.getReportShare()).toPath());

        // Create some transactions
        AccountDTO account = new AccountDTO();
        account.setId("AMEX");
        CategoryDTO category = new CategoryDTO();
        category.setId("HSE");
        TransactionDTO transaction = new TransactionDTO();
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setDate(LocalDate.of(2010,1,1));
        transaction.setAmount(10.02);
        transaction.setDescription("Testing");

        getMockMvc().perform(post("/jbr/ext/money/transaction")
                        .content(this.json(Collections.singletonList(transaction)))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        transaction.setCategory(category);
        transaction.setDate(LocalDate.of(2010,1,2));
        transaction.setAmount(210.02);
        transaction.setDescription("Testing 1");

        getMockMvc().perform(post("/jbr/ext/money/transaction")
                        .content(this.json(Collections.singletonList(transaction)))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        category.setId("FDG");
        transaction.setCategory(category);
        transaction.setDate(LocalDate.of(2010,1,2));
        transaction.setAmount(84.12);
        transaction.setDescription("Testing 2");

        getMockMvc().perform(post("/jbr/ext/money/transaction")
                        .content(this.json(Collections.singletonList(transaction)))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        StatementId statementId = new StatementId();
        statementId.setAccount(modelMapper.map(account,Account.class));
        statementId.setMonth(1);
        statementId.setYear(2010);
        Statement statement = new Statement();
        statement.setId(statementId);

        // Add the transaction to statement
        for(Transaction next : transactionRepository.findAll()) {
            next.setStatement(statement);
            transactionRepository.save(next);
        }

        // Lock the statement
        getMockMvc().perform(post("/jbr/int/money/statement/lock")
                        .content(this.json(statementId))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Request the report.
        ArchiveOrReportRequestDTO request = new ArchiveOrReportRequestDTO();
        request.setMonth(1);
        request.setYear(2010);

        getMockMvc().perform(post("/jbr/int/money/transaction/report")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Check that the report exists.
        // TODO do a more indepth comparison of the SVG files.
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/AMEX.png").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/AMEX.svg").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/HSE.png").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/HSE.svg").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/Report.html").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportShare() + "/2010/Report-January-2010.pdf").toPath()));
    }
}
