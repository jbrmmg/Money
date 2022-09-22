package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;
import java.text.SimpleDateFormat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class ArchiveTest extends Support {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Test
    public void testReport() throws Exception {
        // Clear the directories.
        deleteDirectoryContents(new File(applicationProperties.getReportWorking()).toPath());
        deleteDirectoryContents(new File(applicationProperties.getReportShare()).toPath());

        // Create some transactions
        // TODO add transactions so that pie can be checked.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-yy");
        NewTransaction transaction = new NewTransaction("AMEX", "HSE", sdf.parse("2010-01-01"), 10.02, "Testing");

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

        // TODO improve the checks in this
        boolean savedEnabled = applicationProperties.getArchiveEnabled();
        applicationProperties.setArchiveEnabled(false);
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
        applicationProperties.setArchiveEnabled(savedEnabled);
    }
}
