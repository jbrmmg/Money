package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.StatementMapper;
import com.jbr.middletier.money.utils.UtilityMapper;
import com.jbr.middletier.money.utils.CssAssertHelper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.DOMBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.Collections;
import java.util.Iterator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class AnnualReportTest extends Support {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private UtilityMapper utilityMapper;

    @Autowired
    private StatementMapper statementMapper;

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
        TransactionDTO transaction = new TransactionDTO();
        transaction.setAccountId("AMEX");
        transaction.setCategoryId("HSE");
        transaction.setDate(utilityMapper.map(LocalDate.of(2010,1,1),String.class));
        transaction.setAmount(10.02);
        transaction.setDescription("Testing");

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
            next.setStatement(statementMapper.map(statement,Statement.class));
            transactionRepository.save(next);
        }

        // Lock the statement
        StatementIdDTO lockStatementRequest = new StatementIdDTO("AMEX",1,2010);

        getMockMvc().perform(post("/jbr/int/money/statement/lock")
                        .content(this.json(lockStatementRequest))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Request the report.
        ArchiveOrReportRequestDTO request = new ArchiveOrReportRequestDTO();
        request.setMonth(1);
        request.setYear(2010);

        getMockMvc().perform(post("/jbr/int/money/transaction/annualreport")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Check that the report exists.
        File htmlFile = new File(applicationProperties.getReportWorking() + "/Report.html");
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/AMEX.png").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/AMEX.svg").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/HSE.png").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/HSE.svg").toPath()));
        Assert.assertTrue(Files.exists(htmlFile.toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportShare() + "/2010/Report-2010.pdf").toPath()));

        // Check the HTML file.
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        String html = new String(Files.readAllBytes(htmlFile.toPath()));
        is.setCharacterStream(new StringReader(html));
        org.w3c.dom.Document  document = db.parse(is);

        Document domDocument = new DOMBuilder().build(document);
        Element root = domDocument.getRootElement();
        Element head = root.getChild("head");
        Element style = head.getChild("style");
        Assert.assertNotNull(style);
        CssAssertHelper.checkReportCSS(style.getValue());

        // Get the expected html
        File expectedFile = new File("./src/test/resources/expected/html2.xml");
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
