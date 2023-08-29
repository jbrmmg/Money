package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.DtoBasicModelMapper;
import com.jbr.middletier.money.dto.mapper.DtoComplexModelMapper;
import com.jbr.middletier.money.utils.CssAssertHelper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.DOMBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.xml.sax.InputSource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;

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
    private AccountRepository accountRepository;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private DtoBasicModelMapper modelMapper;

    private void cleanUp() {
        transactionRepository.deleteAll();
        statementRepository.deleteAll();

        for(Account next : accountRepository.findAll()) {
            Statement statement = new Statement();
            statement.setId(new StatementId(next,2010,1));
            statement.setLocked(false);
            statement.setOpenBalance(0);

            statementRepository.save(statement);
        }
    }

    @Test
    public void testReport() throws Exception {
        cleanUp();

        // Clear the directories.
        deleteDirectoryContents(new File(applicationProperties.getReportWorking()).toPath());
        deleteDirectoryContents(new File(applicationProperties.getReportShare()).toPath());

        // Create some transactions
        TransactionDTO transaction = new TransactionDTO();
        transaction.setAccountId("AMEX");
        transaction.setCategoryId("HSE");
        transaction.setDate(DtoComplexModelMapper.localDateStringConverter.convert(LocalDate.of(2010,1,1)));
        transaction.setAmount(-10.02);
        transaction.setDescription("Testing");

        getMockMvc().perform(post("/jbr/ext/money/transaction")
                        .content(this.json(Collections.singletonList(transaction)))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        transaction.setDate(DtoComplexModelMapper.localDateStringConverter.convert(LocalDate.of(2010,1,2)));
        transaction.setAmount(-210.02);
        transaction.setDescription("Testing 1");

        getMockMvc().perform(post("/jbr/ext/money/transaction")
                        .content(this.json(Collections.singletonList(transaction)))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        transaction.setCategoryId("FDG");
        transaction.setDate(DtoComplexModelMapper.localDateStringConverter.convert(LocalDate.of(2010,1,2)));
        transaction.setAmount(-84.12);
        transaction.setDescription("This is a much longer description test!!");

        getMockMvc().perform(post("/jbr/ext/money/transaction")
                        .content(this.json(Collections.singletonList(transaction)))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        StatementDTO statement = new StatementDTO();
        statement.setAccountId("AMEX");
        statement.setMonth(1);
        statement.setYear(2010);
        // Add the transaction to statement
        for(Transaction next : transactionRepository.findAll()) {
            next.setStatement(modelMapper.map(statement,Statement.class));
            transactionRepository.save(next);
        }

        // Lock the statement
        StatementIdDTO statementId = new StatementIdDTO("AMEX",1,2010);
        getMockMvc().perform(post("/jbr/int/money/statement/lock")
                        .content(this.json(statementId))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Request the report.
        ArchiveOrReportRequestDTO request = new ArchiveOrReportRequestDTO();
        request.setMonth(1);
        request.setYear(2010);

        getMockMvc().perform(post("/jbr/int/money/transaction/report")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Check that the report exists.
        File htmlFile = new File(applicationProperties.getReportWorking() + "/Report.html");
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/AMEX.png").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/AMEX.svg").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/HSE.png").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/HSE.svg").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/pie-.png").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/pie.svg").toPath()));
        Assert.assertTrue(Files.exists(htmlFile.toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportShare() + "/2010/Report-January-2010.pdf").toPath()));

        // Check the HTML file.
        String html = new String(Files.readAllBytes(htmlFile.toPath()));

        // Check the CSS.
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(html));
        org.w3c.dom.Document  document = db.parse(is);
        Document domDocument = new DOMBuilder().build(document);

        Element root = domDocument.getRootElement();
        Element head = root.getChild("head");
        Element style = head.getChild("style");
        Assert.assertNotNull(style);
        CssAssertHelper.checkReportCSS(style.getValue());

        // Get the expected html
        File expectedFile = new File("./src/test/resources/expected/html1.xml");
        String expected = new String(Files.readAllBytes(expectedFile.toPath()));

        // Get the difference.
        Diff htmlDiff = DiffBuilder.compare(expected).withTest(html).ignoreWhitespace().build();

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
