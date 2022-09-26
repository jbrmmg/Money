package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;

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
    private ApplicationProperties applicationProperties;

    private void cleanUp() {
        transactionRepository.deleteAll();
        for(Statement next : statementRepository.findAll()) {
            if(next.getLocked()) {
                next.setLocked(false);
                statementRepository.save(next);
            }
        }
    }

    @Test
    public void testReport() throws Exception {
        cleanUp();

        // Clear the directories.
        deleteDirectoryContents(new File(applicationProperties.getReportWorking()).toPath());
        deleteDirectoryContents(new File(applicationProperties.getReportShare()).toPath());

        // Create some transactions
        // TODO add transactions so that pie can be checked.
        NewTransaction transaction = new NewTransaction("AMEX", "HSE", LocalDate.of(2010,1,1), 10.02, "Testing");

        getMockMvc().perform(post("/jbr/ext/money/transaction/add")
                        .content(this.json(transaction))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        Account account = new Account();
        account.setId("AMEX");
        StatementId statementId = new StatementId();
        statementId.setAccount(account);
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
        LockStatementRequest lockStatementRequest = new LockStatementRequest();
        lockStatementRequest.setAccountId("AMEX");
        lockStatementRequest.setMonth(1);
        lockStatementRequest.setYear(2010);

        getMockMvc().perform(post("/jbr/int/money/statement/lock")
                        .content(this.json(lockStatementRequest))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Request the report.
        ArchiveOrReportRequest request = new ArchiveOrReportRequest();
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
