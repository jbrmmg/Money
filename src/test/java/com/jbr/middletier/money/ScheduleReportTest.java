package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.primary.repository.ReportStatusRepository;
import com.jbr.middletier.money.data.primary.Account;
import com.jbr.middletier.money.data.primary.Category;
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
    public ReportStatusRepository reportStatusRepository;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Test
    void scheduleTest() throws IOException {
        deleteDirectoryContents(new File(applicationProperties.getReportWorking()).toPath());
        deleteDirectoryContents(new File(applicationProperties.getReportShare()).toPath());

        boolean enabled = applicationProperties.getReportEnabled();
        applicationProperties.setReportEnabled(true);
        // Set today so Jan 2010 is within the 18-month window (window: Aug 2009 – Feb 2011)
        applicationProperties.setToday(LocalDate.of(2011, 2, 20));

        transactionRepository.deleteAll();
        reportStatusRepository.deleteAll();
        reinstateStatements(statementRepository, accountRepository);

        Category category = new Category();
        category.setId("HSE");

        for (Account nextAccount : accountRepository.findAll()) {
            if (!nextAccount.getClosed()) {
                Transaction transaction = new Transaction();
                transaction.setCategory(category);
                transaction.setDescription("Test");
                transaction.setAccount(nextAccount);
                transaction.setAmount(BigDecimal.valueOf(10));
                transaction.setDate(LocalDate.of(2010, Month.JANUARY, 1));
                transactionRepository.save(transaction);
            }
        }

        reportGenerator.regularReport();
        Assertions.assertTrue(Files.exists(new File(applicationProperties.getReportShare() + "/2010/January.html").toPath()));
        Assertions.assertTrue(Files.exists(new File(applicationProperties.getReportShare() + "/2010/index.html").toPath()));
        Assertions.assertTrue(Files.exists(new File(applicationProperties.getReportShare() + "/index.html").toPath()));

        applicationProperties.setReportEnabled(enabled);
        applicationProperties.setToday(null);
        transactionRepository.deleteAll();
        reportStatusRepository.deleteAll();
        reinstateStatements(statementRepository, accountRepository);
    }
}
