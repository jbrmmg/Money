package com.jbr.middletier.money;

import com.itextpdf.text.DocumentException;
import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.primary.Account;
import com.jbr.middletier.money.data.primary.Category;
import com.jbr.middletier.money.data.primary.Statement;
import com.jbr.middletier.money.data.primary.Transaction;
import com.jbr.middletier.money.data.primary.repository.AccountRepository;
import com.jbr.middletier.money.data.primary.repository.StatementRepository;
import com.jbr.middletier.money.data.primary.repository.TransactionRepository;
import com.jbr.middletier.money.reporting.ReportGenerator;
import org.apache.batik.transcoder.TranscoderException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
public class ScheduleReportTest extends Support {
    @Autowired
    public ReportGenerator reportGenerator;
    @Autowired
    public TransactionRepository transactionRepository;
    @Autowired
    public AccountRepository accountRepository;
    @Autowired
    public StatementRepository statementRepository;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Test
    public void scheduleTest() throws TranscoderException, DocumentException, IOException {
        deleteDirectoryContents(new File(applicationProperties.getReportWorking()).toPath());
        deleteDirectoryContents(new File(applicationProperties.getReportShare()).toPath());

        boolean enabled = applicationProperties.getReportEnabled();
        applicationProperties.setReportEnabled(true);

        transactionRepository.deleteAll();
        reinstateStatements(statementRepository, accountRepository);

        Category category = new Category();
        category.setId("HSE");

        for(Account nextAccount : accountRepository.findAll()) {
            if(!nextAccount.getClosed()) {
                for(Statement nextStatement : statementRepository.findByIdAccountAndLocked(nextAccount,false)) {
                    Transaction transaction = new Transaction();
                    transaction.setStatement(nextStatement);
                    transaction.setCategory(category);
                    transaction.setDescription("Test");
                    transaction.setAccount(nextAccount);
                    transaction.setAmount(10);
                    transaction.setDate(LocalDate.of(2010,1,1));

                    transactionRepository.save(transaction);

                    nextStatement.setLocked(true);
                    statementRepository.save(nextStatement);
                }
            }
        }

        reportGenerator.regularReport();
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportShare() + "/2010/Report-January-2010.pdf").toPath()));

        applicationProperties.setReportEnabled(enabled);
        transactionRepository.deleteAll();
        reinstateStatements(statementRepository, accountRepository);
    }
}
