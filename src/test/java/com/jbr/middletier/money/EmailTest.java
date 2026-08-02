package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.primary.*;
import com.jbr.middletier.money.data.primary.repository.AccountRepository;
import com.jbr.middletier.money.data.primary.repository.StatementRepository;
import com.jbr.middletier.money.data.primary.repository.TransactionRepository;
import com.jbr.middletier.money.dto.EmailRequestDTO;
import com.jbr.middletier.money.dto.ReportDefinitionDTO;
import com.jbr.middletier.money.exceptions.EmailGenerationException;
import com.jbr.middletier.money.manager.AccountManager;
import com.jbr.middletier.money.manager.AccountTransactionManager;
import com.jbr.middletier.money.manager.StatementManager;
import com.jbr.middletier.money.reporting.EmailGenerator;
import com.jbr.middletier.money.util.FinancialAmount;
import com.jbr.middletier.money.util.TransportWrapper;
import com.jbr.middletier.money.xml.html.EmailHtml;
import com.jbr.middletier.money.xml.html.HyperTextMarkupLanguage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
@ActiveProfiles(value="emailtest")
class EmailTest extends Support {
    private static final Logger LOG = LoggerFactory.getLogger(EmailTest.class);

    @Autowired
    public TransactionRepository transactionRepository;
    @Autowired
    public AccountRepository accountRepository;
    @Autowired
    public StatementRepository statementRepository;
    @Autowired
    public ApplicationProperties applicationProperties;
    @Autowired
    public AccountTransactionManager accountTransactionManager;
    @Autowired
    public StatementManager statementManager;
    @Autowired
    public AccountManager accountManager;

