package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.manager.ArchiveManager;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;
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
    private ModelMapper modelMapper;

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
    public void testSchedule() {
        archiveManager.scheduledArchive();
        // Make sure the test gets here.
        // TODO add more checkds.
        Assert.assertTrue(true);
    }

    @Test
    @Ignore("HTML generation needs checking")
    public void testReport() throws Exception {
        cleanUp();

        // Clear the directories.
        deleteDirectoryContents(new File(applicationProperties.getReportWorking()).toPath());
        deleteDirectoryContents(new File(applicationProperties.getReportShare()).toPath());

        // Create some transactions
        // TODO add transactions so that pie can be checked.
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
    }
}
