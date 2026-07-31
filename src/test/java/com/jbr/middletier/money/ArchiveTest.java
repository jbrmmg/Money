package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.primary.*;
import com.jbr.middletier.money.data.primary.repository.AccountRepository;
import com.jbr.middletier.money.data.primary.repository.StatementRepository;
import com.jbr.middletier.money.data.primary.repository.TransactionRepository;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.StatementMapper;
import com.jbr.middletier.money.utils.UtilityMapper;
import com.jbr.middletier.money.manager.ArchiveManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
class ArchiveTest extends Support {
    private static final Logger LOG = LoggerFactory.getLogger(ArchiveTest.class);

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
    private UtilityMapper utilityMapper;

    @Autowired
    private StatementMapper statementMapper;

    private void cleanUp() {
        transactionRepository.deleteAll();

        for(Account next : accountRepository.findAll()) {
            Statement statement = new Statement();
            statement.setId(new StatementId(next,2010,1));
            statement.setLocked(false);
            statement.setOpenBalance(BigDecimal.ZERO);

            statementRepository.save(statement);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testSchedule() throws Exception {
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
        Assertions.assertNotEquals(0,count);
        Assertions.assertNotNull(testStatement);

        // Create a transaction that will be deleted.
        Category category = new Category();
        category.setId("HSE");

        Account account = new Account();
        account.setId("AMEX");

        Transaction transaction = new Transaction();
        transaction.setAmount(BigDecimal.TEN);
        transaction.setDate(LocalDate.of(2010, Month.JANUARY,1));
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
        Assertions.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-January-2010.pdf");
        Assertions.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-February-2010.pdf");
        Assertions.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-March-2010.pdf");
        Assertions.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-April-2010.pdf");
        Assertions.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-May-2010.pdf");
        Assertions.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-June-2010.pdf");
        Assertions.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-July-2010.pdf");
        Assertions.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-August-2010.pdf");
        Assertions.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-September-2010.pdf");
        Assertions.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-October-2010.pdf");
        Assertions.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-November-2010.pdf");
        Assertions.assertTrue(file.createNewFile());
        file = new File(applicationProperties.getReportShare() + "/2010/Report-December-2010.pdf");
        Assertions.assertTrue(file.createNewFile());

        archiveManager.scheduledArchive();

        // Statements should be deleted
        List<Statement> statements = new ArrayList<>();
        for(Statement checkStatement : statementRepository.findAll()) {
            statements.add(checkStatement);
        }
        Assertions.assertEquals(0, statements.size());

        List<Transaction> transactions = new ArrayList<>();
        for(Transaction checkTransaction : transactionRepository.findAll()) {
            transactions.add(checkTransaction);
        }

        if(!transactions.isEmpty()) {
            for(Transaction next: transactions) {
                LOG.info("test-find-log T {} {} {} {} {} {}", next.getDescription(), next.getAmount().getValue(), next.getAccount().getId(), next.getCategory().getId(), next.getDate(), next.getId());
            }
        }
        LOG.info("test-find-log T - done");
        Assertions.assertEquals(0,transactions.size());

        applicationProperties.setArchiveEnabled(false);
    }

    @Test
    void testReport() throws Exception {
        cleanUp();

        // Clear the directories.
        deleteDirectoryContents(new File(applicationProperties.getReportWorking()).toPath());
        deleteDirectoryContents(new File(applicationProperties.getReportShare()).toPath());

        // Create some transactions
        TransactionDTO transaction = new TransactionDTO();
        transaction.setAccountId("AMEX");
        transaction.setCategoryId("HSE");
        transaction.setDate(utilityMapper.map(LocalDate.of(2010, Month.JANUARY,1),String.class));
        transaction.setAmount(BigDecimal.valueOf(10.02));
        transaction.setDescription("Testing");

        getMockMvc().perform(post("/api/v1/transaction")
                        .content(this.json(Collections.singletonList(transaction)))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        StatementDTO statementDTO = new StatementDTO();
        statementDTO.setAccountId("AMEX");
        statementDTO.setMonth(1);
        statementDTO.setYear(2010);

        // Add the transaction to statement
        for(Transaction next : transactionRepository.findAll()) {
            next.setStatement(statementMapper.map(statementDTO,Statement.class));
            transactionRepository.save(next);
        }

        // Lock the statement
        StatementIdDTO lockStatementId = new StatementIdDTO("AMEX",1,2010);

        getMockMvc().perform(post("/api/v1/statement/lock")
                        .content(this.json(lockStatementId))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Request the report.
        ArchiveOrReportRequestDTO request = new ArchiveOrReportRequestDTO();
        request.setMonth(1);
        request.setYear(2010);

        // JBR-440: improve the checks in this
        getMockMvc().perform(post("/api/v1/transaction/archive")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        applicationProperties.setArchiveEnabled(true);
        getMockMvc().perform(post("/api/v1/transaction/archive")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Generate the report.
        getMockMvc().perform(post("/api/v1/transaction/annual-report")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/api/v1/transaction/archive")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        getMockMvc().perform(post("/api/v1/transaction/archive")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());
        Assertions.assertTrue(Files.exists(new File(applicationProperties.getReportShare() + "/2010/Report-2010.pdf").toPath()));
    }
}