    @Test
    void testEmail() throws Exception {
        // Request a report that doesn't exist — expect failure
        ReportDefinitionDTO request = new ReportDefinitionDTO("monthly", 2099, 1);
        String error = Objects.requireNonNull(getMockMvc().perform(post("/api/v1/email")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isFailedDependency())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("Failed to read report file: 2099", error);
    }

    @Test
    void testEmail2() throws Exception {
        // Create a dummy report file so the send succeeds
        File dir = new File(applicationProperties.getReportShare() + "/2010");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        File reportFile = new File(dir, "January.html");
        //noinspection ResultOfMethodCallIgnored
        reportFile.createNewFile();

        try {
            ReportDefinitionDTO request = new ReportDefinitionDTO("monthly", 2010, 1);
            getMockMvc().perform(post("/api/v1/email")
                            .content(this.json(request))
                            .contentType(getContentType()))
                    .andExpect(status().isOk());
        } finally {
            //noinspection ResultOfMethodCallIgnored
            reportFile.delete();
            //noinspection ResultOfMethodCallIgnored
            dir.delete();
        }
    }

    @Test
    void testEmailFormat() throws IOException {
        FinancialAmount start = new FinancialAmount(BigDecimal.valueOf(-10.02));
        List<Transaction> transactions = new ArrayList<>();

        Account account = new Account();
        account.setId("BANK");

        Category category = new Category();
        category.setId("HSE");
        category.setName("House");

        Transaction transaction = new Transaction();
        transaction.setDescription("Test");
        transaction.setCategory(category);
        transaction.setAmount(BigDecimal.valueOf(192.92));
        transaction.setDate(LocalDate.of(2021, Month.JANUARY,3));
        transaction.setAccount(account);
        transactions.add(transaction);

        transaction = new Transaction();
        transaction.setDescription("Test");
        transaction.setCategory(category);
        transaction.setAmount(BigDecimal.valueOf(-312.92));
        transaction.setDate(LocalDate.of(2021, Month.JANUARY,12));
        transaction.setAccount(account);
        transactions.add(transaction);

        HyperTextMarkupLanguage email = new EmailHtml(start,transactions);
        String emailHtml = email.getHtmlAsString();
        Assertions.assertEquals(1693, emailHtml.length());

        // Get the expected html
        File expectedFile = new File("./src/test/resources/expected/email.xml");
        String expected = new String(Files.readAllBytes(expectedFile.toPath()));// Get the difference.
        Diff htmlDiff = DiffBuilder.compare(expected).withTest(emailHtml).ignoreWhitespace().build();

        // Only the CSS should be different (this is checked separately).
        Iterator<Difference> iterator = htmlDiff.getDifferences().iterator();
        Difference expectedDifferent = null;
        int differenceCount = 0;
        while (iterator.hasNext()) {
            expectedDifferent = iterator.next();
            differenceCount++;
        }
        Assertions.assertEquals(1,differenceCount);
        Assertions.assertNotNull(expectedDifferent);
        Assertions.assertEquals("/html[1]/head[1]/style[1]/text()[1]",expectedDifferent.getComparison().getControlDetails().getXPath());
    }

    @Test
    void testEmail3() throws EmailGenerationException, IOException {
        transactionRepository.deleteAll();
        statementRepository.deleteAll();

        deleteDirectoryContents(new File(applicationProperties.getReportWorking()).toPath());
        deleteDirectoryContents(new File(applicationProperties.getReportShare()).toPath());

        // Setup two statements
        Account account = new Account();
        account.setId("AMEX");
        StatementId currentId = new StatementId();
        currentId.setAccount(account);
        currentId.setYear(2010);
        currentId.setMonth(1);
        Statement current = new Statement();
        current.setId(currentId);
        current.setLocked(true);
        current.setOpenBalance(BigDecimal.ZERO);
        statementRepository.save(current);

        StatementId nextId = new StatementId();
        nextId.setAccount(account);
        nextId.setYear(2010);
        nextId.setMonth(2);
        Statement next = new Statement();
        next.setId(nextId);
        next.setLocked(false);
        next.setOpenBalance(BigDecimal.valueOf(10));
        statementRepository.save(next);

        // Create a transaction that will be deleted.
        Category category = new Category();
        category.setId("HSE");

        Transaction transaction = new Transaction();
        transaction.setAmount(BigDecimal.valueOf(10));
        transaction.setDate(applicationProperties.getToday().plusDays(-30));
        transaction.setDescription("Testing");
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setStatement(current);
        transactionRepository.save(transaction);

        transaction = new Transaction();
        transaction.setAmount(BigDecimal.valueOf(20));
        transaction.setDate(applicationProperties.getToday().plusDays(-7));
        transaction.setDescription("Testing 2");
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setStatement(next);
        transactionRepository.save(transaction);

        transaction = new Transaction();
        transaction.setAmount(BigDecimal.valueOf(5));
        transaction.setDate(applicationProperties.getToday().plusDays(-6));
        transaction.setDescription("Testing 3");
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setStatement(next);
        transactionRepository.save(transaction);

        TransportWrapper testWrapper = message -> assertDoesNotThrow(() -> {
            Assertions.assertNotNull(message);
            String content = (String)message.getContent();

            // Get the expected html
            File expectedFile = new File("./src/test/resources/expected/email2.xml");
            String expected = new String(Files.readAllBytes(expectedFile.toPath()));// Get the difference.
            Diff htmlDiff = DiffBuilder.compare(expected).withTest(content).ignoreWhitespace().build();

            // Only the CSS should be different (this is checked separately).
            Iterator<Difference> iterator = htmlDiff.getDifferences().iterator();
            Difference expectedDifferent = null;
            int differenceCount = 0;
            while (iterator.hasNext()) {
                expectedDifferent = iterator.next();
                LOG.info(expectedDifferent.getComparison().getControlDetails().getXPath());
                differenceCount++;
            }
            Assertions.assertEquals(3,differenceCount);
            Assertions.assertNotNull(expectedDifferent);
        });

        EmailGenerator testGenerator = new EmailGenerator(
                accountTransactionManager,
                statementManager,
                accountManager,
                testWrapper,
                applicationProperties);

        EmailRequestDTO request = new EmailRequestDTO();
        request.setTo("a");
        request.setWeeks(4);
        testGenerator.generateReport(request);

        // Re-instate the standard statements.
        transactionRepository.deleteAll();
        reinstateStatements(statementRepository, accountRepository);
    }
}
