package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.DtoBasicModelMapper;
import com.jbr.middletier.money.manager.ArchiveManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
public class ArchiveTest extends Support {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ArchiveManager archiveManager;

    @Autowired
    private DtoBasicModelMapper modelMapper;

    private void cleanUp() {
        transactionRepository.deleteAll();

        for(Account next : accountRepository.findAll()) {
            Statement statement = new Statement();
            statement.setId(new StatementId(next,2010,1));
            statement.setLocked(false);
            statement.setOpenBalance(0);

            statementRepository.save(statement);
        }
    }

    @Test
    public void testSchedule() throws Exception {
        applicationProperties.setArchiveEnabled(true);

        Statement testStatement = null;
        int count = 0;
        for(Statement next : statementRepository.findAll()) {
            count++;

            if(next.getId().getAccount().getId().equals("AMEX") &&
                next.getId().getYear() == 2010 &&
                next.getId().getMonth() == 1) {
                testStatement = next;
            }
        }
        Assert.assertNotEquals(0,count);
        Assert.assertNotNull(testStatement);

        // Create a transaction that will be deleted.
        Category category = new Category();
        category.setId("HSE");

        Account account = new Account();
        account.setId("AMEX");

        Transaction transaction = new Transaction();
        transaction.setAmount(10);
        transaction.setDate(LocalDate.of(2010,1,1));
        transaction.setDescription("Testing");
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setStatement(testStatement);
        transactionRepository.save(transaction);

        // Clear the directories.
        deleteDirectoryContents(new File(applicationProperties.getReportWorking()).toPath());
        deleteDirectoryContents(new File(applicationProperties.getReportShare()).toPath());

        File directory = new File(applicationProperties.getReportShare() + "/2010/");
        directory.mkdirs();
        File file = new File(applicationProperties.getReportShare() + "/2010/Report-2010.pdf");
        Assert.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-January-2010.pdf");
        Assert.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-February-2010.pdf");
        Assert.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-March-2010.pdf");
        Assert.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-April-2010.pdf");
        Assert.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-May-2010.pdf");
        Assert.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-June-2010.pdf");
        Assert.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-July-2010.pdf");
        Assert.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-August-2010.pdf");
        Assert.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-September-2010.pdf");
        Assert.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-October-2010.pdf");
        Assert.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-November-2010.pdf");
        Assert.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-December-2010.pdf");
        Assert.assertTrue(file.createNewFile());

        archiveManager.scheduledArchive();

        // Statements should be deleted
        count = 0;
        for(Statement ignored : statementRepository.findAll()) {
            count++;
        }
        Assert.assertEquals(0,count);

        count = 0;
        for(Transaction ingnored : transactionRepository.findAll()) {
            count++;
        }
        Assert.assertEquals(0,count);

        applicationProperties.setArchiveEnabled(false);
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
        StatementIdDTO lockStatementId = new StatementIdDTO();
        lockStatementId.setAccount(account);
        lockStatementId.setMonth(1);
        lockStatementId.setYear(2010);

        getMockMvc().perform(post("/jbr/int/money/statement/lock")
                        .content(this.json(lockStatementId))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Request the report.
        ArchiveOrReportRequestDTO request = new ArchiveOrReportRequestDTO();
        request.setMonth(1);
        request.setYear(2010);

        // TODO improve the checks in this
        getMockMvc().perform(post("/jbr/int/money/transaction/archive")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        applicationProperties.setArchiveEnabled(true);
        getMockMvc().perform(post("/jbr/int/money/transaction/archive")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Generate the report.
        getMockMvc().perform(post("/jbr/int/money/transaction/annualreport")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/jbr/int/money/transaction/archive")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/jbr/int/money/transaction/archive")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportShare() + "/2010/Report-2010.pdf").toPath()));
    }
}
