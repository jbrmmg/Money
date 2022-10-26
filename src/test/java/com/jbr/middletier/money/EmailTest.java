package com.jbr.middletier.money;

import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.reporting.EmailGenerator;
import com.jbr.middletier.money.util.FinancialAmount;
import com.jbr.middletier.money.util.TransportWrapper;
import com.jbr.middletier.money.xml.html.EmailHtml;
import com.jbr.middletier.money.xml.html.HyperTextMarkupLanguage;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
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

public class EmailTest {
    @Test
    @Ignore("This test is not yet ready.")
    public void testEmail() {
        TransactionRepository transactionRepository = Mockito.mock(TransactionRepository.class);
        StatementRepository statementRepository = Mockito.mock(StatementRepository.class);
        AccountRepository accountRepository = Mockito.mock(AccountRepository.class);
        TransportWrapper transportWrapper = Mockito.mock(TransportWrapper.class);
        ModelMapper modelMapper = Mockito.mock(ModelMapper.class);

        EmailGenerator emailGenerator = new EmailGenerator(
                transactionRepository,
                statementRepository,
                accountRepository,
                transportWrapper,
                modelMapper);
        Assert.assertNotNull(emailGenerator);

        //emailGenerator.generateReport("a@b.com", "a@b.com", "user", "host", "pwd", 1);
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
}
