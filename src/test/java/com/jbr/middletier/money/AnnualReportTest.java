package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.primary.repository.ReportStatusRepository;
import com.jbr.middletier.money.data.primary.Statement;
import com.jbr.middletier.money.data.primary.Transaction;
import com.jbr.middletier.money.data.primary.repository.StatementRepository;
import com.jbr.middletier.money.data.primary.repository.TransactionRepository;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.StatementMapper;
import com.jbr.middletier.money.utils.UtilityMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
class AnnualReportTest extends Support {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private ReportStatusRepository reportStatusRepository;

    @Autowired
    private UtilityMapper utilityMapper;

    @Autowired
    private StatementMapper statementMapper;

    private void cleanUp() {
        transactionRepository.deleteAll();
        reportStatusRepository.deleteAll();
        for(Statement next : statementRepository.findAll()) {
            if(next.getLocked()) {
                next.setLocked(false);
                statementRepository.save(next);
            }
        }
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
        transaction.setDate(utilityMapper.map(LocalDate.of(2010, 1, 1),String.class));
        transaction.setAmount(BigDecimal.valueOf(10.02));
        transaction.setDescription("Testing");

        getMockMvc().perform(post("/api/v1/transaction")
                        .content(this.json(Collections.singletonList(transaction)))
                        .contentType(getContentType()))
                .andExpect(status().isOk());


        StatementDTO statement = new StatementDTO();
        statement.setAccountId("AMEX");
        statement.setMonth(1);
        statement.setYear(2010);

        // Add the transaction to statement
        for(Transaction next : transactionRepository.findAll()) {
            next.setStatement(statementMapper.map(statement,Statement.class));
            transactionRepository.save(next);
        }

        // Lock the statement
        StatementIdDTO lockStatementRequest = new StatementIdDTO("AMEX",1,2010);

        getMockMvc().perform(post("/api/v1/statement/lock")
                        .content(this.json(lockStatementRequest))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Request the report.
        ArchiveOrReportRequestDTO request = new ArchiveOrReportRequestDTO();
        request.setMonth(1);
        request.setYear(2010);

        getMockMvc().perform(post("/api/v1/transaction/annual-report")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Annual HTML should be written to the archive.
        Assertions.assertTrue(Files.exists(new File(applicationProperties.getReportShare() + "/2010/annual.html").toPath()));
        Assertions.assertTrue(Files.exists(new File(applicationProperties.getReportShare() + "/2010/index.html").toPath()));
        Assertions.assertTrue(Files.exists(new File(applicationProperties.getReportShare() + "/index.html").toPath()));
    }
}
