package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.exceptions.EmailGenerationException;
import com.jbr.middletier.money.reporting.EmailGenerator;
import com.jbr.middletier.money.util.FinancialAmount;
import com.jbr.middletier.money.util.TransportWrapper;
import com.jbr.middletier.money.xml.html.EmailHtml;
import com.jbr.middletier.money.xml.html.HyperTextMarkupLanguage;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
@ActiveProfiles(value="emailtest")
public class EmailTest extends Support {

    @Autowired
    public TransactionRepository transactionRepository;
    @Autowired
    public AccountRepository accountRepository;
    @Autowired
    public StatementRepository statementRepository;
    @Autowired
    public ApplicationProperties applicationProperties;
    @Autowired
    public ModelMapper modelMapper;

    @Test
    public void testEmail() throws Exception {
        String error = Objects.requireNonNull(getMockMvc().perform(post("/jbr/int/money/email?host=throw&password=fake&to=throw@com")
                        .contentType(getContentType()))
                .andExpect(status().isFailedDependency())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Failed to send the message", error);
    }

    @Test
    public void testEmail2() throws Exception {
        getMockMvc().perform(post("/jbr/int/money/email?host=ignore.do.not.send&password=fake")
                        .contentType(getContentType()))
                .andExpect(status().isOk());
    }

    @Test
    public void testEmailFormat() throws IOException {
        FinancialAmount start = new FinancialAmount(-10.02);
        List<TransactionDTO> transactions = new ArrayList<>();

        AccountDTO account = new AccountDTO();
        account.setId("BANK");

        CategoryDTO category = new CategoryDTO();
        category.setId("HSE");
        category.setName("House");

        TransactionDTO transaction = new TransactionDTO();
        transaction.setId(1);
        transaction.setDescription("Test");
        transaction.setCategory(category);
        transaction.setAmount(192.92);
        transaction.setDate(LocalDate.of(2021,1,3));
        transaction.setAccount(account);
        transactions.add(transaction);

        transaction = new TransactionDTO();
        transaction.setId(1);
        transaction.setDescription("Test");
        transaction.setCategory(category);
        transaction.setAmount(-312.92);
        transaction.setDate(LocalDate.of(2021,1,12));
        transaction.setAccount(account);
        transactions.add(transaction);

        HyperTextMarkupLanguage email = new EmailHtml(start,transactions);
        String emailHtml = email.getHtmlAsString();
        Assert.assertEquals(1693, emailHtml.length());

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
        Assert.assertEquals(1,differenceCount);
        Assert.assertNotNull(expectedDifferent);
        Assert.assertEquals("/html[1]/head[1]/style[1]/text()[1]",expectedDifferent.getComparison().getControlDetails().getXPath());
    }

    @Test
    public void testEmail3() throws EmailGenerationException, IOException {
        deleteDirectoryContents(new File(applicationProperties.getReportWorking()).toPath());
        deleteDirectoryContents(new File(applicationProperties.getReportShare()).toPath());

        // Create a transaction to report.
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
        transaction.setDate(LocalDate.now().plusDays(-7));
        transaction.setDescription("Testing");
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setStatement(testStatement);
        transactionRepository.save(transaction);

        TransportWrapper testWrapper = message -> {
            try {
                Assert.assertNotNull(message);
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
                    differenceCount++;
                }
                Assert.assertEquals(2,differenceCount);
                Assert.assertNotNull(expectedDifferent);
            } catch (IOException e) {
                Assert.fail();
            }
        };

        EmailGenerator testGenerator = new EmailGenerator(
                transactionRepository,
                statementRepository,
                accountRepository,
                testWrapper,
                modelMapper,
                applicationProperties);

        testGenerator.generateReport("a", "b", "blah", "testing", "", 4);
    }
}
