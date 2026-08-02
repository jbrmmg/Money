package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.primary.Account;
import com.jbr.middletier.money.data.primary.Category;
import com.jbr.middletier.money.data.primary.Statement;
import com.jbr.middletier.money.data.primary.StatementId;
import com.jbr.middletier.money.data.primary.Transaction;
import com.jbr.middletier.money.data.primary.repository.AccountRepository;
import com.jbr.middletier.money.data.primary.repository.StatementRepository;
import com.jbr.middletier.money.data.primary.repository.TransactionRepository;
import com.jbr.middletier.money.reporting.ReportGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.Month;

@SpringBootTest(classes = MiddleTier.class)
class ScheduleReportTest extends Support {
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
    void scheduleTest() throws IOException {
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
                    transaction.setAmount(BigDecimal.valueOf(10));
                    transaction.setDate(LocalDate.of(2010, Month.JANUARY,1));

                    transactionRepository.save(transaction);

                    nextStatement.setLocked(true);
                    statementRepository.save(nextStatement);
                }
            }
        }

        // Add locked statements for Feb-Apr 2010 (no transactions) so that end=Apr gives eval=Jan,
        // placing January within the [start, eval] range that regularReport will generate.
        for (Account nextAccount : accountRepository.findAll()) {
            if (!nextAccount.getClosed()) {
                for (int m = 2; m <= 4; m++) {
                    Statement s = new Statement();
                    s.setId(new StatementId(nextAccount, 2010, m));
                    s.setOpenBalance(java.math.BigDecimal.ZERO);
                    s.setLocked(true);
                    statementRepository.save(s);
                }
            }
        }

        reportGenerator.regularReport();
        Assertions.assertTrue(Files.exists(new File(applicationProperties.getReportShare() + "/2010/January.html").toPath()));
        Assertions.assertTrue(Files.exists(new File(applicationProperties.getReportShare() + "/2010/index.html").toPath()));
        Assertions.assertTrue(Files.exists(new File(applicationProperties.getReportShare() + "/index.html").toPath()));

        applicationProperties.setReportEnabled(enabled);
        transactionRepository.deleteAll();
        reinstateStatements(statementRepository, accountRepository);
    }
}
